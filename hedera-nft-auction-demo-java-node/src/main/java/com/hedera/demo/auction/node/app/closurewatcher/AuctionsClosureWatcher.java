package com.hedera.demo.auction.node.app.closurewatcher;

import com.hedera.demo.auction.node.app.HederaClient;
import com.hedera.demo.auction.node.app.repository.AuctionsRepository;
import io.vertx.ext.web.client.WebClient;
import lombok.SneakyThrows;

public class AuctionsClosureWatcher implements Runnable {

    private final WebClient webClient;
    private final AuctionsRepository auctionsRepository;
    private final int mirrorQueryFrequency;
    private final String mirrorProvider;
    private final boolean transferOnWin;
    private final String refundKey;
    private final HederaClient hederaClient;
    private AuctionClosureWatcherInterface auctionClosureWatcher = null;

    public AuctionsClosureWatcher(HederaClient hederaClient, WebClient webClient, AuctionsRepository auctionsRepository, int mirrorQueryFrequency, boolean transferOnWin, String refundKey) {
        this.webClient = webClient;
        this.auctionsRepository = auctionsRepository;
        this.mirrorQueryFrequency = mirrorQueryFrequency;
        this.transferOnWin = transferOnWin;
        this.refundKey = refundKey;
        this.hederaClient = hederaClient;
        this.mirrorProvider = hederaClient.mirrorProvider();
    }

    @SneakyThrows
    @Override
    public void run() {

        switch (mirrorProvider) {
            case "HEDERA":
                auctionClosureWatcher = new HederaAuctionsClosureWatcher(hederaClient, webClient, auctionsRepository, mirrorQueryFrequency, transferOnWin, refundKey);
                break;
            default:
                throw new Exception("Support for non Hedera mirrors not implemented.");
        }
        auctionClosureWatcher.watch();
    }

    public void stop() {
        if (auctionClosureWatcher != null) {
            auctionClosureWatcher.stop();
        }
    }
}
