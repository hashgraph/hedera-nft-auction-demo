package com.hedera.demo.auction;

import com.google.errorprone.annotations.Var;
import com.hedera.demo.auction.app.HederaClient;
import com.hedera.demo.auction.app.Utils;
import com.hedera.demo.auction.app.domain.Auction;
import com.hedera.demo.auction.app.domain.Bid;
import com.hedera.demo.auction.app.mirrormapping.MirrorTransaction;
import com.hedera.demo.auction.app.mirrormapping.MirrorTransactions;
import com.hedera.demo.auction.app.repository.AuctionsRepository;
import com.hedera.demo.auction.app.repository.BidsRepository;
import io.vertx.core.json.JsonObject;
import lombok.extern.log4j.Log4j2;
import org.jooq.tools.StringUtils;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Checks if bids that are due a refund have been refunded successfully
 */
@Log4j2
public class RefundChecker implements Runnable {

    protected final AuctionsRepository auctionsRepository;
    protected final BidsRepository bidsRepository;
    protected final int mirrorQueryFrequency;
    protected final HederaClient hederaClient;
    protected boolean runThread = true;
    protected Map<Integer, String> queryTimestamps = new HashMap<Integer, String>();
    protected boolean runOnce = false;

    public RefundChecker(HederaClient hederaClient, AuctionsRepository auctionsRepository, BidsRepository bidsRepository, int mirrorQueryFrequency, boolean runOnce) {
        this.auctionsRepository = auctionsRepository;
        this.bidsRepository = bidsRepository;
        this.mirrorQueryFrequency = mirrorQueryFrequency;
        this.hederaClient = hederaClient;
            this.runOnce = runOnce;
    }

    /**
     * continuous loop except if runThread is false which checks for bids needing a refund
     * Pauses a few seconds between each loop
     */
    @Override
    public void run() {
        log.info("Checking for bid refunds");
        while (runThread) {
            if (this.runOnce) {
                while (watchRefunds()) {
                    log.info("Looking for refunded bids");
                };
                log.info("Caught up with refunds");
                this.runThread = false;
            } else {
                watchRefunds();
                Utils.sleep(this.mirrorQueryFrequency);
            }
        }
    }

    public void stop() {
        runThread = false;
    }

    /**
     * For each of the auctions in the database, finds out the lowest timestamp for all bids
     * that are awaiting a refund.
     * Queries mirror node for CRYPTOTRANSFER transactions after that timestamp
     * Processes the response
     * exits when no refunds were found to process
     *
     * @return boolean if refunds were found and processed
     */
    private boolean watchRefunds() {
        String uri = "/api/v1/transactions";

        ExecutorService executor = Executors.newFixedThreadPool(1);
        @Var boolean foundRefundsToCheck = false;
        try {
            List<Auction> auctions = auctionsRepository.getAuctionsList();
            for (Auction auction: auctions) {
                try {
                    if ( ! queryTimestamps.containsKey(auction.getId())) {
                        String firstBidTimestamp = bidsRepository.getFirstBidToRefund(auction.getId());
                        if (! StringUtils.isEmpty(firstBidTimestamp)) {
                            queryTimestamps.put(auction.getId(), firstBidTimestamp);
                        }
                    }
                    @Var String queryFromTimestamp = queryTimestamps.get(auction.getId());
                    while (!StringUtils.isEmpty(queryFromTimestamp)) {

                        Map<String, String> queryParameters = new HashMap<>();
                        queryParameters.put("account.id", auction.getAuctionaccountid());
                        queryParameters.put("transactiontype", "CRYPTOTRANSFER");
                        queryParameters.put("order", "asc");
                        queryParameters.put("timestamp", "gt:".concat(queryFromTimestamp));
                        Future<JsonObject> future = executor.submit(Utils.queryMirror(hederaClient, uri, queryParameters));

                        JsonObject body = future.get();
                        MirrorTransactions mirrorTransactions = body.mapTo(MirrorTransactions.class);
                        if (handleResponse(mirrorTransactions)) {
                            foundRefundsToCheck = true;
                        }
                        queryFromTimestamp = Utils.getTimestampFromMirrorLink(mirrorTransactions.links.next);
                        if (StringUtils.isEmpty(queryFromTimestamp)) {
                            int transactionCount = mirrorTransactions.transactions.size();
                            if (transactionCount > 0) {
                                queryFromTimestamp = mirrorTransactions.transactions.get(transactionCount - 1).consensusTimestamp;
                            }
                        }
                        if (! StringUtils.isEmpty(queryFromTimestamp)) {
                            queryTimestamps.put(auction.getId(), queryFromTimestamp);
                        }
                    }
                } catch (SQLException e) {
                    log.error("unable to fetch first bid to refund", e);
                } catch (InterruptedException e) {
                    log.error("error occurred getting future", e);
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    log.error("error occurred getting future", e);
                }
            }
        } catch (SQLException e) {
            log.error("Unable to fetch auctions list", e);
        }
        executor.shutdown();
        return foundRefundsToCheck;
    }

    /**
     * Handles the response from a mirror node containing transactions to check
     * For each of the transactions in mirrorTransactions, check if the transaction has a memo matching
     * the refund transaction memo prefix.
     * If memo is matching and the transaction is successful, extract the bid transaction id from the memo and set the memo to refunded.
     * If memo is matching and the transaction failed, set the corresponding bid status to pending so the refund can be attempted again
     *
     * @param mirrorTransactions a list of transactions to process
     * @return boolean true if the status of a bid to be refunded has been updated
     */
    public boolean handleResponse(MirrorTransactions mirrorTransactions) {
        @Var boolean refundsProcessed = false;
        for (MirrorTransaction transaction : mirrorTransactions.transactions) {
            String transactionMemo = transaction.getMemoString();
            log.debug("Memo {}", transactionMemo);
            if (transactionMemo.contains(Bid.REFUND_MEMO_PREFIX)) {
                String bidTransactionId = transaction.getMemoString().replace(Bid.REFUND_MEMO_PREFIX,"");
                if (transaction.isSuccessful()) {
                    // set bid refund complete
                    log.debug("Found successful refund transaction on {} for bid transaction id {}", transaction.consensusTimestamp, bidTransactionId);
                    try {
                        if (bidsRepository.setRefunded(bidTransactionId, transaction.transactionId, transaction.getTransactionHashString())) {
                            refundsProcessed = true;
                        }
                    } catch (SQLException e) {
                        log.error("Error setting bid to refunded (bid transaction id {})", bidTransactionId, e);
                    }
                } else {
                    // set bid refund pending
                    log.debug("Found failed refund transaction on {} for bid transaction id {}", transaction.consensusTimestamp, bidTransactionId);
                    try {
                        if (bidsRepository.setRefundPending(bidTransactionId)) {
                            refundsProcessed = true;
                        }
                    } catch (SQLException e) {
                        log.error("Error setting bid to refund pending (bid transaction id {})", bidTransactionId, e);
                    }
                }
            }
        }
        return refundsProcessed;
    }
}
