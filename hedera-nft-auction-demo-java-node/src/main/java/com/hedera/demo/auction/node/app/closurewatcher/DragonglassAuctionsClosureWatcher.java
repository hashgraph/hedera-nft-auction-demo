package com.hedera.demo.auction.node.app.closurewatcher;

import com.hedera.demo.auction.node.app.repository.AuctionsRepository;
import io.vertx.ext.web.client.WebClient;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class DragonglassAuctionsClosureWatcher extends AbstractAuctionsClosureWatcher implements AuctionClosureWatcherInterface {

    public DragonglassAuctionsClosureWatcher(WebClient webClient, AuctionsRepository auctionsRepository, int mirrorQueryFrequency, boolean transferOnWin, String refundKey) throws Exception {
        super(webClient, auctionsRepository, mirrorQueryFrequency, transferOnWin, refundKey);
    }

    @Override
    public void watch() {
        //TODO:
        log.debug("Dragonglass watch not implemented");
    }
}
