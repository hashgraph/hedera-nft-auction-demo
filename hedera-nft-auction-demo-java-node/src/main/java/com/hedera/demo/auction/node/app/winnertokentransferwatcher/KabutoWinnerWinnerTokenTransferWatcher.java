package com.hedera.demo.auction.node.app.winnertokentransferwatcher;

import com.hedera.demo.auction.node.app.domain.Auction;
import com.hedera.demo.auction.node.app.repository.AuctionsRepository;
import io.vertx.ext.web.client.WebClient;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class KabutoWinnerWinnerTokenTransferWatcher extends AbstractWinnerTokenTransferWatcher implements WinnerTokenTransferWatcherInterface {

    public KabutoWinnerWinnerTokenTransferWatcher(WebClient webClient, AuctionsRepository auctionsRepository, Auction auction) throws Exception {
        super(webClient, auctionsRepository, auction);
    }

    @Override
    public void check() {
        //TODO:
        log.debug("Kabuto watch not implemented");
    }
}
