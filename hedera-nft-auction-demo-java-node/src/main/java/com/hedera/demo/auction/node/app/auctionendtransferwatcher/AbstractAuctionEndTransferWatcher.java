package com.hedera.demo.auction.node.app.auctionendtransferwatcher;

import com.hedera.demo.auction.node.app.HederaClient;
import com.hedera.demo.auction.node.app.domain.Auction;
import com.hedera.demo.auction.node.app.mirrormapping.MirrorTransaction;
import com.hedera.demo.auction.node.app.mirrormapping.MirrorTransactions;
import com.hedera.demo.auction.node.app.repository.AuctionsRepository;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import lombok.extern.log4j.Log4j2;

import java.sql.SQLException;

@Log4j2
public abstract class AbstractAuctionEndTransferWatcher {

    protected final WebClient webClient;
    protected final AuctionsRepository auctionsRepository;
    protected final Auction auction;
    protected String mirrorURL;

    protected AbstractAuctionEndTransferWatcher(HederaClient hederaClient, WebClient webClient, AuctionsRepository auctionsRepository, Auction auction) {
        this.webClient = webClient;
        this.auctionsRepository = auctionsRepository;
        this.auction = auction;
        this.mirrorURL = hederaClient.mirrorUrl();
    }

    public void handleResponse(JsonObject response, Auction auction) {
        try {
            MirrorTransactions mirrorTransactions = response.mapTo(MirrorTransactions.class);
            if (mirrorTransactions.transactions.size() > 0) {
                for (MirrorTransaction transaction : mirrorTransactions.transactions) {
                    if (transaction.isSuccessful()) {
                        // token transfer was successful
                        log.debug("Found successful token transfer transaction");
                        auctionsRepository.setTransferTransactionByAuctionId(auction.getId(), transaction.transactionId, transaction.getTransactionHashString());
                    } else {
                        log.debug("Token transfer transaction id " + auction.getTransfertxid() + " failed: " + transaction.result);
                    }
                }
            } else {
                log.debug("No " + auction.getTransfertxid() + " transaction found");
            }
        } catch (RuntimeException | SQLException e) {
            log.error(e);
        }
    }
}
