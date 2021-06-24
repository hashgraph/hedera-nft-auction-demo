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

@Log4j2
public class BidsWatcher implements Runnable {

    private final int auctionId;
    private Auction auction;
    private final AuctionsRepository auctionsRepository;
    private final int mirrorQueryFrequency;
    private final HederaClient hederaClient;
    protected boolean runThread = true;
    protected boolean testing = false;
    protected boolean runOnce = false;

    public BidsWatcher(HederaClient hederaClient, AuctionsRepository auctionsRepository, int auctionId, int mirrorQueryFrequency) {
        this.auctionsRepository = auctionsRepository;
        this.auctionId = auctionId;
        this.mirrorQueryFrequency = mirrorQueryFrequency;
        this.hederaClient = hederaClient;

        try {
            this.auction = auctionsRepository.getAuction(auctionId);
        } catch (Exception e) {
            log.error("unable to get auction id {}", auctionId, e);
        }
    }

    public BidsWatcher(HederaClient hederaClient, AuctionsRepository auctionsRepository, int auctionId, int mirrorQueryFrequency, boolean runOnce) {
        this(hederaClient, auctionsRepository, auctionId, mirrorQueryFrequency);
        this.runOnce = runOnce;
    }

    public void setTesting() {
        this.testing = true;
    }

    public void stop() {
        runThread = false;
    }

    @Override
    public void run() {

        @Var String nextLink = "";
        String uri = "/api/v1/transactions";

        ExecutorService executor = Executors.newFixedThreadPool(1);
        while (runThread) {
            try {
                // reload auction from database
                Auction watchedAuction = auctionsRepository.getAuction(auctionId);

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

    public void handleResponse(MirrorTransactions mirrorTransactions) {
        try {
            for (MirrorTransaction transaction : mirrorTransactions.transactions) {
                if (transaction.isSuccessful()) {
                    handleTransaction(transaction);
                }
                this.auction.setLastconsensustimestamp(transaction.consensusTimestamp);
                auctionsRepository.save(this.auction);
            }
        } catch (Exception e) {
            log.error(e, e);
        }
    }

    public void handleTransaction(MirrorTransaction transaction) throws SQLException {
        @Var String rejectReason = "";
        @Var boolean refund = false;
        String auctionAccountId = this.auction.getAuctionaccountid();
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
            if (transaction.consensusTimestamp.compareTo(this.auction.getEndtimestamp()) > 0) {
                // payment past auctions end, close it, but continue processing
                if (!this.auction.isClosed()) {
                    this.auction = auctionsRepository.setClosed(this.auction);
                }
                // find payment amount
                refund = true;
                rejectReason = Bid.AUCTION_CLOSED;
            } else if (transaction.consensusTimestamp.compareTo(this.auction.getStarttimestamp()) <= 0) {
                refund = true;
                rejectReason = Bid.AUCTION_NOT_STARTED;
            }

            if ( ! refund) {
                // check if paying account is different to the current winner
                if (transaction.payer().equals(this.auction.getWinningaccount())) {
                    if (! this.auction.getWinnerCanBid()) {
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

                long bidDelta = (bidAmount - this.auction.getWinningbid());
                if ((bidDelta > 0) && (bidDelta < this.auction.getMinimumbid())) {
                    rejectReason = Bid.INCREASE_TOO_SMALL;
                    refund = true;
                }
                if (bidAmount != 0) { // if bid !=0, no refund is expected at this stage
                    // we have a bid, check it against bidding rules
                    if (bidAmount < this.auction.getReserve()) {
                        rejectReason = Bid.BELOW_RESERVE;
                        refund = true;
                    } else if (bidAmount <= this.auction.getWinningbid()) {
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
                if ( StringUtils.isEmpty(this.auction.getWinningaccount())) {
                    // do not refund the very first bid !!!
//                    priorBid.setRefundstatus("");
                    refund = false;
                } else {
                    priorBid.setTimestamp(this.auction.getWinningtimestamp());
                    priorBid.setStatus(Bid.HIGHER_BID);
                    priorBid.setRefundstatus(Bid.REFUND_PENDING);
                    updatePriorBid = true;
                }

                // update the auction
                this.auction.setWinningtimestamp(transaction.consensusTimestamp);
                this.auction.setWinningaccount(transaction.payer());
                this.auction.setWinningbid(bidAmount);
                this.auction.setWinningtxid(transaction.transactionId);
                this.auction.setWinningtxhash(transaction.getTransactionHashString());
            }

            Bid newBid = new Bid();
            if (bidAmount > 0) {
                // store the bid
                newBid.setBidamount(bidAmount);
                newBid.setAuctionid(this.auction.getId());
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

            auctionsRepository.commitBidAndAuction(updatePriorBid, bidAmount, priorBid, auction, newBid);
        } else {
            log.debug("Transaction Id {} status not SUCCESS.", transaction.transactionId);
        }
    }

    public boolean checkMemos(String memo) {
        if (StringUtils.isEmpty(memo)) {
            return false;
        }
        String[] memos = new String[]{"CREATEAUCTION", "FUNDACCOUNT", "TRANSFERTOAUCTION", "ASSOCIATE", "AUCTION REFUND", "TOKEN TRANSFER FROM AUCTION", "SCHEDULED REFUND"};
        return Arrays.stream(memos).anyMatch(memo.toUpperCase()::startsWith);
    }
}