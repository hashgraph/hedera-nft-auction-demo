package com.hedera.demo.auction;

import com.google.errorprone.annotations.Var;
import com.hedera.demo.auction.app.HederaClient;
import com.hedera.demo.auction.app.Utils;
import com.hedera.demo.auction.app.domain.Auction;
import com.hedera.demo.auction.app.domain.Bid;
import com.hedera.demo.auction.app.mirrormapping.MirrorHbarTransfer;
import com.hedera.demo.auction.app.mirrormapping.MirrorTransaction;
import com.hedera.demo.auction.app.mirrormapping.MirrorTransactions;
import com.hedera.demo.auction.app.repository.AuctionsRepository;
import io.vertx.core.json.JsonObject;
import lombok.extern.log4j.Log4j2;
import org.jooq.tools.StringUtils;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Watches for bids against an auction
 */
@Log4j2
public class BidsWatcher implements Runnable {

    private final int auctionId;
    private final AuctionsRepository auctionsRepository;
    private final int mirrorQueryFrequency;
    private final HederaClient hederaClient;
    protected boolean runThread = true;
    protected boolean runOnce;
    protected Auction watchedAuction = new Auction();

    /**
     * Constructor
     *
     * @param hederaClient the HederaClient to use to connect to Hedera
     * @param auctionsRepository the auction repository for database access
     * @param auctionId the id of the auction to check bids for
     * @param mirrorQueryFrequency the frequency at which to query a mirror node
     * @param runOnce runs the check only once
     */
    public BidsWatcher(HederaClient hederaClient, AuctionsRepository auctionsRepository, int auctionId, int mirrorQueryFrequency, boolean runOnce) {
        this.auctionsRepository = auctionsRepository;
        this.auctionId = auctionId;
        this.mirrorQueryFrequency = mirrorQueryFrequency;
        this.hederaClient = hederaClient;
        this.runOnce = runOnce;
    }

    /**
     * Stops the thread cleanly
     */
    public void stop() {
        runThread = false;
    }

    /**
     * For the given auction id, repeatedly query the mirror node for CRYPTOTRANSFER transactions involving the auction's account id
     * process the transactions
     */
    @Override
    public void run() {
        @Var String nextLink = "";
        String uri = "/api/v1/transactions";

        ExecutorService executor = Executors.newFixedThreadPool(1);
        while (runThread) {
            try {
                // reload auction from database
                watchedAuction = auctionsRepository.getAuction(auctionId);

                log.debug("Checking for bids on account {} and token {}", watchedAuction.getAuctionaccountid(), watchedAuction.getTokenid());

                @Var String consensusTimeStampFrom = StringUtils.isEmpty(nextLink) ? watchedAuction.getLastconsensustimestamp() : nextLink;
                consensusTimeStampFrom = StringUtils.isEmpty(consensusTimeStampFrom) ? "0.0" : consensusTimeStampFrom;
                Map<String, String> queryParameters = new HashMap<>();
                queryParameters.put("account.id", watchedAuction.getAuctionaccountid());
                queryParameters.put("transactiontype", "CRYPTOTRANSFER");
                queryParameters.put("order", "asc");
                queryParameters.put("timestamp", "gt:".concat(consensusTimeStampFrom));

                Future<JsonObject> future = executor.submit(Utils.queryMirror(hederaClient, uri, queryParameters));

                try {
                    JsonObject response = future.get();
                    if (response != null) {
                        MirrorTransactions mirrorTransactions = response.mapTo(MirrorTransactions.class);
                        nextLink = Utils.getTimestampFromMirrorLink(mirrorTransactions.links.next);
                        handleResponse(mirrorTransactions);
                    }
                } catch (InterruptedException e) {
                    log.error(e, e);
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    log.error(e, e);
                }

            } catch (Exception e) {
                log.error(e, e);
            }
            if (StringUtils.isEmpty(nextLink)) {
                // no more to process
                if (this.runOnce) {
                    this.runThread = false;
                } else {
                    Utils.sleep(this.mirrorQueryFrequency);
                }
            }
        }
        executor.shutdown();
    }

    /**
     * For each of the transactions, if successful, handle the transaction details
     * Record the timestamp of the transaction in the database so that future
     * mirror queries are performed from this timestamp onwards
     *
     * @param mirrorTransactions a list of transactions from mirror node
     */
    public void handleResponse(MirrorTransactions mirrorTransactions) throws Exception {
        if (this.watchedAuction.getId() == 0) {
            watchedAuction = auctionsRepository.getAuction(auctionId);
        }
        try {
            for (MirrorTransaction transaction : mirrorTransactions.transactions) {
                if (transaction.isSuccessful()) {
                    handleTransaction(transaction);
                }
                auctionsRepository.setLastConsensusTimestamp(this.watchedAuction.getId(), transaction.consensusTimestamp);
            }
        } catch (Exception e) {
            log.error(e, e);
        }
    }

