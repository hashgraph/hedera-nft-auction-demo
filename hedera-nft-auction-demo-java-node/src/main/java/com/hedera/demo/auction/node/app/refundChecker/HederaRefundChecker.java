package com.hedera.demo.auction.node.app.refundChecker;

import com.hedera.demo.auction.node.app.HederaClient;
import com.hedera.demo.auction.node.app.Utils;
import com.hedera.demo.auction.node.app.repository.BidsRepository;
import io.github.cdimascio.dotenv.Dotenv;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.codec.BodyCodec;
import lombok.extern.log4j.Log4j2;

import java.sql.SQLException;
import java.util.Map;

@Log4j2
public class HederaRefundChecker extends AbstractRefundChecker implements RefundCheckerInterface {

    public HederaRefundChecker(HederaClient hederaClient, WebClient webClient, BidsRepository bidsRepository, Dotenv env) {
        super(hederaClient, webClient, bidsRepository, env);
    }

    @Override
    public void watch() throws SQLException {

        log.info("Checking for bid refunds");

        String uri = "/api/v1/transactions";

        while (true) {
            // get list of bids where refund is in progress (refunded = false)
            // look for a successful scheduled transaction
            // and set bid accordingly (refunded)
            for (Map.Entry<String, String> bidToRefund : bidsRepository.bidsRefundToConfirm().entrySet()) {
                String transactionId = bidToRefund.getValue();
                String timestamp = bidToRefund.getKey();
                log.debug("Checking for refund on timestamp " + timestamp + " transaction id " + transactionId);

                String txURI = uri.concat("/").concat(Utils.hederaMirrorTransactionId(transactionId));
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
}
