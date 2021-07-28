package com.hedera.demo.auction;

import com.google.common.base.Splitter;
import com.hedera.demo.auction.app.HederaClient;
import com.hedera.demo.auction.app.Utils;
import com.hedera.demo.auction.app.Utils.ScheduledStatus;
import com.hedera.demo.auction.app.domain.Auction;
import com.hedera.demo.auction.app.domain.Bid;
import com.hedera.demo.auction.app.repository.AuctionsRepository;
import com.hedera.demo.auction.app.repository.BidsRepository;
import com.hedera.demo.auction.app.scheduledoperations.TransactionScheduler;
import com.hedera.hashgraph.sdk.AccountId;
import lombok.extern.log4j.Log4j2;
import org.jooq.tools.StringUtils;

import java.sql.SQLException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * This class is responsible for identifying bids to refund and submitting the refund scheduled transaction
 * It is also checking for outstanding refunds that have been processed but failed to refund
 * by resetting the bid's refund status to pending if appropriate once an hour (when the hour changes).
 * The time is determined by querying a mirror node for its latest consensus timestamp
 * The purpose of checking for the hourly change is to ensure that all participating nodes resubmit
 * refund scheduled transactions more or less at the same time. Indeed a scheduled transaction's lifetime
 * is 30 minutes maximum, if all nodes submit at very different time, there's a possibility the resulting
 * scheduled transactions will never execute
 */
@Log4j2
public class Refunder implements Runnable {
    private final BidsRepository bidsRepository;
    private final AuctionsRepository auctionsRepository;
    private final HederaClient hederaClient;
    private final int mirrorQueryFrequency;
    private boolean runThread = true;
    private final ExecutorService executor;
    private final Map<String, String> refundsInProgress = new HashMap();
    private int lastErrorCheckHour = -1;

    /**
     * Constructor
     * @param hederaClient the hedera client to use for communicating with Hedera
     * @param auctionsRepository the auction repository
     * @param bidsRepository the bids repository
     * @param mirrorQueryFrequency the time to sleep in seconds between mirror queries
     */
    public Refunder(HederaClient hederaClient, AuctionsRepository auctionsRepository, BidsRepository bidsRepository, int mirrorQueryFrequency, int refundThreads) {
        this.auctionsRepository = auctionsRepository;
        this.bidsRepository = bidsRepository;
        this.hederaClient = hederaClient;
        this.mirrorQueryFrequency = mirrorQueryFrequency;
        this.executor = Executors.newFixedThreadPool(refundThreads);
    }

    /**
     * Stops the thread
     */
    public void stop() {
        runThread = false;
    }

