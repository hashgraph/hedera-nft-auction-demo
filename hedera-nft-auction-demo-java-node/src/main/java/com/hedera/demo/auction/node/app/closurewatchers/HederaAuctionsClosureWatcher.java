package com.hedera.demo.auction.node.app.closurewatchers;

import com.google.errorprone.annotations.Var;
import com.hedera.demo.auction.node.app.repository.AuctionsRepository;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.codec.BodyCodec;
import lombok.extern.log4j.Log4j2;

import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

@Log4j2
public class HederaAuctionsClosureWatcher extends AbstractAuctionsClosureWatcher {

    public HederaAuctionsClosureWatcher(WebClient webClient, AuctionsRepository auctionsRepository, int mirrorQueryFrequency) throws Exception {
        super(webClient, auctionsRepository, mirrorQueryFrequency);
    }

    public void watch() {
        AtomicReference<String> uri = new AtomicReference<>("");

        uri.set("/api/v1/transactions");
        var webQuery = webClient
                .get(443, mirrorURL, uri.get())
                .ssl(true)
                .as(BodyCodec.jsonObject())
                .addQueryParam("limit", "1"); // only need one row (the latest)

        while (true) {
            webQuery.send(response -> {
                if (response.succeeded()) {
                    JsonObject body = response.result().body();
                    JsonArray transactions = body.getJsonArray("transactions");
                    if (transactions != null) {
                        @Var String consensusTimestamp = "";
                        for (Object transactionObject : transactions) {
                            JsonObject transaction = JsonObject.mapFrom(transactionObject);
                            consensusTimestamp = transaction.getString("consensus_timestamp");
                            break;
                        }

                        // AUCTIONS.ENDTIMESTAMP, AUCTIONS.ID
                        for (Map.Entry<String, Integer> auctions : auctionsRepository.openPendingAuctions().entrySet()) {
                            String endTimestamp = auctions.getKey();
                            int auctionId = auctions.getValue();

                            if (consensusTimestamp.compareTo(endTimestamp) > 0) {
                                // payment past auctions end, close it
                                log.info("Closing auction id " + auctionId);
                                try {
                                    auctionsRepository.setClosed(auctionId);
                                } catch (SQLException e) {
                                    log.error(e);
                                }
                            }
                        }

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
