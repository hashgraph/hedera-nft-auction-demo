package com.hedera.demo.auction;

import com.hedera.demo.auction.app.HederaClient;
import com.hedera.demo.auction.app.Utils;
import com.hedera.demo.auction.app.domain.Auction;
import com.hedera.demo.auction.app.domain.Bid;
import com.hedera.demo.auction.app.repository.AuctionsRepository;
import com.hedera.demo.auction.app.repository.BidsRepository;
import com.hedera.demo.auction.app.scheduledoperations.TransactionScheduler;
import com.hedera.hashgraph.sdk.AccountId;
import com.hedera.hashgraph.sdk.Client;
import com.hedera.hashgraph.sdk.PrivateKey;
import lombok.extern.log4j.Log4j2;

import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Log4j2
public class Refunder implements Runnable {
    private final BidsRepository bidsRepository;
    private final AuctionsRepository auctionsRepository;
    private final HederaClient hederaClient;
    private final int mirrorQueryFrequency;
    private boolean testing = false;
    private boolean runThread = true;
    private final PrivateKey operatorKey;
    private final ExecutorService executor = Executors.newFixedThreadPool(10);

    public Refunder(HederaClient hederaClient, AuctionsRepository auctionsRepository, BidsRepository bidsRepository, String operatorKey, int mirrorQueryFrequency) {
        this.auctionsRepository = auctionsRepository;
        this.bidsRepository = bidsRepository;
        this.operatorKey = PrivateKey.fromString(operatorKey);
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
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                log.debug("Performing some shutdown cleanup...");
                runThread = false;
                log.debug("Done cleaning");
            }
        }));

        while (runThread) {
            try {
                List<Auction> auctions = auctionsRepository.getAuctionsList();
                if (auctions != null) {
                    for (Auction auction : auctions) {
                        try {
                            Client auctionClient = hederaClient.auctionClient(auction, operatorKey);
                            try {
                                List<Bid> bidsToRefund = bidsRepository.bidsToRefund(auction.getId());
                                if (bidsToRefund != null) {
                                    for (Bid bid : bidsToRefund) {
                                        issueRefund(auction.getAuctionaccountid(), bid);
                                    }
                                }
                            auctionClient.close();
                            } catch (SQLException sqlException) {
                                log.error("unable to fetch bids to refund");
                                log.error(sqlException);
                            } catch (Exception e) {
                                log.error("error issuing refund");
                                log.error(e);
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
            Utils.sleep(this.mirrorQueryFrequency);
        }
        executor.shutdownNow();
    }

    private void issueRefund(String auctionAccount, Bid bid) {
        AccountId auctionAccountId = AccountId.fromString(auctionAccount);

        log.info("Refunding " + bid.getBidamount() + " from " + auctionAccount + " to " + bid.getBidderaccountid());
        String memo = Bid.REFUND_MEMO_PREFIX.concat(bid.getTransactionid());
        // issue refund
        if (testing) {
            // just testing, we can't sign a scheduled transaction, just record the state change on the bid
            try {
                bidsRepository.setRefundIssued(bid.getTimestamp(), "");
            } catch (SQLException sqlException) {
                log.error("Failed to set bid refund in progress (bid timestamp " + bid.getTimestamp() + ")");
                log.error(sqlException);
            }
        } else {
            executor.execute(new Runnable() {
                public void run() {
                    try {
                        bidsRepository.setRefundIssuing(bid.getTimestamp());
                        TransactionScheduler transactionScheduler = new TransactionScheduler(auctionAccountId);
                        transactionScheduler.issueScheduledTransactionForRefund(bid, bidsRepository, memo);
                    } catch (Exception e) {
                        log.error(e, e);
                    }
                }
            });
        }
    }
}
