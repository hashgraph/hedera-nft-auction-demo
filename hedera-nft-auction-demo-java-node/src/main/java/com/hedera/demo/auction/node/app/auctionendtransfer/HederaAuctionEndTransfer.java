package com.hedera.demo.auction.node.app.auctionendtransfer;

import com.google.errorprone.annotations.Var;
import com.hedera.demo.auction.node.app.HederaClient;
import com.hedera.demo.auction.node.app.Utils;
import com.hedera.demo.auction.node.app.domain.Auction;
import com.hedera.demo.auction.node.app.mirrormapping.MirrorTransactions;
import com.hedera.demo.auction.node.app.repository.AuctionsRepository;
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
public class HederaAuctionEndTransfer extends AbstractAuctionEndTransfer implements AuctionEndTransferInterface {

    public HederaAuctionEndTransfer(HederaClient hederaClient, WebClient webClient, AuctionsRepository auctionsRepository, String tokenId, String winningAccountId) {
        super(hederaClient, webClient, auctionsRepository, tokenId, winningAccountId);
    }

    @Override
    public TransferResult checkTransferInProgress(Auction auction) {
        String uri = "/api/v1/transactions";

        @Var TransferResult result = TransferResult.NOT_FOUND;
        @Var String nextTimestamp = auction.getEndtimestamp();
        ExecutorService executor = Executors.newFixedThreadPool(1);
        while (!StringUtils.isEmpty(nextTimestamp)) {
            Map<String, String> queryParameters = new HashMap<>();
            if (StringUtils.isEmpty(auction.getWinningaccount())) {
                queryParameters.put("account.id", auction.getTokenowneraccount());
            } else {
                queryParameters.put("account.id", auction.getWinningaccount());
            }
            queryParameters.put("transactiontype", "CRYPTOTRANSFER");
            queryParameters.put("order", "asc");
            queryParameters.put("timestamp", "gt:".concat(nextTimestamp));

            log.debug("querying mirror for successful transaction for account " + queryParameters.get("account.id"));
            Future<JsonObject> future = executor.submit(Utils.queryMirror(webClient, mirrorURL, mirrorPort, uri, queryParameters));
            try {
                JsonObject response = future.get();
                if (response != null) {
                    MirrorTransactions mirrorTransactions = response.mapTo(MirrorTransactions.class);
                    result = transferOccurredAlready(mirrorTransactions, tokenId);
                    log.info(result);
                    nextTimestamp = Utils.getTimestampFromMirrorLink(mirrorTransactions.links.next);
                }
            } catch (InterruptedException interruptedException) {
                log.error(interruptedException);
                Thread.currentThread().interrupt();
            } catch (ExecutionException executionException) {
                log.error(executionException);
            }

        }
        executor.shutdown();
        return result;
    }


}
