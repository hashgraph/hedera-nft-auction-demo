package com.hedera.demo.auction.node.app.auctionendtransfer;

import com.hedera.demo.auction.node.app.HederaClient;
import com.hedera.demo.auction.node.app.repository.AuctionsRepository;
import io.vertx.ext.web.client.WebClient;

public abstract class AbstractAuctionEndTransfer {

    protected final WebClient webClient;
    protected final AuctionsRepository auctionsRepository;
    protected final String tokenId;
    protected final String winningAccountId;
    protected final String mirrorURL;
    protected final HederaClient hederaClient;

    protected AbstractAuctionEndTransfer(HederaClient hederaClient, WebClient webClient, AuctionsRepository auctionsRepository, String tokenId, String winningAccountId) {
        this.webClient = webClient;
        this.auctionsRepository = auctionsRepository;
        this.tokenId = tokenId;
        this.winningAccountId = winningAccountId;
        this.hederaClient = hederaClient;
        this.mirrorURL = hederaClient.mirrorUrl();
    }
}