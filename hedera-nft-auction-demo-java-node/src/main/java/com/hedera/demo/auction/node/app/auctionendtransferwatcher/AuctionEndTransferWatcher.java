package com.hedera.demo.auction.node.app.auctionendtransferwatcher;

import com.hedera.demo.auction.node.app.HederaClient;
import com.hedera.demo.auction.node.app.domain.Auction;
import com.hedera.demo.auction.node.app.repository.AuctionsRepository;
import io.vertx.ext.web.client.WebClient;
import lombok.extern.log4j.Log4j2;

import java.sql.SQLException;

@Log4j2
public class AuctionEndTransferWatcher implements Runnable {

    private final WebClient webClient;
    private final AuctionsRepository auctionsRepository;
    private final int mirrorQueryFrequency;
    private final String mirrorProvider;
    private final HederaClient hederaClient;
    private boolean runThread = true;
    private AuctionEndTransferWatcherInterface auctionEndTransferWatcherInterface = null;

    public AuctionEndTransferWatcher(HederaClient hederaClient, WebClient webClient, AuctionsRepository auctionsRepository, int mirrorQueryFrequency) {
        this.webClient = webClient;
        this.auctionsRepository = auctionsRepository;
        this.mirrorQueryFrequency = mirrorQueryFrequency;
        this.hederaClient = hederaClient;
        this.mirrorProvider = hederaClient.mirrorProvider();
    }

    @Override
    public void run() {
        while (runThread) {
            try {
                for (Auction auction : auctionsRepository.getAuctionsList()) {
                    if (auction.isTransferring()) {
                        if (!auction.getTransfertxid().isEmpty()) {
                            // find if transaction is complete and successful
                            switch (mirrorProvider) {
                                case "HEDERA":
                                    auctionEndTransferWatcherInterface = new HederaAuctionEndTransferWatcher(hederaClient, webClient, auctionsRepository, auction);
                                    break;
                                default:
                                    throw new Exception("Support for non Hedera mirrors not implemented.");
                            }
                            auctionEndTransferWatcherInterface.check();
                        }
                    }
                }
                Thread.sleep(mirrorQueryFrequency);
            } catch (InterruptedException e) {
                log.error(e);
            } catch (RuntimeException e) {
                log.error(e);
            } catch (SQLException e) {
                log.error(e);
            } catch (Exception e) {
                log.error(e);
            }
        }
    }

    public void stop() {
        runThread = false;
    }
}
