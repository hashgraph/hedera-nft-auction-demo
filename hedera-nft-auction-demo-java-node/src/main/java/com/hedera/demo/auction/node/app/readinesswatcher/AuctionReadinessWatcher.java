package com.hedera.demo.auction.node.app.readinesswatcher;

import com.hedera.demo.auction.node.app.HederaClient;
import com.hedera.demo.auction.node.app.domain.Auction;
import com.hedera.demo.auction.node.app.repository.AuctionsRepository;
import com.hedera.demo.auction.node.app.repository.BidsRepository;
import io.vertx.ext.web.client.WebClient;
import lombok.SneakyThrows;

public class AuctionReadinessWatcher implements Runnable {

    protected final Auction auction;
    protected final WebClient webClient;
    protected final AuctionsRepository auctionsRepository;
    protected final BidsRepository bidsRepository;
    protected final int mirrorQueryFrequency;
    protected final String mirrorProvider;
    protected final String refundKey;
    protected final HederaClient hederaClient;
    private AuctionReadinessWatcherInterface auctionReadinessWatcher;

    public AuctionReadinessWatcher(HederaClient hederaClient, WebClient webClient, AuctionsRepository auctionsRepository, BidsRepository bidsRepository, Auction auction, String refundKey, int mirrorQueryFrequency) {
        this.webClient = webClient;
        this.auctionsRepository = auctionsRepository;
        this.bidsRepository = bidsRepository;
        this.auction = auction;
        this.mirrorQueryFrequency = mirrorQueryFrequency;
        this.refundKey = refundKey;
        this.hederaClient = hederaClient;
        this.mirrorProvider = hederaClient.mirrorProvider();
        this.auctionReadinessWatcher = new HederaAuctionReadinessWatcher(hederaClient, webClient, auctionsRepository, bidsRepository, auction, refundKey, mirrorQueryFrequency);
    }

    /**
     * check transaction history for token, if associated update auction status
     * start new bidding monitor thread
     * and close this thread
     */
    @SneakyThrows
    @Override
    public void run() {
        switch (mirrorProvider) {
            case "HEDERA":
                auctionReadinessWatcher = new HederaAuctionReadinessWatcher(hederaClient, webClient, auctionsRepository, bidsRepository, auction, refundKey, mirrorQueryFrequency);
                break;
            default:
                throw new Exception("Support for non Hedera mirrors not implemented.");
        }
        auctionReadinessWatcher.watch();
    }

    public void stop() {
        if (auctionReadinessWatcher != null) {
            auctionReadinessWatcher.stop();
        }
    }
}
