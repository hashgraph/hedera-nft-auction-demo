package com.hedera.demo.auction.node.app.closurewatcher;

import com.hedera.demo.auction.node.app.repository.AuctionsRepository;
import io.vertx.ext.web.client.WebClient;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class KabutoAuctionsClosureWatcher extends AbstractAuctionsClosureWatcher implements AuctionClosureWatcherInterface {

    public KabutoAuctionsClosureWatcher(WebClient webClient, AuctionsRepository auctionsRepository, int mirrorQueryFrequency, boolean transferOnWin) throws Exception {
        super(webClient, auctionsRepository, mirrorQueryFrequency, transferOnWin);
    }

    @Override
    public void watch() {
        //TODO:
        log.debug("Kabuto watch not implemented");
    }
}