package com.hedera.demo.auction.node.app.winnertokentransfer;

import com.hedera.demo.auction.node.app.HederaClient;
import com.hedera.demo.auction.node.app.repository.AuctionsRepository;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.codec.BodyCodec;
import lombok.extern.log4j.Log4j2;

import java.sql.SQLException;

@Log4j2
public class HederaWinnerTokenTransfer extends AbstractWinnerTokenTransfer implements WinnerTokenTransferInterface {

    public HederaWinnerTokenTransfer(HederaClient hederaClient, WebClient webClient, AuctionsRepository auctionsRepository, String tokenId, String winningAccountId) {
        super(hederaClient, webClient, auctionsRepository, tokenId, winningAccountId);
    }

    /**
     * check association of token with winner
     */
    @Override
    public void checkAssociation() {

        String uri = "/api/v1/tokens/".concat(tokenId).concat("/balances");

        var webQuery  = webClient
                .get(mirrorURL, uri)
                .as(BodyCodec.jsonObject())
                .addQueryParam("account.id", winningAccountId);

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
        // if the token is associated with the account, it will be in the response
        // the actual balance is not important, a balance of 0 means associated in any case
        if (body.containsKey("balances")) {
            JsonArray balances = body.getJsonArray("balances");
            if (balances.size() != 0) {
                for (Object balanceObject : balances) {
                    JsonObject balance = JsonObject.mapFrom(balanceObject);
                    if (balance.getString("account").equals(winningAccountId)) {
                        auctionsRepository.setTransferring(tokenId);
                    }
                }
            }
        }
    }
}
