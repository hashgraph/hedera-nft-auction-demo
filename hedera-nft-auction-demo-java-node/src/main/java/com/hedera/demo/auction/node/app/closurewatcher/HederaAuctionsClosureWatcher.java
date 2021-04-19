package com.hedera.demo.auction.node.app.closurewatcher;

import com.hedera.demo.auction.node.app.HederaClient;
import com.hedera.demo.auction.node.app.repository.AuctionsRepository;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.codec.BodyCodec;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class HederaAuctionsClosureWatcher extends AbstractAuctionsClosureWatcher implements AuctionClosureWatcherInterface {

    public HederaAuctionsClosureWatcher(HederaClient hederaClient, WebClient webClient, AuctionsRepository auctionsRepository, int mirrorQueryFrequency, boolean transferOnWin, String refundKey) {
        super(hederaClient, webClient, auctionsRepository, mirrorQueryFrequency, transferOnWin, refundKey);
    }

    @Override
    public void watch() {
        var webQuery = webClient
                .get(mirrorURL, "/api/v1/transactions")
                .as(BodyCodec.jsonObject())
                .addQueryParam("limit", "1"); // only need one row (the latest)

        while (runThread) {
            webQuery.send(response -> {
                if (response.succeeded()) {

                    JsonObject body = response.result().body();
                    try {
                        handleResponse(body);
                    } catch (Exception e) {
                        log.error(e);
                    }
                } else {
                    log.error(response.cause().getMessage());
                }
            });

            try {
                Thread.sleep(this.mirrorQueryFrequency);
            } catch (InterruptedException e) {
                e.printStackTrace();
                log.error(e);
            }
        }
    }
}
