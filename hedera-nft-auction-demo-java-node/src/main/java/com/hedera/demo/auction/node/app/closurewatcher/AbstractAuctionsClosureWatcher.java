package com.hedera.demo.auction.node.app.closurewatcher;

import com.hedera.demo.auction.node.app.HederaClient;
import com.hedera.demo.auction.node.app.repository.AuctionsRepository;
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

    protected AbstractAuctionsClosureWatcher(WebClient webClient, AuctionsRepository auctionsRepository, int mirrorQueryFrequency) throws Exception {
        this.webClient = webClient;
        this.auctionsRepository = auctionsRepository;
        this.mirrorQueryFrequency = mirrorQueryFrequency;
        this.mirrorURL = HederaClient.getMirrorUrl();
    }

    protected void closeAuctionIfPastEnd(String consensusTimestamp) {
        for (Map.Entry<String, Integer> auctions : auctionsRepository.openAndPendingAuctions().entrySet()) {
            String endTimestamp = auctions.getKey();
            int auctionId = auctions.getValue();

            if (consensusTimestamp.compareTo(endTimestamp) > 0) {
                // payment past auctions end, close it
                log.info("Closing auction id " + auctionId);
                try {
                    auctionsRepository.setClosed(auctionId);
                } catch (SQLException e) {
                    log.error(e);
                }
            }
        }
    }
}
