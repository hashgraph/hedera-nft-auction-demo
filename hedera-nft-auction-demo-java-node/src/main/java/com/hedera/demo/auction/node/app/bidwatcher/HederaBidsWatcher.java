package com.hedera.demo.auction.node.app.bidwatcher;

import com.hedera.demo.auction.node.app.domain.Auction;
import com.hedera.demo.auction.node.app.repository.AuctionsRepository;
import com.hedera.demo.auction.node.app.repository.BidsRepository;
import com.hedera.demo.auction.node.mirrorentities.MirrorTransaction;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.codec.BodyCodec;
import lombok.extern.log4j.Log4j2;

import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicBoolean;

@Log4j2
public class HederaBidsWatcher extends AbstractBidsWatcher implements BidsWatcherInterface {

    public HederaBidsWatcher(WebClient webClient, AuctionsRepository auctionsRepository, BidsRepository bidsRepository, Auction auction, String refundKey, int mirrorQueryFrequency) throws Exception {
        super(webClient, auctionsRepository, bidsRepository, auction, refundKey, mirrorQueryFrequency);
    }

    @Override
    public void watch() {

        AtomicBoolean querying = new AtomicBoolean(false);

        var webQuery = webClient
                .get(mirrorURL, "/api/v1/transactions")
                .addQueryParam("account.id", auction.getAuctionaccountid())
                .addQueryParam("transactiontype", "CRYPTOTRANSFER")
                .addQueryParam("order", "asc")
                .addQueryParam("timestamp","gt:0");

        while (true) { if (!querying.get()) {
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
                        } catch (RuntimeException | SQLException e) {
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

    private void handleResponse(JsonObject response) throws SQLException {
        try {
            JsonArray transactions = response.getJsonArray("transactions");
            for (Object transactionObject : transactions) {
                JsonObject transaction = JsonObject.mapFrom(transactionObject);
                MirrorTransaction mirrorTransaction = new MirrorTransaction(transaction, auction.getAuctionaccountid());
                handleTransaction(mirrorTransaction);
                this.auction.setLastconsensustimestamp(mirrorTransaction.consensusTimestamp);
                auctionsRepository.save(this.auction);
            }
        } catch (Exception e) {
            log.error(e);
        }
    }

//
//    private long transactionBidAmount(JsonObject transaction) {
//        @Var long bidAmount = 0;
//        // find payment amount
//        JsonArray transfers = transaction.getJsonArray("transfers");
//        // get the bid value which is the payment amount to the auction account
//        for (Object transferObject : transfers) {
//            JsonObject transfer = JsonObject.mapFrom(transferObject);
//            if (transfer.getString("account").equals(this.auction.getAuctionaccountid())) {
//                bidAmount = transfer.getLong("amount");
//                log.debug("Bid amount is " + bidAmount);
//                break;
//            }
//        }
//        return bidAmount;
//    }
}
