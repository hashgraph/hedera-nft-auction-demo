package com.hedera.demo.auction.node.app.refundChecker;

import com.hedera.demo.auction.node.app.HederaClient;
import com.hedera.demo.auction.node.app.Utils;
import com.hedera.demo.auction.node.app.repository.BidsRepository;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.codec.BodyCodec;
import lombok.extern.log4j.Log4j2;

import java.sql.SQLException;
import java.util.Map;

@Log4j2
public class HederaRefundChecker extends AbstractRefundChecker implements RefundCheckerInterface {

    public HederaRefundChecker(HederaClient hederaClient, WebClient webClient, BidsRepository bidsRepository, int mirrorQueryFrequency) {
        super(hederaClient, webClient, bidsRepository, mirrorQueryFrequency);
    }

    @Override
    public void watch() {

        log.info("Checking for bid refunds");

        String uri = "/api/v1/transactions";

        while (runThread) {
            // get list of bids where refund is in progress (refunded = false)
            // look for a successful scheduled transaction
            // and set bid accordingly (refunded)
            Map<String, String> bidsRefundToConfirm = null;
            try {
                bidsRefundToConfirm = bidsRepository.bidsRefundToConfirm();
                for (Map.Entry<String, String> bidToRefund : bidsRefundToConfirm.entrySet()) {
                    String transactionId = bidToRefund.getValue();
                    String timestamp = bidToRefund.getKey();
                    log.debug("Checking for refund on timestamp " + timestamp + " transaction id " + transactionId);

                    String txURI = uri.concat("/").concat(Utils.hederaMirrorTransactionId(transactionId));
                    var webQuery =
                            webClient
                                    .get(this.mirrorURL, txURI)
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
            } catch (SQLException sqlException) {
                log.error("Unable to fetch bids to confirm refund on");
                log.error(sqlException);
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
