package com.hedera.demo.auction.node.app.auctionendtransferwatcher;

import com.hedera.demo.auction.node.app.HederaClient;
import com.hedera.demo.auction.node.app.Utils;
import com.hedera.demo.auction.node.app.domain.Auction;
import com.hedera.demo.auction.node.app.repository.AuctionsRepository;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.codec.BodyCodec;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class HederaAuctionEndTransferWatcher extends AbstractAuctionEndTransferWatcher implements AuctionEndTransferWatcherInterface {

    public HederaAuctionEndTransferWatcher(HederaClient hederaClient, WebClient webClient, AuctionsRepository auctionsRepository, Auction auction) {
        super(hederaClient, webClient, auctionsRepository, auction);
    }

    @Override
    public void check() {
        log.info("Checking for token transfer transaction status");

        String transactionId = Utils.hederaMirrorTransactionId(auction.getTransfertxid());

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
}