    /**
     * For a given transaction
     * * If the payer of the transaction is the auction account, skip it
     * * Pattern match the memo and return if memo matches
     * * Check the transaction timestamp against the auction's end date, if the timestamp is greater
     * than the auction end, reject the bid and set it to be refunded
     * * If the transaction timestamp is less or equal to the auction's start timestamp, reject the bid and set it to be refunded
     * * If the transaction payer is the current auction winner and the winner is not allowed to bid, reject the bid and set it to be refunded
     * * Look for the paid amount in the transaction
     * * If the delta between the paid amount and the current winning bid is lower than the minimum bid, reject the bid and set it to be refunded
     * * If the bid is below reserve, reject and set it to be refunded
     * * If the bid is below the current winning bid, reject and set it to be refunded
     *
     * If the bid has not been rejected and is not the first bid, set the prior winning bid to be refunded
     * Update the auction with the new winning bid, winning account, transaction id and hash and bid timestamp
     *
     * Store the new bid in the database
     *
     * @param transaction the transaction to analyze
     * @throws SQLException thrown in the event of a database connection error
     */
    private void handleTransaction(MirrorTransaction transaction) throws SQLException {
        @Var String rejectReason = "";
        @Var boolean refund = false;
        String auctionAccountId = this.watchedAuction.getAuctionaccountid();
        @Var long bidAmount = 0;

        if (transaction.payer().equals(auctionAccountId)) {
            log.debug("Skipping auction account refund transaction");
            return;
        }

        if (transaction.isSuccessful()) {
            //Handle memo on transfer and create to allow for transactions that aren't bids
            if (checkMemos(transaction.getMemoString())) {
                return;
            }

            // check the timestamp to verify if auction should end
            if (transaction.consensusTimestamp.compareTo(this.watchedAuction.getEndtimestamp()) > 0) {
                // find payment amount
                refund = true;
                rejectReason = Bid.AUCTION_CLOSED;
            } else if (transaction.consensusTimestamp.compareTo(this.watchedAuction.getStarttimestamp()) <= 0) {
                refund = true;
                rejectReason = Bid.AUCTION_NOT_STARTED;
            }

            if ( ! refund) {
                // check if paying account is different to the current winner
                if (transaction.payer().equals(this.watchedAuction.getWinningaccount())) {
                    if (! this.watchedAuction.getWinnerCanBid()) {
                        // same account as winner, not allowed
                        rejectReason = Bid.WINNER_CANT_BID;
                        refund = true;
                    }
                }
            }

            // find payment amount
            for (MirrorHbarTransfer transfer : transaction.hbarTransfers) {
                if (transfer.account.equals(auctionAccountId)) {
                    bidAmount = transfer.amount;
                    log.debug("Bid amount is {}", bidAmount);
                    break;
                }
            }

            if ( ! refund) {

                long bidDelta = (bidAmount - this.watchedAuction.getWinningbid());
                if ((bidDelta > 0) && (bidDelta < this.watchedAuction.getMinimumbid())) {
                    rejectReason = Bid.INCREASE_TOO_SMALL;
                    refund = true;
                }
                if (bidAmount != 0) { // if bid !=0, no refund is expected at this stage
                    // we have a bid, check it against bidding rules
                    if (bidAmount < this.watchedAuction.getReserve()) {
                        rejectReason = Bid.BELOW_RESERVE;
                        refund = true;
                    } else if (bidAmount <= this.watchedAuction.getWinningbid()) {
                        rejectReason = Bid.UNDER_BID;
                        refund = true;
                    }
                }
            }

            @Var boolean updatePriorBid = false;
            Bid priorBid = new Bid();

            if (StringUtils.isEmpty(rejectReason)) {
                // we have a winner
                // update prior winning bid
                // setting the timestamp for refund to match the winning timestamp
                // will accelerate the refund (avoids repeated EXPIRED_TRANSACTIONS when refunding)
                if ( StringUtils.isEmpty(this.watchedAuction.getWinningaccount())) {
                    // do not refund the very first bid !!!
//                    priorBid.setRefundstatus("");
                    refund = false;
                } else {
                    priorBid.setTimestamp(this.watchedAuction.getWinningtimestamp());
                    priorBid.setStatus(Bid.HIGHER_BID);
                    priorBid.setRefundstatus(Bid.REFUND_PENDING);
                    updatePriorBid = true;
                }

                // update the auction
                this.watchedAuction.setWinningtimestamp(transaction.consensusTimestamp);
                this.watchedAuction.setWinningaccount(transaction.payer());
                this.watchedAuction.setWinningbid(bidAmount);
                this.watchedAuction.setWinningtxid(transaction.transactionId);
                this.watchedAuction.setWinningtxhash(transaction.getTransactionHashString());
            }

            Bid newBid = new Bid();
            if (bidAmount > 0) {
                // store the bid
                newBid.setBidamount(bidAmount);
                newBid.setAuctionid(this.watchedAuction.getId());
                newBid.setBidderaccountid(transaction.payer());
                newBid.setTimestamp(transaction.consensusTimestamp);
                newBid.setStatus(rejectReason);
                newBid.setTransactionid(transaction.transactionId);
                newBid.setTransactionhash(transaction.getTransactionHashString());
                if (refund) {
                    newBid.setRefundstatus(Bid.REFUND_PENDING);
                }
            } else {
                log.info("Bid amount {} less than or equal to 0, not recording bid", bidAmount);
            }

            auctionsRepository.commitBidAndAuction(updatePriorBid, bidAmount, priorBid, watchedAuction, newBid);
        } else {
            log.debug("Transaction Id {} status not SUCCESS.", transaction.transactionId);
        }
    }

    /**
     * Checks a transaction's memo for certain values to avoid processing unnecessary transactions
     *
     * @param memo the transaction's memo
     * @return true if the transaction's memo matches one of the defined memos
     */
    public boolean checkMemos(String memo) {
        if (StringUtils.isEmpty(memo)) {
            return false;
        }
        String[] memos = new String[]{"CREATEAUCTION", "FUNDACCOUNT", "TRANSFERTOAUCTION", "ASSOCIATE", "AUCTION REFUND", "TOKEN TRANSFER FROM AUCTION", "SCHEDULED REFUND", "MANAGE VALIDATORS"};
        return Arrays.stream(memos).anyMatch(memo.toUpperCase()::startsWith);
    }
}
