package com.hedera.demo.auction.node.app.refundChecker;

import com.google.errorprone.annotations.Var;
import com.hedera.demo.auction.node.app.HederaClient;
import com.hedera.demo.auction.node.app.Utils;
import com.hedera.demo.auction.node.app.domain.Auction;
import com.hedera.demo.auction.node.app.mirrormapping.MirrorTransactions;
import com.hedera.demo.auction.node.app.repository.AuctionsRepository;
import com.hedera.demo.auction.node.app.repository.BidsRepository;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.codec.BodyCodec;
import lombok.extern.log4j.Log4j2;
import org.jooq.tools.StringUtils;

import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Log4j2
public class HederaRefundChecker extends AbstractRefundChecker implements RefundCheckerInterface {

    public HederaRefundChecker(HederaClient hederaClient, WebClient webClient, AuctionsRepository auctionsRepository, BidsRepository bidsRepository, int mirrorQueryFrequency) {
        super(hederaClient, webClient, auctionsRepository, bidsRepository, mirrorQueryFrequency);
    }

    @Override
    public void watch() {
        while (runThread) {
            watchRefunds();
            Utils.sleep(this.mirrorQueryFrequency);
        }
    }

    @Override
    public void watchOnce() {
        while (watchRefunds()) {
            log.info("Looking for refunded bids");
        };
        log.info("Caught up with refunds");
    }

    private boolean watchRefunds() {
        ExecutorService executor = Executors.newFixedThreadPool(1);
        @Var boolean foundRefundsToCheck = false;
        try {
            List<Auction> auctions = auctionsRepository.getAuctionsList();
            for (Auction auction: auctions) {
                try {
                    @Var String queryFromTimestamp = bidsRepository.getFirstBidToRefund(auction.getId());
                    while (!StringUtils.isEmpty(queryFromTimestamp)) {
                        Future<JsonObject> future = executor.submit(queryMirror(auction.getAuctionaccountid(), queryFromTimestamp));
                        JsonObject body = future.get();
                        MirrorTransactions mirrorTransactions = body.mapTo(MirrorTransactions.class);
                        if (handleResponse(mirrorTransactions)) {
                            foundRefundsToCheck = true;
                        }
                        queryFromTimestamp = Utils.getTimestampFromMirrorLink(mirrorTransactions.links.next);
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

    private Callable<JsonObject> queryMirror(String auctionAccountId, String timestampFrom) {
        return () -> {
            var webQuery = webClient
                    .get(mirrorURL, "/api/v1/transactions")
                    .addQueryParam("account.id", auctionAccountId)
                    .addQueryParam("transactiontype", "CRYPTOTRANSFER")
                    .addQueryParam("order", "asc")
                    .addQueryParam("timestamp", "gt:".concat(timestampFrom));

            log.debug("Checking for refunds on account " + auctionAccountId);

            CompletableFuture<JsonObject> future = new CompletableFuture<>();

            webQuery.as(BodyCodec.jsonObject())
                    .send()
                    .onSuccess(response -> {
                        try {
                            future.complete(response.body());
                        } catch (RuntimeException e) {
                            log.error(e);
                            future.complete(new JsonObject());
                        }
                    })
                    .onFailure(err -> {
                                log.error(err.getMessage());
                                future.complete(new JsonObject());
                            }
                    );

            return future.get();
        };
    }
}
