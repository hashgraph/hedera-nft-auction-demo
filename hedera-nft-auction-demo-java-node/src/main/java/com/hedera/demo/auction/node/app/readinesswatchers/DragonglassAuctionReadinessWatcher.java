package com.hedera.demo.auction.node.app.readinesswatchers;

import com.hedera.demo.auction.node.app.domain.Auction;
import com.hedera.demo.auction.node.app.repository.AuctionsRepository;
import com.hedera.demo.auction.node.app.repository.BidsRepository;
import io.vertx.ext.web.client.WebClient;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class DragonglassAuctionReadinessWatcher extends AuctionReadinessWatcher {

    public DragonglassAuctionReadinessWatcher(WebClient webClient, AuctionsRepository auctionsRepository, BidsRepository bidsRepository, Auction auction, String refundKey, int mirrorQueryFrequency) throws Exception {
        super(webClient, auctionsRepository, bidsRepository, auction, refundKey, mirrorQueryFrequency);
    }

    /**
     * check transaction history for token, if associated update auction status
     * start new bidding monitor thread
     * and close this thread
     */
    public void watch() {
        //TODO:
        log.debug("Dragonglass watch not implemented");
        log.info("Watching auction account Id " + auction.getAuctionaccountid() + ", token Id " + auction.getTokenid());
    }
}
