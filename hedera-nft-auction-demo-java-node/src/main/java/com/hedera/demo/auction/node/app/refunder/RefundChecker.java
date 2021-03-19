package com.hedera.demo.auction.node.app.refunder;

import com.google.errorprone.annotations.Var;
import com.hedera.demo.auction.node.app.HederaClient;
import com.hedera.demo.auction.node.app.repository.BidsRepository;
import io.github.cdimascio.dotenv.Dotenv;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.codec.BodyCodec;
import lombok.extern.log4j.Log4j2;

import java.util.Map;
import java.util.Optional;

@Log4j2
public class RefundChecker implements Runnable {

    private final WebClient webClient;
    private final BidsRepository bidsRepository;
    private final int mirrorQueryFrequency;
    private String mirrorURL = "";
    private final String mirrorProvider = HederaClient.getMirrorProvider();

    public RefundChecker(WebClient webClient, BidsRepository bidsRepository, Dotenv env) throws Exception {
        this.webClient = webClient;
        this.bidsRepository = bidsRepository;
        this.mirrorQueryFrequency = Integer.parseInt(Optional.ofNullable(env.get("MIRROR_QUERY_FREQUENCY")).orElse("5000"));
        this.mirrorURL = HederaClient.getMirrorUrl();
    }

    @Override
    public void run() {

        log.info("Checking for bid refunds");

        @Var String uri = "";

        switch (mirrorProvider) {
            case "HEDERA":
                uri = "/api/v1/transactions";
                break;
            case "DRAGONGLASS":
                //TODO: Handle dragonglass mirror
                break;
            default:
                //TODO: Handle kabuto mirror
                break;
        }

        while (true) {
            // get list of bids to refund
            for (Map.Entry<String, String> bidToRefund : bidsRepository.bidsToRefund().entrySet()) {
                String transactionId = bidToRefund.getValue();
                String timestamp = bidToRefund.getKey();
                log.debug("Checking for refund on timestamp " + timestamp + " transaction id " + transactionId);

                if (mirrorProvider.equals("HEDERA")) {
                    String txURI = uri.concat("/").concat(transactionId);
                    var webQuery =
                    webClient
                            .get(443, this.mirrorURL, txURI)
//TODO:
//                            .addQueryParam("scheduled", true)
                            .ssl(true);

                            webQuery.as(BodyCodec.jsonObject())
                            .send(response -> {
                                if (response.succeeded()) {
                                    JsonObject body = response.result().body();
                                    try {
                                        handleHederaResponse(body, timestamp, transactionId);
                                    } catch (RuntimeException e) {
                                        log.error(e);
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
            }
            try {
                Thread.sleep(this.mirrorQueryFrequency);
            } catch (InterruptedException e) {
                e.printStackTrace();
                log.error(e);
            }
        }
    }

    private void handleHederaResponse(JsonObject response, String timestamp, String transactionId) {
        try {
            JsonArray transactions = response.getJsonArray("transactions");
            if (transactions != null) {
                for (Object transactionObject : transactions) {
                    JsonObject transaction = JsonObject.mapFrom(transactionObject);
                    if (transaction.getString("result").equals("SUCCESS")) {
                        // set refunded to true
                        log.debug("Found successful refund transaction on " + timestamp + " transaction id " + transactionId);
                        bidsRepository.setRefunded(timestamp);
                    } else {
                        log.debug("Refund transaction on " + timestamp + " transaction id " + transactionId + " failed: " + transaction.getString("result"));
                    }
                }
            } else {
                log.debug("No " + transactionId + " transaction found");
            }
        } catch (RuntimeException e) {
            log.error(e);
        }
    }
}
