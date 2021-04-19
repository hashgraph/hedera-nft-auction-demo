package com.hedera.demo.auction.node.app.bidwatcher;

import com.hedera.demo.auction.node.app.HederaClient;
import com.hedera.demo.auction.node.app.repository.AuctionsRepository;
import com.hedera.demo.auction.node.app.repository.BidsRepository;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.codec.BodyCodec;
import lombok.extern.log4j.Log4j2;

import java.util.concurrent.atomic.AtomicBoolean;

@Log4j2
public class HederaBidsWatcher extends AbstractBidsWatcher implements BidsWatcherInterface {

    public HederaBidsWatcher(HederaClient hederaClient, WebClient webClient, AuctionsRepository auctionsRepository, BidsRepository bidsRepository, int auctionId, String refundKey, int mirrorQueryFrequency) throws Exception {
        super(hederaClient, webClient, auctionsRepository, bidsRepository, auctionId, refundKey, mirrorQueryFrequency);
    }

    @Override
    public void watch() throws Exception {

        AtomicBoolean querying = new AtomicBoolean(false);

        log.info("Watching auction account Id " + auction.getAuctionaccountid() + ", token Id " + auction.getTokenid());

        var webQuery = webClient
                .get(mirrorURL, "/api/v1/transactions")
                .addQueryParam("account.id", auction.getAuctionaccountid())
                .addQueryParam("transactiontype", "CRYPTOTRANSFER")
                .addQueryParam("order", "asc")
                .addQueryParam("timestamp","gt:0");

        while (runThread) {
            if (!querying.get()) {
                querying.set(true);

                log.debug("Checking for bids on account " + auction.getAuctionaccountid() + " and token " + auction.getTokenid());

                if (auction.getLastconsensustimestamp() != null) {
                    webQuery.setQueryParam("timestamp", "gt:".concat(auction.getLastconsensustimestamp()));
                }

                webQuery.as(BodyCodec.jsonObject())
                .send(response -> {
                    if (response.succeeded()) {
                        JsonObject body = response.result().body();
                        try {
                            handleResponse(body);
                        } catch (RuntimeException e) {
                            log.error(e);
                        } finally {
                            querying.set(false);
                        }
                    } else {
                        log.error(response.cause().getMessage());
                        querying.set(false);
                    }
                });
            }
            try {
                Thread.sleep(this.mirrorQueryFrequency);
            } catch (InterruptedException e) {
                e.printStackTrace();
                log.error(e);
            }
            // reload auction from database
            this.auction = auctionsRepository.getAuction(auctionId);
        }
    }
}
