package com.hedera.demo.auction.node.app.readinesswatchers;

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
    protected final String mirrorProvider = HederaClient.getMirrorProvider();
    protected final String refundKey;

    public AuctionReadinessWatcher(WebClient webClient, AuctionsRepository auctionsRepository, BidsRepository bidsRepository, Auction auction, String refundKey, int mirrorQueryFrequency) {
        this.webClient = webClient;
        this.auctionsRepository = auctionsRepository;
        this.bidsRepository = bidsRepository;
        this.auction = auction;
        this.mirrorQueryFrequency = mirrorQueryFrequency;
        this.refundKey = refundKey;
    }

    /**
     * check transaction history for token, if associated update auction status
     * start new bidding monitor thread
     * and close this thread
     */
    @SneakyThrows
    @Override
    public void run() {
        AuctionReadinessWatcherInterface auctionReadinessWatcher;
        switch (mirrorProvider) {
            case "HEDERA":
                auctionReadinessWatcher = new HederaAuctionReadinessWatcher(webClient, auctionsRepository, bidsRepository, auction, refundKey, mirrorQueryFrequency);
                break;
            case "DRAGONGLASS":
                auctionReadinessWatcher = new DragonglassAuctionReadinessWatcher(webClient, auctionsRepository, bidsRepository, auction, refundKey, mirrorQueryFrequency);
                break;
            default:
                auctionReadinessWatcher = new KabutoAuctionReadinessWatcher(webClient, auctionsRepository, bidsRepository, auction, refundKey, mirrorQueryFrequency);
                break;
        }
        auctionReadinessWatcher.watch();
    }
}
