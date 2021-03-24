package com.hedera.demo.auction.node.app.refunder;

import com.hedera.demo.auction.node.app.repository.BidsRepository;
import io.github.cdimascio.dotenv.Dotenv;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.codec.BodyCodec;
import lombok.extern.log4j.Log4j2;

import java.util.Map;

@Log4j2
public class HederaRefundChecker extends AbstractRefundChecker implements RefundCheckerInterface {

    public HederaRefundChecker(WebClient webClient, BidsRepository bidsRepository, Dotenv env) throws Exception {
        super(webClient, bidsRepository, env);
    }

    @Override
    public void watch() {

        log.info("Checking for bid refunds");

        String uri = "/api/v1/transactions";

        while (true) {
            // get list of bids to refund (refund = false, but refund transaction in progress)
            // look for a successful scheduled transaction
            // and set bid accordingly (refunded)
            for (Map.Entry<String, String> bidToRefund : bidsRepository.bidsToRefund().entrySet()) {
                String transactionId = bidToRefund.getValue();
                String timestamp = bidToRefund.getKey();
                log.debug("Checking for refund on timestamp " + timestamp + " transaction id " + transactionId);

                String txURI = uri.concat("/").concat(transactionId);
                var webQuery =
                webClient
                    .get(this.mirrorURL, txURI)
//TODO:
//                            .addQueryParam("scheduled", true)
                    .as(BodyCodec.jsonObject());

                webQuery
                        .send(response -> {
                            if (response.succeeded()) {
                                JsonObject body = response.result().body();
                                try {
                                    handleResponse(body, timestamp, transactionId);
                                } catch (RuntimeException e) {
                                    log.error(e);
                                }
                            } else {
                                log.error(response.cause().getMessage());
                            }
                        });
            }
            try {
                Thread.sleep(this.mirrorQueryFrequency);
            } catch (InterruptedException e) {
                e.printStackTrace();
                log.error(e);
            }
        }
    }

    private void handleResponse(JsonObject response, String timestamp, String transactionId) {
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
