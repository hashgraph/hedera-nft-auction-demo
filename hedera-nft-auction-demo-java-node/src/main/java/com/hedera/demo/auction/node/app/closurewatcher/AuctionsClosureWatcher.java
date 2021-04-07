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
        AuctionClosureWatcherInterface auctionClosureWatcher;
        switch (mirrorProvider) {
            case "HEDERA":
                auctionClosureWatcher = new HederaAuctionsClosureWatcher(hederaClient, webClient, auctionsRepository, mirrorQueryFrequency, transferOnWin, refundKey);
                break;
            case "DRAGONGLASS":
                auctionClosureWatcher = new DragonglassAuctionsClosureWatcher(hederaClient, webClient, auctionsRepository, mirrorQueryFrequency, transferOnWin, refundKey);
                break;
            default:
                auctionClosureWatcher = new KabutoAuctionsClosureWatcher(hederaClient, webClient, auctionsRepository, mirrorQueryFrequency, transferOnWin, refundKey);
                break;
        }
        auctionClosureWatcher.watch();
    }
}
