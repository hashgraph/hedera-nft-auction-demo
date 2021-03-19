package com.hedera.demo.auction.node.app.auctionwatchers;

import com.google.errorprone.annotations.Var;
import com.hedera.demo.auction.node.app.HederaClient;
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
public class AuctionsClosureWatcher implements Runnable {

    private final WebClient webClient;
    private final AuctionsRepository auctionsRepository;
    private final int mirrorQueryFrequency;
    private String mirrorURL = "";
    private final String mirrorProvider = HederaClient.getMirrorProvider();

    public AuctionsClosureWatcher(WebClient webClient, AuctionsRepository auctionsRepository, int mirrorQueryFrequency) throws Exception {
        this.webClient = webClient;
        this.auctionsRepository = auctionsRepository;
        this.mirrorQueryFrequency = mirrorQueryFrequency;
        this.mirrorURL = HederaClient.getMirrorUrl();
    }

    /**
     * check transaction history for token, if associated update auction status
     * start new bidding monitor thread
     * and close this thread
     */
    @Override
    public void run() {
        AtomicReference<String> uri = new AtomicReference<>("");

        switch (mirrorProvider) {
            case "HEDERA":
                uri.set("/api/v1/transactions");
                break;
            case "DRAGONGLASS":
                //TODO: Handle dragonglass mirror
                uri.set("");
                break;
            default:
                //TODO: Handle kabuto mirror
                uri.set("");
                break;
        }

        while (true) {
            if (mirrorProvider.equals("HEDERA")) {
                webClient
                    .get(443, mirrorURL, uri.get())
                    .ssl(true)
                    .as(BodyCodec.jsonObject())
                    .addQueryParam("limit", "1") // only need one row (the latest)
                    .send(response -> {
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
            } else if (mirrorProvider.equals("KABUTO")) {
                //TODO: Handle kabuto mirror
            } else if (mirrorProvider.equals("DRAGONGLASS")) {
                //TODO: Handle dragonglass mirror
            }

            try {
                Thread.sleep(this.mirrorQueryFrequency);
            } catch (InterruptedException e) {
                e.printStackTrace();
                log.error(e);
            }
        }
    }
}
