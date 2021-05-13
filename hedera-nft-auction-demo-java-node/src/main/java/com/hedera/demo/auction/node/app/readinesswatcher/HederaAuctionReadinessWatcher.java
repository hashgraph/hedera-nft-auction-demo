package com.hedera.demo.auction.node.app.readinesswatcher;

import com.hedera.demo.auction.node.app.HederaClient;
import com.hedera.demo.auction.node.app.Utils;
import com.hedera.demo.auction.node.app.domain.Auction;
import com.hedera.demo.auction.node.app.repository.AuctionsRepository;
import com.hedera.demo.auction.node.app.repository.BidsRepository;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.codec.BodyCodec;
import kotlin.Pair;
import lombok.extern.log4j.Log4j2;
import org.jooq.tools.StringUtils;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@Log4j2
public class HederaAuctionReadinessWatcher extends AbstractAuctionReadinessWatcher implements AuctionReadinessWatcherInterface {

    public HederaAuctionReadinessWatcher(HederaClient hederaClient, WebClient webClient, AuctionsRepository auctionsRepository, BidsRepository bidsRepository, Auction auction, String refundKey, int mirrorQueryFrequency) {
        super(hederaClient, webClient, auctionsRepository, bidsRepository, auction, refundKey, mirrorQueryFrequency);
    }

    /**
     * check transaction history for token, if associated update auction status
     * start new bidding monitor thread
     * and close this thread
     *
     * Note: Considered simply checking balances for the account, but this doesn't give us
     * a common consensus timestamp to indicate the start of the auction
     */
    @Override
    public void watch() {
        AtomicBoolean querying = new AtomicBoolean(false);

        log.info("Watching auction account Id " + auction.getAuctionaccountid() + ", token Id " + auction.getTokenid());

        AtomicReference<String> uri = new AtomicReference<>("");
        uri.set("/api/v1/transactions");

        while (runThread) {
            if (!querying.get()) {
                querying.set(true);

                var webQuery = webClient
                        .get(this.mirrorPort, mirrorURL, uri.get())
                        .as(BodyCodec.jsonObject())
                        .addQueryParam("account.id", this.auction.getAuctionaccountid())
                        .addQueryParam("transactiontype", "CRYPTOTRANSFER")
                        .addQueryParam("order", "asc");

                log.debug("Checking ownership of token " + auction.getTokenid() + " for account " + auction.getAuctionaccountid());
                webQuery.send(response -> {
                    if (response.succeeded()) {
                        JsonObject body = response.result().body();
                        Pair<Boolean, String> checkAssociation = handleResponse(body);
                        if (checkAssociation.getFirst()) {
                            // token is associated, exit this thread
                            return;
                        } else {
                            if (testing) {
                                return;
                            }
                            if (!StringUtils.isEmpty(checkAssociation.getSecond())) {
                                uri.set(checkAssociation.getSecond());
                            }
                        }
                    } else {
                        log.error(response.cause().getMessage());
                        return;
                    }
                    querying.set(testing);
                });
            }
            if (testing) {
                return;
            } else {
                Utils.sleep(this.mirrorQueryFrequency);
            }
        }
    }
}
