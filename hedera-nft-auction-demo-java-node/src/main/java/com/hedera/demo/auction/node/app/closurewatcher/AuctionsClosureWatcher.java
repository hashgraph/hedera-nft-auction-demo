package com.hedera.demo.auction.node.app.closurewatcher;

import com.hedera.demo.auction.node.app.HederaClient;
import com.hedera.demo.auction.node.app.repository.AuctionsRepository;
import io.vertx.ext.web.client.WebClient;
import lombok.SneakyThrows;

public class AuctionsClosureWatcher implements Runnable {

    private final WebClient webClient;
    private final AuctionsRepository auctionsRepository;
    private final int mirrorQueryFrequency;
    private final String mirrorProvider = HederaClient.getMirrorProvider();
    private final boolean transferOnWin;
    private final String refundKey;

    public AuctionsClosureWatcher(WebClient webClient, AuctionsRepository auctionsRepository, int mirrorQueryFrequency, boolean transferOnWin, String refundKey) {
        this.webClient = webClient;
        this.auctionsRepository = auctionsRepository;
        this.mirrorQueryFrequency = mirrorQueryFrequency;
        this.transferOnWin = transferOnWin;
        this.refundKey = refundKey;
    }

    @SneakyThrows
    @Override
    public void run() {
        AuctionClosureWatcherInterface auctionClosureWatcher;
        switch (mirrorProvider) {
            case "HEDERA":
                auctionClosureWatcher = new HederaAuctionsClosureWatcher(webClient, auctionsRepository, mirrorQueryFrequency, transferOnWin, refundKey);
                break;
            case "DRAGONGLASS":
                auctionClosureWatcher = new DragonglassAuctionsClosureWatcher(webClient, auctionsRepository, mirrorQueryFrequency, transferOnWin, refundKey);
                break;
            default:
                auctionClosureWatcher = new KabutoAuctionsClosureWatcher(webClient, auctionsRepository, mirrorQueryFrequency, transferOnWin, refundKey);
                break;
        }
        auctionClosureWatcher.watch();
    }
}
