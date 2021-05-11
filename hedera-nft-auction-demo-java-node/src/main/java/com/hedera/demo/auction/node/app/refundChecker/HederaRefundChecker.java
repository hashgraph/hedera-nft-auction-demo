package com.hedera.demo.auction.node.app.refundChecker;

import com.google.errorprone.annotations.Var;
import com.hedera.demo.auction.node.app.HederaClient;
import com.hedera.demo.auction.node.app.domain.Auction;
import com.hedera.demo.auction.node.app.repository.AuctionsRepository;
import com.hedera.demo.auction.node.app.repository.BidsRepository;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.codec.BodyCodec;
import lombok.extern.log4j.Log4j2;

import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

@Log4j2
public class HederaRefundChecker extends AbstractRefundChecker implements RefundCheckerInterface {

    public HederaRefundChecker(HederaClient hederaClient, WebClient webClient, AuctionsRepository auctionsRepository, BidsRepository bidsRepository, int mirrorQueryFrequency) {
        super(hederaClient, webClient, auctionsRepository, bidsRepository, mirrorQueryFrequency);
    }

    @Override
    public void watch() {
        while (runThread) {
            watchRefunds();
            try {
                Thread.sleep(this.mirrorQueryFrequency);
            } catch (InterruptedException e) {
                e.printStackTrace();
                log.error(e);
            }
        }
    }

    @Override
    public void watchOnce() {
        while (watchRefunds()) {
            log.info("Looking for refunded bids");
        };
    }

    private boolean watchRefunds() {
        @Var boolean foundRefundsToCheck = false;
        try {
            List<Auction> auctions = auctionsRepository.getAuctionsList();
            for (Auction auction : auctions) {
                try {
                    String queryFromTimestamp = bidsRepository.getFirstBidToRefund(auction.getId());
                    if ( ! queryFromTimestamp.isBlank()) {
                        CompletableFuture<JsonObject> completableFuture = queryMirror(queryFromTimestamp, auction);

                        JsonObject body = completableFuture.get();
                        if (handleResponse(body)) {
                            foundRefundsToCheck = true;
                        }
                    }
                } catch (SQLException sqlException) {
                    log.error("Unable to fetch first bid to refund");
                    log.error(sqlException);
                }
            }
        } catch (SQLException e) {
            log.error("Unable to fetch list of auctions");
            log.error(e);
        } catch (InterruptedException e) {
            log.error("Error while querying mirror for transactions (interrupted)");
            log.error(e);
        } catch (ExecutionException e) {
            log.error("Error while querying mirror for transactions (execution)");
            log.error(e);
        }
        return foundRefundsToCheck;
    }

    private CompletableFuture<JsonObject> queryMirror(String fromTimestamp, Auction auction) {
        CompletableFuture<JsonObject> completableFuture = new CompletableFuture<>();

        Executors.newCachedThreadPool().submit(() -> {
            var webQuery = webClient
                    .get(mirrorURL, "/api/v1/transactions")
                    .addQueryParam("account.id", auction.getAuctionaccountid())
                    .addQueryParam("transactiontype", "CRYPTOTRANSFER")
                    .addQueryParam("order", "asc")
                    .addQueryParam("timestamp", "gt:".concat(fromTimestamp));

            log.debug("Checking for refunds on account " + auction.getAuctionaccountid());

            webQuery.as(BodyCodec.jsonObject())
                    .send(response -> {
                        if (response.succeeded()) {
                            JsonObject body = response.result().body();
                            try {
                                completableFuture.complete(body);
                                return;
                            } catch (RuntimeException e) {
                                log.error(e);
                                completableFuture.complete(new JsonObject());
                                return;
                            }
                        } else {
                            log.error(response.cause().getMessage());
                            completableFuture.complete(new JsonObject());
                            return;
                        }
                    });

        });
        return completableFuture;
    }
}
