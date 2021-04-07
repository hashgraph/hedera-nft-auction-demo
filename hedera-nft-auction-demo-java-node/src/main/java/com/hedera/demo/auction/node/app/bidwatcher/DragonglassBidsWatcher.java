package com.hedera.demo.auction.node.app.bidwatcher;

import com.hedera.demo.auction.node.app.HederaClient;
import com.hedera.demo.auction.node.app.repository.AuctionsRepository;
import com.hedera.demo.auction.node.app.repository.BidsRepository;
import io.vertx.ext.web.client.WebClient;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class DragonglassBidsWatcher extends AbstractBidsWatcher implements BidsWatcherInterface {

    public DragonglassBidsWatcher(HederaClient hederaClient, WebClient webClient, AuctionsRepository auctionsRepository, BidsRepository bidsRepository, int auctionId, String refundKey, int mirrorQueryFrequency) throws Exception {
        super(hederaClient, webClient, auctionsRepository, bidsRepository, auctionId, refundKey, mirrorQueryFrequency);
    }

    @Override
    public void watch() {
        //TODO:
        log.debug("Dragonglass watch not implemented");
        log.debug("Checking for bids on account " + auction.getAuctionaccountid() + " and token " + auction.getTokenid());
    }
}
