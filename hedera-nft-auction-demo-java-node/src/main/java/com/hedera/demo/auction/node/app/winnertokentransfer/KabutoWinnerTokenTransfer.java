package com.hedera.demo.auction.node.app.winnertokentransfer;

import com.hedera.demo.auction.node.app.repository.AuctionsRepository;
import io.vertx.ext.web.client.WebClient;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class KabutoWinnerTokenTransfer extends AbstractWinnerTokenTransfer implements WinnerTokenTransferInterface {

    public KabutoWinnerTokenTransfer(WebClient webClient, AuctionsRepository auctionsRepository, String tokenId, String winningAccountId) throws Exception {
        super(webClient, auctionsRepository, tokenId, winningAccountId);
    }

    @Override
    public void checkAssociation() {
        //TODO:
        log.debug("Kabuto watch not implemented");
    }
}
