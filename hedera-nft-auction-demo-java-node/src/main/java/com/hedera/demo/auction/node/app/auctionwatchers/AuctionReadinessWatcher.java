package com.hedera.demo.auction.node.app.auctionwatchers;

import com.hedera.demo.auction.node.app.domain.Auction;
import com.hedera.demo.auction.node.app.repository.AuctionsRepository;
import com.hedera.demo.auction.node.app.repository.BidsRepository;
import io.github.cdimascio.dotenv.Dotenv;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.codec.BodyCodec;
import kotlin.Pair;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@Log4j2
public class AuctionReadinessWatcher implements Runnable {

    private final Auction auction;
    private final WebClient webClient;
    private final String mirrorToUse;
    private final AuctionsRepository auctionsRepository;
    private final BidsRepository bidsRepository;
    private final Dotenv env;
    private final int mirrorQueryFrequency;

    public AuctionReadinessWatcher(WebClient webClient, AuctionsRepository auctionsRepository, BidsRepository bidsRepository, Auction auction, Dotenv env) {
        this.webClient = webClient;
        this.auctionsRepository = auctionsRepository;
        this.bidsRepository = bidsRepository;
        this.auction = auction;
        this.env = env;
        this.mirrorToUse = Optional.ofNullable(env.get("MIRROR_NODE")).orElse("");
        this.mirrorQueryFrequency = Integer.parseInt(Optional.ofNullable(env.get("MIRROR_QUERY_FREQUENCY")).orElse("5000"));
    }

    /**
     * check transaction history for token, if associated update auction status
     * start new bidding monitor thread
     * and close this thread
     */
    @SneakyThrows
    @Override
    public void run() {
        AtomicBoolean querying = new AtomicBoolean(false);
        AtomicBoolean done = new AtomicBoolean(false);

        log.info("Watching auction account Id " + auction.getAuctionaccountid() + ", token Id " + auction.getTokenid());

        AtomicReference<String> uri = new AtomicReference<>("");
        if (mirrorToUse.toUpperCase().contains("HEDERA")) {
            uri.set("/api/v1/transactions");
        } else if (mirrorToUse.toUpperCase().contains("KABUTO")) {
            //TODO: Handle kabuto mirror
        } else if (mirrorToUse.toUpperCase().contains("DRAGONGLASS")) {
            //TODO: Handle dragonglass mirror
        }

        while (! done.get()) {
            if (!querying.get()) {
                querying.set(true);

                log.trace("Checking association for account " + auction.getAuctionaccountid() + " and token " + auction.getTokenid());
                String operatorId = env.get("OPERATOR_ID");
                if (mirrorToUse.toUpperCase().contains("HEDERA")) {
                    webClient
                            .get(443, mirrorToUse, uri.get())
                            .ssl(true)
                            //TODO: fix this once mirror bug fixed
//                            .addQueryParam("account.id", auction.getAuctionaccountid())
                            .addQueryParam("account.id", operatorId)
                            .addQueryParam("transactiontype", "CRYPTOTRANSFER")
                            .addQueryParam("order", "desc")
                            .as(BodyCodec.jsonObject())
                            .send()
                            .onSuccess(response -> {
                                JsonObject body = response.body();
                                try {
                                    Pair<Boolean, String> checkAssociation = HandleHederaResponse(body);
                                    if (checkAssociation.getFirst()) {
                                        // token is associated
                                        log.info("Account " + auction.getAuctionaccountid() + " and token " + auction.getTokenid() + " associated, starting auction");
                                        auctionsRepository.setActive(auction);
                                        // start the thread to monitor bids
                                        Thread t = new Thread(new BidsWatcher(webClient, auctionsRepository, bidsRepository, auction, env));
                                        t.start();
                                        done.set(true);
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
                            })
                            .onFailure(e -> {
                                log.error(e);
                                querying.set(false);
                            });
                } else if (mirrorToUse.toUpperCase().contains("KABUTO")) {
                    //TODO: Handle kabuto mirror
                } else if (mirrorToUse.toUpperCase().contains("DRAGONGLASS")) {
                    //TODO: Handle dragonglass mirror
                }
            }
            Thread.sleep(this.mirrorQueryFrequency);
        }
    }

    private Pair<Boolean, String> HandleHederaResponse(JsonObject response) {
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
                                    return new Pair(true, null);
                                }
                            }
                        }
                    }
                }
            }

            JsonObject links = response.getJsonObject("links");
            return new Pair(false, links.getString("next"));
        } catch (Exception e) {
            log.error(e);
            return new Pair(false, null);
        }
    }
}
