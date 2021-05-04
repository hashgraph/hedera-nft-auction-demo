package com.hedera.demo.auction.node.app.auctionendtransfer;

import com.hedera.demo.auction.node.app.HederaClient;
import com.hedera.demo.auction.node.app.repository.AuctionsRepository;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.codec.BodyCodec;
import lombok.extern.log4j.Log4j2;

import java.sql.SQLException;

@Log4j2
public class HederaAuctionEndTransfer extends AbstractAuctionEndTransfer implements AuctionEndTransferInterface {

    public HederaAuctionEndTransfer(HederaClient hederaClient, WebClient webClient, AuctionsRepository auctionsRepository, String tokenId, String winningAccountId) {
        super(hederaClient, webClient, auctionsRepository, tokenId, winningAccountId);
    }

    /**
     * check association of token with winner
     */
    @Override
    public void checkAssociation() {

        String uri = "/api/v1/transactions";

        var webQuery  = webClient
                .get(mirrorURL, uri)
                .as(BodyCodec.jsonObject())
                .addQueryParam("account.id", winningAccountId)
                .addQueryParam("transactiontype", "TOKENASSOCIATE")
                .addQueryParam("result", "SUCCESS");

        webQuery.send(response -> {
            if (response.succeeded()) {
                JsonObject body = response.result().body();
                try {
                    checkForAssociation(body);
                } catch (Exception e) {
                    log.error(e);
                }
            } else {
                log.error(response.cause().getMessage());
            }
        });
    }

    public void checkForAssociation(JsonObject body) throws SQLException {
        if (body.containsKey("transactions")) {
            JsonArray transactions = body.getJsonArray("transactions");

            if (transactions.size() != 0) {
                auctionsRepository.setTransferring(tokenId);
            }
        }
    }
}
