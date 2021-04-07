package com.hedera.demo.auction.node.app.winnertokentransfer;

import com.hedera.demo.auction.node.app.HederaClient;
import com.hedera.demo.auction.node.app.repository.AuctionsRepository;
import io.vertx.ext.web.client.WebClient;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class DragonglassWinnerTokenTransfer extends AbstractWinnerTokenTransfer implements WinnerTokenTransferInterface {

    public DragonglassWinnerTokenTransfer(HederaClient hederaClient, WebClient webClient, AuctionsRepository auctionsRepository, String tokenId, String winningAccountId) {
        super(hederaClient, webClient, auctionsRepository, tokenId, winningAccountId);
    }

    @Override
    public void checkAssociation() {
        //TODO:
        log.debug("Dragonglass watch not implemented");
//        log.info("Checking association between  auction account Id " + auction.getAuctionaccountid() + ", token Id " + auction.getTokenid());
    }
}