    /**
     * Get the current list of auctions
     * For each auction, if we're processing refunds, identify bids to refund and issue a scheduled transaction for the refund
     * Once all bids and all auctions have been checked, see if any outstanding refunds have to be reprocessed
     */
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
                for (Auction auction : auctions) {
                    if (auction.getProcessrefunds()) {
                        try {
                            List<Bid> bidsToRefund = bidsRepository.getBidsToRefund(auction.getId());
                            if (bidsToRefund != null) {
                                for (Bid bid : bidsToRefund) {
                                    issueRefund(auction.getAuctionaccountid(), bid);
                                }
                            }
                        } catch (SQLException e) {
                            log.error("unable to fetch bids to refund", e);
                        } catch (RuntimeException e) {
                            log.error("error issuing refund", e);
                        }
                    }
                }
            } catch (SQLException e) {
                log.error("unable to fetch auctions list", e);
            }

            checkOutstandingRefunds();

            Utils.sleep(this.mirrorQueryFrequency);
        }
        executor.shutdownNow();
    }

    /**
     * Issues a refund via a scheduled transaction unless the testing flag is enabled in which
     * case, the bid refund is set to "ISSUED"
     * Changes the status of the bid to "ISSUING" and submits a scheduled transaction to effect the refund
     * via a multi-thread executor to speed up issuing refunds in the event of high demand
     * Also maintains a map of refunds that are in the executor queue
     *
     * @param auctionAccount the auction account to use for the refund
     * @param bid the bid to refund
     */
    private void issueRefund(String auctionAccount, Bid bid) {
        AccountId auctionAccountId = AccountId.fromString(auctionAccount);

        log.info("Refunding {} from {} to {}", bid.getBidamount(), auctionAccount, bid.getBidderaccountid());
        String memo = Bid.REFUND_MEMO_PREFIX.concat(bid.getTransactionid());
        // issue refund
        try {
            if (bidsRepository.setRefundIssuing(bid.getTimestamp())) {
                refundsInProgress.put(bid.getTimestamp(), "");
                CompletableFuture.supplyAsync(() -> {
                    try {
                        TransactionScheduler transactionScheduler = new TransactionScheduler(auctionAccountId);
                        transactionScheduler.issueScheduledTransactionForRefund(bid, bidsRepository, memo);
                        return bid.getTimestamp();
                    } catch (Throwable e) {
                        log.error(e, e);
                        return "";
                    }
                }, executor)
                .thenAccept(refundingBid -> {
                    if (!StringUtils.isEmpty(refundingBid)) {
                        refundsInProgress.remove(refundingBid);
                    }
                })
                .exceptionally(exception -> {
                    log.error(exception, exception);
                    return null;
                });
            }
        } catch (SQLException e) {
            log.error("Unable to set bid to refund issuing", e);
        }
    }

    /**
     * Queries a mirror node for the latest consensus timestamp on a transaction
     * if the resulting consensus timestamp is a new hour (e.g. last was 14:10, new is 15:01)
     * looks for bids that should have refunded and have not.
     * If the bid is still in the queue, do nothing
     * If the bid is no longer in the queue and there's no schedule id on the bid, reset to "PENDING"
     * If the bid is no longer in the queue and the schedule on mirror has not executed, reset to "PENDING"
     */

    private void checkOutstandingRefunds() {
        // get last mirror timestamp
        String lastMirrorTimeStamp = Utils.getLastConsensusTimeFromMirror(hederaClient);
        if (! StringUtils.isEmpty(lastMirrorTimeStamp)) {
            // we have a consensus time in seconds.nanos, strip the nanos
            List<String> timeStampParts = Splitter.on('.').splitToList(lastMirrorTimeStamp);
            if (timeStampParts.size() > 0) {
                // get seconds since epoch
                long seconds = Long.parseLong(timeStampParts.get(0));
                Instant instant = Instant.ofEpochSecond(seconds);
                int currentHour = instant.atZone(ZoneOffset.UTC).getHour();
                if (lastErrorCheckHour == -1) {
                    // set the hour to be the current hour from the timestamp
                    lastErrorCheckHour = currentHour;
                }
                if (currentHour != lastErrorCheckHour) {
                    // we've jumped into a different hour, check outstanding refunds
                    lastErrorCheckHour = currentHour;
                    log.debug("Checking for outstanding refunds to re-process");
                    // use the map of refunds in queue to check refunds are not being attempted already
                    // and running late
                    try {
                        List<Auction> auctions = auctionsRepository.getAuctionsList();
                        for (Auction auction : auctions) {
                            if (auction.getProcessrefunds()) {
                                try {
                                    // get list of ISSUING, ISSUED or ERROR bids
                                    List<Bid> outstandingRefunds = bidsRepository.getOustandingRefunds();
                                    for (Bid bid : outstandingRefunds) {
                                        if (!refundsInProgress.containsKey(bid.getTimestamp())) {
                                            // not currently queued, attempt retry
                                            // is the schedule complete ?
                                            if (!StringUtils.isEmpty(bid.getScheduleId())) {
                                                // check status of schedule on mirror node
                                                // if it has executed, the refund checker will pick it up
                                                // else, reset to pending
                                                // note, unknown could also be returned in the event of an error, don't reschedule in that event
                                                if (Utils.scheduleHasExecuted(hederaClient, bid.getScheduleId(), seconds) == ScheduledStatus.NOT_EXECUTED) {
                                                    bidsRepository.setRefundPending(bid.getTransactionid());
                                                }
                                            } else {
                                                bidsRepository.setRefundPending(bid.getTransactionid());
                                            }
                                        }
                                    }
                                } catch(SQLException e){
                                    log.error("Unable to get list of bids", e);
                                }
                            }
                        }
                    } catch(SQLException e){
                        log.error("Unable to get list of auctions", e);
                    }
                }
            }
        }
    }
}
