package com.hedera.demo.auction.node.app.closurewatcher;

import com.hedera.demo.auction.node.app.HederaClient;
import com.hedera.demo.auction.node.app.repository.AuctionsRepository;
import com.hedera.demo.auction.node.mirrormapping.MirrorTransactions;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import lombok.extern.log4j.Log4j2;

import java.sql.SQLException;
import java.util.Map;

@Log4j2
public abstract class AbstractAuctionsClosureWatcher {

    protected final WebClient webClient;
    protected final AuctionsRepository auctionsRepository;
    protected final int mirrorQueryFrequency;
    protected String mirrorURL;
    protected boolean transferOnWin;

    protected AbstractAuctionsClosureWatcher(WebClient webClient, AuctionsRepository auctionsRepository, int mirrorQueryFrequency, boolean transferOnWin) throws Exception {
        this.webClient = webClient;
        this.auctionsRepository = auctionsRepository;
        this.mirrorQueryFrequency = mirrorQueryFrequency;
        this.mirrorURL = HederaClient.getMirrorUrl();
        this.transferOnWin = transferOnWin;
    }

    void handleResponse(JsonObject response) {
        if (response != null) {
            MirrorTransactions mirrorTransactions = response.mapTo(MirrorTransactions.class);

            if (mirrorTransactions.transactions != null) {
                if (mirrorTransactions.transactions.size() > 0) {
                    closeAuctionIfPastEnd(mirrorTransactions.transactions.get(0).getConsensusTimestamp());
                }
            }
        }
    }

    protected void closeAuctionIfPastEnd(String consensusTimestamp) {
        for (Map.Entry<String, Integer> auctions : auctionsRepository.openAndPendingAuctions().entrySet()) {
            String endTimestamp = auctions.getKey();
            int auctionId = auctions.getValue();

            if (consensusTimestamp.compareTo(endTimestamp) > 0) {
                // payment past auctions end, close it
                log.info("Closing auction id " + auctionId);
                try {
                    if (transferOnWin) {
                        // if the auction transfers the token on winning, set the auction to closed (no more bids)
                        auctionsRepository.setClosed(auctionId);
                    } else {
                        // if the auction does not transfer the token on winning, set the auction to ended
                        auctionsRepository.setEnded(auctionId, "");
                    }
                } catch (SQLException e) {
                    log.error(e);
                }
            }
        }
    }
}
