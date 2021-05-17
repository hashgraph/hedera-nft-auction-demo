package com.hedera.demo.auction.node.app.readinesswatcher;

import com.google.errorprone.annotations.Var;
import com.hedera.demo.auction.node.app.HederaClient;
import com.hedera.demo.auction.node.app.Utils;
import com.hedera.demo.auction.node.app.domain.Auction;
import com.hedera.demo.auction.node.app.mirrormapping.MirrorTransactions;
import com.hedera.demo.auction.node.app.repository.AuctionsRepository;
import com.hedera.demo.auction.node.app.repository.BidsRepository;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import lombok.extern.log4j.Log4j2;
import org.jooq.tools.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

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
        log.info("Watching auction account Id " + auction.getAuctionaccountid() + ", token Id " + auction.getTokenid());
        String uri = "/api/v1/transactions";

        ExecutorService executor = Executors.newFixedThreadPool(1);
        while (runThread) {
            @Var String nextTimestamp = "0.0";
            while (!StringUtils.isEmpty(nextTimestamp)) {
                log.debug("Checking ownership of token " + auction.getTokenid() + " for account " + auction.getAuctionaccountid());
                Map<String, String> queryParameters = new HashMap<>();
                queryParameters.put("account.id", auction.getAuctionaccountid());
                queryParameters.put("transactiontype", "CRYPTOTRANSFER");
                queryParameters.put("order", "desc");
                queryParameters.put("timestamp", "gt:".concat(nextTimestamp));

                Future<JsonObject> future = executor.submit(Utils.queryMirror(webClient, mirrorURL, mirrorPort, uri, queryParameters));
                try {
                    JsonObject response = future.get();
                    if (response != null) {
                        MirrorTransactions mirrorTransactions = response.mapTo(MirrorTransactions.class);

                        if (handleResponse(mirrorTransactions)) {
                            // token is owned by the auction account, exit this thread
                            runThread = false;
                            break;
                        } else {
                            if (testing) {
                                runThread = false;
                            }
                        }
                        nextTimestamp = Utils.getTimestampFromMirrorLink(mirrorTransactions.links.next);
                    }
                } catch (InterruptedException interruptedException) {
                    log.error(interruptedException);
                    Thread.currentThread().interrupt();
                } catch (ExecutionException executionException) {
                    log.error(executionException);
                }
            }

            if (testing) {
                runThread = false;
            } else {
                Utils.sleep(this.mirrorQueryFrequency);
            }
        }
        executor.shutdown();
    }
}
