package com.hedera.demo.auction.node.app.winnertokentransferwatcher;

import com.hedera.demo.auction.node.app.HederaClient;
import com.hedera.demo.auction.node.app.domain.Auction;
import com.hedera.demo.auction.node.app.repository.AuctionsRepository;
import io.vertx.ext.web.client.WebClient;

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
}
