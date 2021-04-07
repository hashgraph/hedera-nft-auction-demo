package com.hedera.demo.auction.node.app.winnertokentransferwatcher;

import com.hedera.demo.auction.node.app.HederaClient;
import com.hedera.demo.auction.node.app.domain.Auction;
import com.hedera.demo.auction.node.app.repository.AuctionsRepository;
import com.hedera.demo.auction.node.app.mirrormapping.MirrorTransaction;
import com.hedera.demo.auction.node.app.mirrormapping.MirrorTransactions;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.codec.binary.Hex;

import java.sql.SQLException;
import java.util.Base64;

@Log4j2
public abstract class AbstractWinnerTokenTransferWatcher {

    protected final WebClient webClient;
    protected final AuctionsRepository auctionsRepository;
    protected final Auction auction;
    protected String mirrorURL;

    protected AbstractWinnerTokenTransferWatcher(WebClient webClient, AuctionsRepository auctionsRepository, Auction auction) throws Exception {
        this.webClient = webClient;
        this.auctionsRepository = auctionsRepository;
        this.auction = auction;
        this.mirrorURL = HederaClient.getMirrorUrl();
    }

    protected void handleResponse(JsonObject response, Auction auction) {
        try {
            MirrorTransactions mirrorTransactions = response.mapTo(MirrorTransactions.class);
            if (mirrorTransactions.transactions.size() > 0) {
                for (MirrorTransaction transaction : mirrorTransactions.transactions) {
                    if (transaction.isSuccessful()) {
                        // token transfer was successful
                        log.debug("Found successful token transfer transaction");
                        byte[] txHashBytes = Base64.getDecoder().decode(transaction.getTransactionHash());
                        String hash = Hex.encodeHexString(txHashBytes);
                        auctionsRepository.setEnded(auction.getId(), hash);
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
