package com.hedera.demo.auction.node.app.readinesswatchers;

import com.hedera.demo.auction.node.app.bidwatchers.BidsWatcher;
import com.hedera.demo.auction.node.app.domain.Auction;
import com.hedera.demo.auction.node.app.repository.AuctionsRepository;
import com.hedera.demo.auction.node.app.repository.BidsRepository;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.codec.BodyCodec;
import kotlin.Pair;
import lombok.extern.log4j.Log4j2;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@Log4j2
public class HederaAuctionReadinessWatcher extends AuctionReadinessWatcher {

    public HederaAuctionReadinessWatcher(WebClient webClient, AuctionsRepository auctionsRepository, BidsRepository bidsRepository, Auction auction, String refundKey, int mirrorQueryFrequency) throws Exception {
        super(webClient, auctionsRepository, bidsRepository, auction, refundKey, mirrorQueryFrequency);
    }

    /**
     * check transaction history for token, if associated update auction status
     * start new bidding monitor thread
     * and close this thread
     */
    public void watch() {
        AtomicBoolean querying = new AtomicBoolean(false);
        AtomicBoolean done = new AtomicBoolean(false);

        log.info("Watching auction account Id " + auction.getAuctionaccountid() + ", token Id " + auction.getTokenid());

        AtomicReference<String> uri = new AtomicReference<>("");
        uri.set("/api/v1/transactions");

        while (! done.get()) {
            if (!querying.get()) {
                querying.set(true);

                var webQuery  = webClient
                    .get(443, mirrorURL, uri.get())
                    .ssl(true)
                    .as(BodyCodec.jsonObject())
                    //TODO: fix this once mirror bug fixed
//                            .addQueryParam("account.id", auction.getAuctionaccountid())
                    .addQueryParam("account.id", this.auction.getAuctionaccountid())
                    .addQueryParam("transactiontype", "CRYPTOTRANSFER")
                    .addQueryParam("order", "asc");

                log.debug("Checking association for account " + auction.getAuctionaccountid() + " and token " + auction.getTokenid());
                webQuery.send(response -> {
                    if (response.succeeded()) {
                        JsonObject body = response.result().body();
                        try {
                            Pair<Boolean, String> checkAssociation = handleResponse(body);
                            if (checkAssociation.getFirst()) {
                                // token is associated
                                log.info("Account " + auction.getAuctionaccountid() + " and token " + auction.getTokenid() + " associated, starting auction");
                                auctionsRepository.setActive(auction, checkAssociation.getSecond());
                                // start the thread to monitor bids
                                Thread t = new Thread(new BidsWatcher(webClient, auctionsRepository, bidsRepository, auction, refundKey, mirrorQueryFrequency));
                                t.start();
                                done.set(true);
                                return;
                            } else {
                                if (checkAssociation.getSecond() != null) {
                                    uri.set(checkAssociation.getSecond());
                                }
                            }
                        } catch (Exception e) {
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
        }
    }

    private Pair<Boolean, String> handleResponse(JsonObject response) {
        try {
            JsonArray transactions = response.getJsonArray("transactions");
            for (Object transactionObject : transactions) {
                JsonObject transaction = JsonObject.mapFrom(transactionObject);

                JsonArray transfers = transaction.getJsonArray("token_transfers");
                // get the bid value which is the payment amount to the auction account
                if (transfers != null) {
                    for (Object transferObject : transfers) {
                        JsonObject transfer = JsonObject.mapFrom(transferObject);
                        if (transfer.getString("account").equals(this.auction.getAuctionaccountid())) {
                            if (transfer.getString("token_id").equals(this.auction.getTokenid())) {
                                if (transfer.getLong("amount") != 0) {
                                    // token is associated with account
                                    return new Pair<Boolean, String>(true, transaction.getString("consensus_timestamp"));
                                }
                            }
                        }
                    }
                }
            }

            JsonObject links = response.getJsonObject("links");
            return new Pair<Boolean, String>(false, links.getString("next"));
        } catch (RuntimeException e) {
            log.error(e);
            return new Pair<Boolean, String>(false, null);
        }
    }
}
