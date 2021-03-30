package com.hedera.demo.auction.node.app.winnertokentransferwatcher;

import com.google.errorprone.annotations.Var;
import com.hedera.demo.auction.node.app.domain.Auction;
import com.hedera.demo.auction.node.app.repository.AuctionsRepository;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.codec.BodyCodec;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.codec.binary.Hex;

import java.sql.SQLException;
import java.util.Base64;

@Log4j2
public class HederaWinnerWinnerTokenTransferWatcher extends AbstractWinnerTokenTransferWatcher implements WinnerTokenTransferWatcherInterface {

    public HederaWinnerWinnerTokenTransferWatcher(WebClient webClient, AuctionsRepository auctionsRepository, Auction auction) throws Exception {
        super(webClient, auctionsRepository, auction);
    }

    @Override
    public void check() {
        log.info("Checking for token transfer transaction status");

        @Var String transactionId = auction.getTransfertxid();
        transactionId = transactionId.replace("@","-");
        transactionId = transactionId.replace(".","-");
        transactionId = transactionId.replace("0-0-", "0.0.");

        String uri = "/api/v1/transactions".concat("/").concat(transactionId);
        log.debug("Checking for status on transaction id " + transactionId);

        var webQuery = webClient
                .get(this.mirrorURL, uri)
//TODO:
//                            .addQueryParam("scheduled", true)
                .as(BodyCodec.jsonObject());

        webQuery.send(response -> {
            if (response.succeeded()) {
                JsonObject body = response.result().body();
                try {
                    handleResponse(body, auction);
                } catch (RuntimeException e) {
                    log.error(e);
                }
            } else {
                log.error(response.cause().getMessage());
            }
        });
    }

    private void handleResponse(JsonObject response, Auction auction) {
        try {
            JsonArray transactions = response.getJsonArray("transactions");
            if (transactions != null) {
                for (Object transactionObject : transactions) {
                    JsonObject transaction = JsonObject.mapFrom(transactionObject);
                    if (transaction.getString("result").equals("SUCCESS")) {
                        // token transfer was successful
                        log.debug("Found successful token transfer transaction");
                        byte[] txHashBytes = Base64.getDecoder().decode(transaction.getString("transaction_hash"));
                        String hash = Hex.encodeHexString(txHashBytes);
                        auctionsRepository.setEnded(auction.getId(), hash);
                    } else {
                        log.debug("Token transfer transaction id " + auction.getTransfertxid() + " failed: " + transaction.getString("result"));
                    }
                }
            } else {
                log.debug("No " + auction.getTransfertxid() + " transaction found");
            }
        } catch (RuntimeException | SQLException e) {
            log.error(e);
        }
    }
}
