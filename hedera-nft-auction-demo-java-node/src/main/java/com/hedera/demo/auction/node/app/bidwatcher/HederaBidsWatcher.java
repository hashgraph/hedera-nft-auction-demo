package com.hedera.demo.auction.node.app.bidwatcher;

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

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Log4j2
public class HederaBidsWatcher extends AbstractBidsWatcher implements BidsWatcherInterface {

    // private auction object so that we don't interfere with the abstract instance
    private Auction watchedAuction;

    public HederaBidsWatcher(HederaClient hederaClient, WebClient webClient, AuctionsRepository auctionsRepository, BidsRepository bidsRepository, int auctionId, String refundKey, int mirrorQueryFrequency) {
        super(hederaClient, webClient, auctionsRepository, bidsRepository, auctionId, refundKey, mirrorQueryFrequency);
    }

    private Callable<JsonObject> queryMirror(String auctionAccountId, String timestampFrom) {
        return () -> {
            var webQuery = webClient
                    .get(mirrorURL, "/api/v1/transactions")
                    .addQueryParam("account.id", auctionAccountId)
                    .addQueryParam("transactiontype", "CRYPTOTRANSFER")
                    .addQueryParam("order", "asc")
                    .addQueryParam("timestamp", "gt:".concat(timestampFrom));

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
                    });


            return future.get();
        };
    }

    @Override
    public void watch() {

        @Var String nextLink = "";

        ExecutorService executor = Executors.newFixedThreadPool(1);
        while (runThread ) {
            try {
                // reload auction from database
                watchedAuction = auctionsRepository.getAuction(auctionId);

                log.debug("Checking for bids on account " + watchedAuction.getAuctionaccountid() + " and token " + watchedAuction.getTokenid());

                String consensusTimeStampFrom = StringUtils.isEmpty(nextLink) ? watchedAuction.getLastconsensustimestamp() : nextLink;
                nextLink = "";

                Future<JsonObject> future = executor.submit(queryMirror(auction.getAuctionaccountid(), consensusTimeStampFrom));

                try {
                    JsonObject response = future.get();
                    if (response != null) {
                        MirrorTransactions mirrorTransactions = response.mapTo(MirrorTransactions.class);
                        nextLink = Utils.getTimestampFromMirrorLink(mirrorTransactions.links.next);
                        handleResponse(mirrorTransactions);
                    }
                } catch (InterruptedException interruptedException) {
                    log.error(interruptedException);
                    Thread.currentThread().interrupt();
                } catch (ExecutionException executionException) {
                    log.error(executionException);
                }

            } catch (Exception e) {
                log.error(e);
            }
            if (StringUtils.isEmpty(nextLink)) {
                Utils.sleep(this.mirrorQueryFrequency);
            }
        }
        executor.shutdown();
    }
}
