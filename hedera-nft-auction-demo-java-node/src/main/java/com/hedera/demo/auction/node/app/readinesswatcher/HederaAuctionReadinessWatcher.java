package com.hedera.demo.auction.node.app.readinesswatcher;

import com.hedera.demo.auction.node.app.bidwatcher.BidsWatcher;
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
public class HederaAuctionReadinessWatcher extends AbstractAuctionReadinessWatcher implements AuctionReadinessWatcherInterface {

    public HederaAuctionReadinessWatcher(WebClient webClient, AuctionsRepository auctionsRepository, BidsRepository bidsRepository, Auction auction, String refundKey, int mirrorQueryFrequency) throws Exception {
        super(webClient, auctionsRepository, bidsRepository, auction, refundKey, mirrorQueryFrequency);
    }

    /**
     * check transaction history for token, if associated update auction status
     * start new bidding monitor thread
     * and close this thread
     */
    @Override
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
                        .get(mirrorURL, uri.get())
                        .as(BodyCodec.jsonObject())
                        .addQueryParam("account.id", this.auction.getAuctionaccountid())
                        .addQueryParam("transactiontype", "CRYPTOTRANSFER")
                        .addQueryParam("order", "asc");

                log.debug("Checking ownership of token " + auction.getTokenid() + " for account " + auction.getAuctionaccountid());
                webQuery.send(response -> {
                    if (response.succeeded()) {
                        JsonObject body = response.result().body();
                        try {
                            Pair<Boolean, String> checkAssociation = handleResponse(body);
                            if (checkAssociation.getFirst()) {
                                // token is associated
                                log.info("Account " + auction.getAuctionaccountid() + " owns token " + auction.getTokenid() + ", starting auction");
                                auctionsRepository.setActive(auction, checkAssociation.getSecond());
                                // start the thread to monitor bids
                                Thread t = new Thread(new BidsWatcher(webClient, auctionsRepository, bidsRepository, auction.getId(), refundKey, mirrorQueryFrequency));
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
                if (transfers != null) {
                    for (Object transferObject : transfers) {
                        JsonObject transfer = JsonObject.mapFrom(transferObject);
                        String account = transfer.getString("account");
                        String tokenId = transfer.getString("token_id");
                        long amount = transfer.getLong("amount");
                        String consensusTimestamp = transaction.getString("consensus_timestamp");
                        if (checkAssociation(account, tokenId, amount)) {
                            return new Pair<Boolean, String>(true, consensusTimestamp);
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
