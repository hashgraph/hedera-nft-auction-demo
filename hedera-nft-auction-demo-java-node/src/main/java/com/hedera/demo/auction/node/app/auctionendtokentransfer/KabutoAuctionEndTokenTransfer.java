package com.hedera.demo.auction.node.app.auctionendtokentransfer;

import com.hedera.demo.auction.node.app.HederaClient;
import com.hedera.demo.auction.node.app.repository.AuctionsRepository;
import io.vertx.ext.web.client.WebClient;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class KabutoAuctionEndTokenTransfer extends AbstractAuctionEndTokenTransfer implements AuctionEndTokenTransferInterface {

    public KabutoAuctionEndTokenTransfer(HederaClient hederaClient, WebClient webClient, AuctionsRepository auctionsRepository, String tokenId, String winningAccountId) {
        super(hederaClient, webClient, auctionsRepository, tokenId, winningAccountId);
    }

    @Override
    public void checkAssociation() {
        //TODO:
        log.debug("Kabuto watch not implemented");
    }
}
