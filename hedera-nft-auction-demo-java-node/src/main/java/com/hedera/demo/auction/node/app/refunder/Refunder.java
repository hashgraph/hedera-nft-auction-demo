package com.hedera.demo.auction.node.app.refunder;

import com.google.common.base.Splitter;
import com.hedera.demo.auction.node.app.HederaClient;
import com.hedera.demo.auction.node.app.TransactionScheduler;
import com.hedera.demo.auction.node.app.domain.Auction;
import com.hedera.demo.auction.node.app.domain.Bid;
import com.hedera.demo.auction.node.app.repository.AuctionsRepository;
import com.hedera.demo.auction.node.app.repository.BidsRepository;
import com.hedera.hashgraph.sdk.*;
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
                            List<Bid> bidsToRefund = bidsRepository.bidsToRefund(auction.getId());
                            if (bidsToRefund != null) {
                                for (Bid bid : bidsToRefund) {
                                    issueRefund(auction.getAuctionaccountid(), bid);
                                }
                            }
                        } catch (SQLException sqlException) {
                            log.error("unable to fetch bids to refund");
                            log.error(sqlException);
                        }
                    }
                }
            } catch (SQLException sqlException) {
                log.error("unable to fetch auctions list");
                log.error(sqlException);
            }
        }

        try {
            Thread.sleep(mirrorQueryFrequency);
        } catch (InterruptedException e) {
            log.error(e);
        }
    }

    private void issueRefund(String auctionAccount, Bid bid) {
        boolean refundIsInProgress = false;
        boolean delayRefund = false;

        AccountId auctionAccountId = AccountId.fromString(auctionAccount);
        //TODO: Check a scheduled transaction has not already completed (success) for this bid
        // can only work with scheduled transactions

        // create a client for the auction's account
        Client client = hederaClient.client();

        client.setOperator(auctionAccountId, this.refundKey);
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
                TransactionScheduler.issueScheduledTransaction(client, auctionAccountId, refundKey, transactionId, transferTransaction);
                refundIsInProgress = true;
                log.info("Refund transaction successfully scheduled (id " + shortTransactionId + ")");
            } catch (ReceiptStatusException receiptStatusException) {
                if (receiptStatusException.receipt.status == Status.SCHEDULE_ALREADY_EXECUTED) {
                    refundIsInProgress = true;
                } else if (receiptStatusException.receipt.status == Status.TRANSACTION_EXPIRED) {
                    delayRefund = true;
                } else {
                    log.error("Error issuing refund to bid - timestamp = " + bid.getTimestamp());
                    log.error(receiptStatusException.receipt.status);
                }
            } catch (PrecheckStatusException precheckStatusException) {
                if (precheckStatusException.status == Status.SCHEDULE_ALREADY_EXECUTED) {
                    refundIsInProgress = true;
                } else if (precheckStatusException.status == Status.TRANSACTION_EXPIRED) {
                    delayRefund = true;
                } else {
                    log.error("Error issuing refund to bid - timestamp = " + bid.getTimestamp());
                    log.error(precheckStatusException.status);
                }
            } catch (TimeoutException timeoutException) {
                log.error(timeoutException);
            } finally {
                if (refundIsInProgress) {
                    log.info("setting bid to refund in progress (timestamp = " + bid.getTimestamp() + ")");
                    setRefundInProgress(bid);
                }
                if (delayRefund) {
                    // the bid's timestamp is too far in the past for a deterministic transaction id, add 30s and let the process
                    // try again later
                    log.info("delaying bid refund (timestamp = " + bid.getTimestamp() + ")");
                    String bidTimeStamp = bid.getTimestamp();
                    List<String> timeStampParts = Splitter.on('.').splitToList(bid.getTimestampforrefund());
                    Long seconds = Long.parseLong(timeStampParts.get(0)) + 30;
                    String bidRefundTimeStamp = String.valueOf(seconds).concat(".").concat(timeStampParts.get(1));

                    try {
                        bidsRepository.setBidRefundTimestamp(bidTimeStamp, bidRefundTimeStamp);
                    } catch (SQLException sqlException) {
                        log.error("Unable to set bid next refund timestamp - bid timestamp = " + bidTimeStamp);
                        log.error(sqlException);
                    }
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
