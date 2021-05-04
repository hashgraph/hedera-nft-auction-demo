package com.hedera.demo.auction.node.app.bidwatcher;

import com.hedera.demo.auction.node.app.HederaClient;
import com.hedera.demo.auction.node.app.repository.AuctionsRepository;
import com.hedera.demo.auction.node.app.repository.BidsRepository;
import io.vertx.ext.web.client.WebClient;
import lombok.SneakyThrows;

public class BidsWatcher implements Runnable {

    private final int auctionId;
    private final WebClient webClient;
    private final BidsRepository bidsRepository;
    private final AuctionsRepository auctionsRepository;
    private final String refundKey;
    private final int mirrorQueryFrequency;
    private final String mirrorProvider;
    private final HederaClient hederaClient;
    protected boolean runThread = true;
    private BidsWatcherInterface bidsWatcher = null;

    public BidsWatcher(HederaClient hederaClient, WebClient webClient, AuctionsRepository auctionsRepository, BidsRepository bidsRepository, int auctionId, String refundKey, int mirrorQueryFrequency) {
        this.webClient = webClient;
        this.bidsRepository = bidsRepository;
        this.auctionsRepository = auctionsRepository;
        this.auctionId = auctionId;
        this.refundKey = refundKey;
        this.mirrorQueryFrequency = mirrorQueryFrequency;
        this.hederaClient = hederaClient;
        this.mirrorProvider = hederaClient.mirrorProvider();
    }

    @SneakyThrows
    @Override
    public void run() {
        switch (mirrorProvider) {
            case "HEDERA":
                bidsWatcher = new HederaBidsWatcher(hederaClient, webClient, auctionsRepository, bidsRepository, auctionId, refundKey, mirrorQueryFrequency);
                break;
            default:
                throw new Exception("Support for non Hedera mirrors not implemented.");
        }
        bidsWatcher.watch();
    }

    public void stop() {
        if (bidsWatcher != null) {
            bidsWatcher.stop();
        }
    }
}
