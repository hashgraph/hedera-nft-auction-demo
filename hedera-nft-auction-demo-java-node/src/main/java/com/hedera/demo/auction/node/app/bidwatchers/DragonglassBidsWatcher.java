package com.hedera.demo.auction.node.app.bidwatchers;

import com.hedera.demo.auction.node.app.domain.Auction;
import com.hedera.demo.auction.node.app.repository.AuctionsRepository;
import com.hedera.demo.auction.node.app.repository.BidsRepository;
import io.vertx.ext.web.client.WebClient;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class DragonglassBidsWatcher extends AbstractBidsWatcher implements BidsWatcherInterface {

    public DragonglassBidsWatcher(WebClient webClient, AuctionsRepository auctionsRepository, BidsRepository bidsRepository, Auction auction, String refundKey, int mirrorQueryFrequency) throws Exception {
        super(webClient, auctionsRepository, bidsRepository, auction, refundKey, mirrorQueryFrequency);
    }

    @Override
    public void watch() {
        //TODO:
        log.debug("Dragonglass watch not implemented");
        log.debug("Checking for bids on account " + auction.getAuctionaccountid() + " and token " + auction.getTokenid());
    }
}
