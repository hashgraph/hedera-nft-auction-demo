package com.hedera.demo.auction.node.app.bidwatcher;

import com.google.errorprone.annotations.Var;
import com.hedera.demo.auction.node.app.HederaClient;
import com.hedera.demo.auction.node.app.domain.Auction;
import com.hedera.demo.auction.node.app.domain.Bid;
import com.hedera.demo.auction.node.app.refunder.Refunder;
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

    protected AbstractBidsWatcher(WebClient webClient, AuctionsRepository auctionsRepository, BidsRepository bidsRepository, int auctionId, String refundKey, int mirrorQueryFrequency) throws Exception {
        this.webClient = webClient;
        this.bidsRepository = bidsRepository;
        this.auctionsRepository = auctionsRepository;
        this.auctionId = auctionId;
        this.refundKey = refundKey;
        this.mirrorQueryFrequency = mirrorQueryFrequency;
        this.mirrorURL = HederaClient.getMirrorUrl();
        this.auction = auctionsRepository.getAuction(auctionId);
    }

    void handleResponse(JsonObject response) {
        try {
            if (response != null) {
                MirrorTransactions mirrorTransactions = response.mapTo(MirrorTransactions.class);

                for (MirrorTransaction transaction : mirrorTransactions.transactions) {
                    handleTransaction(transaction);
                    this.auction.setLastconsensustimestamp(transaction.consensusTimestamp);
                    auctionsRepository.save(this.auction);
                }
            }
        } catch (Exception e) {
            log.error(e);
        }
    }


    void handleTransaction(MirrorTransaction transaction) throws SQLException {
        @Var String rejectReason = "";
        @Var boolean refund = false;
        String auctionAccountId = auction.getAuctionaccountid();
        @Var long bidAmount = 0;

        if (transaction.payer().equals(this.auction.getAuctionaccountid())) {
            log.debug("Skipping auction account refund transaction");
            return;
        }

        if (transaction.isSuccessful()) {
            //Handle memo on transfer and create to allow for transactions that aren't bids
            if (checkMemos(transaction.getMemo())) {
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
                rejectReason = "Auction is closed";
            } else if (transaction.consensusTimestamp.compareTo(this.auction.getStarttimestamp()) <= 0) {
                refund = true;
                rejectReason = "Auction has not started yet";
            }

            if ( ! refund) {
                // check if paying account is different to the current winner
                if (transaction.payer().equals(this.auction.getWinningaccount())) {
                    if (! this.auction.getWinnerCanBid()) {
                        // same account as winner, not allowed
                        rejectReason = "Winner can't bid again";
                        refund = true;
                    }
                }
            }

            if ( ! refund) {
                // find payment amount
                for (MirrorHbarTransfer transfer : transaction.hbarTransfers) {
                    if (transfer.account.equals(auctionAccountId)) {
                        bidAmount = transfer.amount;
                        log.debug("Bid amount is " + bidAmount);
                        break;
                    }
                }

                long bidDelta = (bidAmount - this.auction.getWinningbid()) / 100000000;
                if ((bidDelta > 0) && (bidDelta < this.auction.getMinimumbid())) {
                    rejectReason = "Bid increase too small";
                    refund = true;
                }
                if (bidAmount != 0) { // if bid !=0, no refund is expected at this stage
                    // we have a bid, check it against bidding rules
                    if (bidAmount < this.auction.getReserve()) {
                        rejectReason = "Bid below reserve";
                    } else if (bidAmount <= this.auction.getWinningbid()) {
                        rejectReason = "Under bid";
                        refund = true;
                    }
                }
            }

            //TODO: update auction and bid in a single tx
            if (StringUtils.isEmpty(rejectReason)) {
                // we have a winner
                // refund previous bid
                if ( ! StringUtils.isEmpty(this.auction.getWinningaccount())) {
                    // do not refund the very first bid !!!
                    startRefundThread (this.auction.getWinningbid(), this.auction.getWinningaccount(), this.auction.getWinningtimestamp(), this.auction.getWinningtxid());
                    refund = false;
                }
                // update prior winning bid
                Bid priorBid = new Bid();
                priorBid.setTimestamp(this.auction.getWinningtimestamp());
                priorBid.setStatus("Higher bid received");
                bidsRepository.setStatus(priorBid);

                // update the auction
                this.auction.setWinningtimestamp(transaction.consensusTimestamp);
                this.auction.setWinningaccount(transaction.payer());
                this.auction.setWinningbid(bidAmount);
                this.auction.setWinningtxid(transaction.transactionId);
                this.auction.setWinningtxhash(transaction.getTransactionHash());
            }

            // store the bid
            Bid currentBid = new Bid();
            currentBid.setStatus(rejectReason);
            currentBid.setBidamount(bidAmount);
            currentBid.setAuctionid(this.auction.getId());
            currentBid.setBidderaccountid(transaction.payer());
            currentBid.setTimestamp(transaction.consensusTimestamp);
            currentBid.setStatus(rejectReason);
            currentBid.setTransactionid(transaction.transactionId);
            currentBid.setTransactionhash(transaction.getTransactionHash());
            bidsRepository.add(currentBid);

            if (refund) {
                // refund this transaction
                startRefundThread (bidAmount, transaction.payer(), transaction.consensusTimestamp, transaction.transactionId);
            }

        } else {
            log.debug("Transaction Id " + transaction.transactionId + " status not SUCCESS.");
        }
    }

    boolean checkMemos(String memo) {
        if (StringUtils.isEmpty(memo)) {
            return false;
        }
        String[] memos = new String[]{"CREATEAUCTION", "FUNDACCOUNT", "TRANSFERTOAUCTION", "ASSOCIATE", "AUCTION REFUND"};
        return Arrays.stream(memos).anyMatch(memo.toUpperCase()::equals);
    }

    void startRefundThread(long refundAmound, String refundToAccount, String timestamp, String transactionId) {
        Thread t = new Thread(new Refunder(bidsRepository, auction.getAuctionaccountid(), refundAmound, refundToAccount, timestamp, transactionId, refundKey));
        t.start();
    }
}
