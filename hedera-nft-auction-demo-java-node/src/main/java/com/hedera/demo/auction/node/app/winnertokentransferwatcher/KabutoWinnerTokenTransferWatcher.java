package com.hedera.demo.auction.node.app.winnertokentransferwatcher;

import com.hedera.demo.auction.node.app.HederaClient;
import com.hedera.demo.auction.node.app.domain.Auction;
import com.hedera.demo.auction.node.app.repository.AuctionsRepository;
import io.vertx.ext.web.client.WebClient;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class KabutoWinnerTokenTransferWatcher extends AbstractWinnerTokenTransferWatcher implements WinnerTokenTransferWatcherInterface {

    public KabutoWinnerTokenTransferWatcher(HederaClient hederaClient, WebClient webClient, AuctionsRepository auctionsRepository, Auction auction) {
        super(hederaClient, webClient, auctionsRepository, auction);
    }

    @Override
    public void check() {
        //TODO:
        log.debug("Kabuto watch not implemented");
    }
}