package com.hedera.demo.auction.node.app.winnertokentransferwatcher;

import com.google.errorprone.annotations.Var;
import com.hedera.demo.auction.node.app.HederaClient;
import com.hedera.demo.auction.node.app.domain.Auction;
import com.hedera.demo.auction.node.app.repository.AuctionsRepository;
import io.vertx.ext.web.client.WebClient;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class WinnerTokenTransferWatcher implements Runnable {

    private final WebClient webClient;
    private final AuctionsRepository auctionsRepository;
    private final int mirrorQueryFrequency;
    private final String mirrorProvider = HederaClient.getMirrorProvider();

    public WinnerTokenTransferWatcher(WebClient webClient, AuctionsRepository auctionsRepository, int mirrorQueryFrequency) {
        this.webClient = webClient;
        this.auctionsRepository = auctionsRepository;
        this.mirrorQueryFrequency = mirrorQueryFrequency;
    }

    @Override
    public void run() {
        @Var WinnerTokenTransferWatcherInterface winnerTokenTransferWatcher;

        while (true) {
            try {
                for (Auction auction : auctionsRepository.getAuctionsList()) {
                    if (auction.isTransferring()) {
                        if (!auction.getTransfertxid().isEmpty()) {
                            // find if transaction is complete and successful
                            switch (mirrorProvider) {
                                case "HEDERA":
                                    winnerTokenTransferWatcher = new HederaWinnerTokenTransferWatcher(webClient, auctionsRepository, auction);
                                    break;
                                case "DRAGONGLASS":
                                    winnerTokenTransferWatcher = new DragonglassWinnerTokenTransferWatcher(webClient, auctionsRepository, auction);
                                    break;
                                default:
                                    winnerTokenTransferWatcher = new KabutoWinnerTokenTransferWatcher(webClient, auctionsRepository, auction);
                                    break;
                            }
                            winnerTokenTransferWatcher.check();
                        }
                    }
                }
                Thread.sleep(mirrorQueryFrequency);
            } catch (InterruptedException e) {
                log.error(e);
            } catch (Exception e) {
                log.error(e);
            }
        }
    }
}
