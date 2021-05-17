package com.hedera.demo.auction.node.app.refunder;

import com.google.errorprone.annotations.Var;
import com.hedera.demo.auction.node.app.HederaClient;
import com.hedera.demo.auction.node.app.Utils;
import com.hedera.demo.auction.node.app.domain.Auction;
import com.hedera.demo.auction.node.app.domain.Bid;
import com.hedera.demo.auction.node.app.repository.AuctionsRepository;
import com.hedera.demo.auction.node.app.repository.BidsRepository;
import com.hedera.demo.auction.node.app.scheduledoperations.TransactionScheduler;
import com.hedera.demo.auction.node.app.scheduledoperations.TransactionSchedulerResult;
import com.hedera.hashgraph.sdk.AccountId;
import com.hedera.hashgraph.sdk.Client;
import com.hedera.hashgraph.sdk.Hbar;
import com.hedera.hashgraph.sdk.PrivateKey;
import com.hedera.hashgraph.sdk.Status;
import com.hedera.hashgraph.sdk.TransactionId;
import com.hedera.hashgraph.sdk.TransferTransaction;
import lombok.extern.log4j.Log4j2;

import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.TimeoutException;

@Log4j2
public class Refunder implements Runnable {
    private final BidsRepository bidsRepository;
    private final AuctionsRepository auctionsRepository;
    private final HederaClient hederaClient;
    private final int mirrorQueryFrequency;
    private boolean testing = false;
    private boolean runThread = true;
    private final PrivateKey refundKey;

    public Refunder(HederaClient hederaClient, AuctionsRepository auctionsRepository, BidsRepository bidsRepository, String refundKey, int mirrorQueryFrequency) {
        this.auctionsRepository = auctionsRepository;
        this.bidsRepository = bidsRepository;
        this.refundKey = PrivateKey.fromString(refundKey);
        this.hederaClient = hederaClient;
        this.mirrorQueryFrequency = mirrorQueryFrequency;
    }
    public void setTesting() {
        this.testing = true;
    }

    public void stop() {
        runThread = false;
    }

    @Override
    public void run() {
        while (runThread) {
            try {
                List<Auction> auctions = auctionsRepository.getAuctionsList();
                if (auctions != null) {
                    for (Auction auction : auctions) {
                        try {
                            Client auctionClient = hederaClient.auctionClient(auction, refundKey);
                            try {
                                List<Bid> bidsToRefund = bidsRepository.bidsToRefund(auction.getId());
                                if (bidsToRefund != null) {
                                    for (Bid bid : bidsToRefund) {
                                        issueRefund(auction.getAuctionaccountid(), bid, auctionClient);
                                    }
                                }
                            auctionClient.close();
                            } catch (SQLException sqlException) {
                                log.error("unable to fetch bids to refund");
                                log.error(sqlException);
                            }
                        } catch (Exception e) {
                            log.error("error setting up auction client");
                            log.error(e);
                        }
                    }
                }
            } catch (SQLException sqlException) {
                log.error("unable to fetch auctions list");
                log.error(sqlException);
            }
        }

        Utils.sleep(this.mirrorQueryFrequency);
    }

    private void issueRefund(String auctionAccount, Bid bid, Client client) {
        @Var boolean refundIsInProgress = false;
        @Var boolean delayRefund = false;

        AccountId auctionAccountId = AccountId.fromString(auctionAccount);

        log.info("Refunding " + bid.getBidamount() + " from " + auctionAccount + " to " + bid.getBidderaccountid());
        String memo = Bid.REFUND_MEMO_PREFIX.concat(bid.getTransactionid());
        // issue refund

        String txId = auctionAccount.concat("@").concat(bid.getTimestampforrefund());
        TransactionId transactionId = TransactionId.fromString(txId);
        transactionId.setScheduled(true);
        String shortTransactionId = transactionId.toString().replace("?scheduled", "");

        if (testing) {
            // just testing, we can't sign a scheduled transaction, just record the state change on the bid
            setRefundInProgress(bid);
        } else {
            // Create a transfer transaction for the refund
            TransferTransaction transferTransaction = new TransferTransaction();
            transferTransaction.setTransactionMemo(memo);
            transferTransaction.addHbarTransfer(auctionAccountId, Hbar.fromTinybars(-bid.getBidamount()));
            transferTransaction.addHbarTransfer(AccountId.fromString(bid.getBidderaccountid()), Hbar.fromTinybars(bid.getBidamount()));

            try {
                TransactionScheduler transactionScheduler = new TransactionScheduler(client, auctionAccountId, refundKey, transactionId, transferTransaction);
                TransactionSchedulerResult transactionSchedulerResult = transactionScheduler.issueScheduledTransaction();

                if (transactionSchedulerResult.success) {
                    refundIsInProgress = true;
                    log.info("Refund transaction successfully scheduled (id " + shortTransactionId + ")");
                } else if (transactionSchedulerResult.status == Status.TRANSACTION_EXPIRED) {
                    delayRefund = true;
                } else {
                    log.error("Error issuing refund to bid - timestamp = " + bid.getTimestamp());
                    log.error(transactionSchedulerResult.status);
                }
            } catch (TimeoutException timeoutException) {
                log.error(timeoutException);
            }

            if (refundIsInProgress) {
                log.info("setting bid to refund in progress (timestamp = " + bid.getTimestamp() + ")");
                setRefundInProgress(bid);
            }
            if (delayRefund) {
                // the bid's timestamp is too far in the past for a deterministic transaction id, add 30s and let the process
                // try again later
                log.info("delaying bid refund (timestamp = " + bid.getTimestamp() + ")");
                String bidTimeStamp = bid.getTimestamp();
                String bidRefundTimeStamp = Utils.addToTimestamp(bid.getTimestampforrefund(), 30);

                try {
                    bidsRepository.setBidRefundTimestamp(bidTimeStamp, bidRefundTimeStamp);
                } catch (SQLException sqlException) {
                    log.error("Unable to set bid next refund timestamp - bid timestamp = " + bidTimeStamp);
                    log.error(sqlException);
                }
            }
        }
    }

    private void setRefundInProgress(Bid bid) {
        try {
            bidsRepository.setRefundIssued(bid.getTimestamp());
        } catch (SQLException sqlException) {
            log.error("Failed to set bid refund in progress (bid timestamp " + bid.getTimestamp() + ")");
            log.error(sqlException);
        }
    }
}
