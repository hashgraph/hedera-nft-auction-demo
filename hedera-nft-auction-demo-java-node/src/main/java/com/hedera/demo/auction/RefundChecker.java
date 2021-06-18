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
import io.vertx.ext.web.client.WebClient;
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

@Log4j2
public class RefundChecker implements Runnable {

    protected final WebClient webClient;
    protected final AuctionsRepository auctionsRepository;
    protected final BidsRepository bidsRepository;
    protected final int mirrorQueryFrequency;
    protected final HederaClient hederaClient;
    protected boolean runThread = true;
    protected Map<Integer, String> queryTimestamps = new HashMap<Integer, String>();

    public RefundChecker(HederaClient hederaClient, WebClient webClient, AuctionsRepository auctionsRepository, BidsRepository bidsRepository, int mirrorQueryFrequency) {
        this.webClient = webClient;
        this.auctionsRepository = auctionsRepository;
        this.bidsRepository = bidsRepository;
        this.mirrorQueryFrequency = mirrorQueryFrequency;
        this.hederaClient = hederaClient;
    }

    @Override
    public void run() {

        log.info("Checking for bid refunds");
        while (runThread) {
            watchRefunds();
            Utils.sleep(this.mirrorQueryFrequency);
        }
    }

    public void stop() {
        runThread = false;
    }

    public boolean handleResponse(MirrorTransactions mirrorTransactions) {
        @Var boolean refundsProcessed = false;
        for (MirrorTransaction transaction : mirrorTransactions.transactions) {
            String transactionMemo = transaction.getMemoString();
            log.info("Memo " + transactionMemo);
            if (transactionMemo.contains(Bid.REFUND_MEMO_PREFIX)) {
                String bidTransactionId = transaction.getMemoString().replace(Bid.REFUND_MEMO_PREFIX,"");
                if (transaction.isSuccessful()) {
                    // set bid refund complete
                    log.debug("Found successful refund transaction on " + transaction.consensusTimestamp + " for bid transaction id " + bidTransactionId);
                    try {
                        if (bidsRepository.setRefunded(bidTransactionId, transaction.transactionId, transaction.getTransactionHashString())) {
                            refundsProcessed = true;
                        }
                    } catch (SQLException sqlException) {
                        log.error("Error setting bid to refunded (bid transaction id + " + bidTransactionId + ")");
                        log.error(sqlException);
                    }
                } else {
                    // set bid refund pending
                    log.debug("Found failed refund transaction on " + transaction.consensusTimestamp + " for bid transaction id " + bidTransactionId);
                    try {
                        if (bidsRepository.setRefundPending(bidTransactionId)) {
                            refundsProcessed = true;
                        }
                    } catch (SQLException sqlException) {
                        log.error("Error setting bid to refund pending (bid transaction id + " + bidTransactionId + ")");
                        log.error(sqlException);
                    }
                }
            }
        }
        return refundsProcessed;
    }

    public void runOnce() {
        while (watchRefunds()) {
            log.info("Looking for refunded bids");
        };
        log.info("Caught up with refunds");
    }

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
                        Future<JsonObject> future = executor.submit(Utils.queryMirror(webClient, hederaClient, uri, queryParameters));

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
                } catch (SQLException sqlException) {
                    log.error("unable to fetch first bid to refund");
                    log.error(sqlException);
                } catch (InterruptedException e) {
                    log.error("error occurred getting future");
                    log.error(e);
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    log.error("error occurred getting future");
                    log.error(e);
                }
            }
        } catch (SQLException e) {
            log.error("Unable to fetch auctions list");
            log.error(e);
        }
        executor.shutdown();
        return foundRefundsToCheck;
    }

}
