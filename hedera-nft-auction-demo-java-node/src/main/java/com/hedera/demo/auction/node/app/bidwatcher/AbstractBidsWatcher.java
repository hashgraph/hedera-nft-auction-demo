package com.hedera.demo.auction.node.app.bidwatcher;

import com.google.errorprone.annotations.Var;
import com.hedera.demo.auction.node.app.HederaClient;
import com.hedera.demo.auction.node.app.domain.Auction;
import com.hedera.demo.auction.node.app.domain.Bid;
import com.hedera.demo.auction.node.app.repository.AuctionsRepository;
import com.hedera.demo.auction.node.app.repository.BidsRepository;
import com.hedera.demo.auction.node.app.mirrormapping.MirrorHbarTransfer;
import com.hedera.demo.auction.node.app.mirrormapping.MirrorTransaction;
import com.hedera.demo.auction.node.app.mirrormapping.MirrorTransactions;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import lombok.extern.log4j.Log4j2;
import org.jooq.tools.StringUtils;

import java.sql.SQLException;
import java.util.Arrays;

@Log4j2
public abstract class AbstractBidsWatcher {

    protected Auction auction;
    protected final WebClient webClient;
    protected final BidsRepository bidsRepository;
    protected final AuctionsRepository auctionsRepository;
    protected final String refundKey;
    protected final int mirrorQueryFrequency;
    protected String mirrorURL;
    protected final int auctionId;
    protected final HederaClient hederaClient;
    protected boolean testing = false;
    protected boolean runThread = true;

    protected AbstractBidsWatcher(HederaClient hederaClient, WebClient webClient, AuctionsRepository auctionsRepository, BidsRepository bidsRepository, int auctionId, String refundKey, int mirrorQueryFrequency) {
        this.webClient = webClient;
        this.bidsRepository = bidsRepository;
        this.auctionsRepository = auctionsRepository;
        this.auctionId = auctionId;
        this.refundKey = refundKey;
        this.mirrorQueryFrequency = mirrorQueryFrequency;
        this.hederaClient = hederaClient;
        this.mirrorURL = hederaClient.mirrorUrl();
        try {
            this.auction = auctionsRepository.getAuction(auctionId);
        } catch (Exception e) {
            log.error("failed to fetch auction id " + auctionId + " from database.");
            log.error(e);
        }
    }

    public void setTesting() {
        this.testing = true;
    }

    public void stop() {
        runThread = false;
    }

    public void handleResponse(JsonObject response) {
        try {
            if (response != null) {
                MirrorTransactions mirrorTransactions = response.mapTo(MirrorTransactions.class);

                for (MirrorTransaction transaction : mirrorTransactions.transactions) {
                    if (transaction.isSuccessful()) {
                        handleTransaction(transaction);
                    }
                    this.auction.setLastconsensustimestamp(transaction.consensusTimestamp);
                    auctionsRepository.save(this.auction);
                }
            }
        } catch (Exception e) {
            log.error(e);
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
                    log.debug("Bid amount is " + bidAmount);
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

            //TODO: update auction and bid in a single tx
            if (StringUtils.isEmpty(rejectReason)) {
                // we have a winner
                // update prior winning bid
                Bid priorBid = new Bid();
                // setting the timestamp for refund to match the winning timestamp
                // will accelerate the refund (avoids repeated EXPIRED_TRANSACTIONS when refunding)
                priorBid.setTimestampforrefund(transaction.consensusTimestamp);
                priorBid.setTimestamp(this.auction.getWinningtimestamp());
                priorBid.setStatus(Bid.HIGHER_BID);
                if ( StringUtils.isEmpty(this.auction.getWinningaccount())) {
                    // do not refund the very first bid !!!
                    priorBid.setRefundstatus("");
                    refund = false;
                } else {
                    priorBid.setRefundstatus(Bid.REFUND_PENDING);
                }
                bidsRepository.setStatus(priorBid);

                // update the auction
                this.auction.setWinningtimestamp(transaction.consensusTimestamp);
                this.auction.setWinningaccount(transaction.payer());
                this.auction.setWinningbid(bidAmount);
                this.auction.setWinningtxid(transaction.transactionId);
                this.auction.setWinningtxhash(transaction.getTransactionHashString());
                auctionsRepository.save(this.auction);
            }

            // store the bid
            Bid currentBid = new Bid();
            currentBid.setBidamount(bidAmount);
            currentBid.setAuctionid(this.auction.getId());
            currentBid.setBidderaccountid(transaction.payer());
            currentBid.setTimestamp(transaction.consensusTimestamp);
            currentBid.setStatus(rejectReason);
            currentBid.setTransactionid(transaction.transactionId);
            currentBid.setTransactionhash(transaction.getTransactionHashString());
            if (refund) {
                currentBid.setRefundstatus(Bid.REFUND_PENDING);
            }
            bidsRepository.add(currentBid);
        } else {
            log.debug("Transaction Id " + transaction.transactionId + " status not SUCCESS.");
        }
    }

    public boolean checkMemos(String memo) {
        if (StringUtils.isEmpty(memo)) {
            return false;
        }
        String[] memos = new String[]{"CREATEAUCTION", "FUNDACCOUNT", "TRANSFERTOAUCTION", "ASSOCIATE", "AUCTION REFUND"};
        return Arrays.stream(memos).anyMatch(memo.toUpperCase()::startsWith);
    }
}
