package com.hedera.demo.auction.node.app.bidwatcher;

import com.hedera.demo.auction.node.app.HederaClient;
import com.hedera.demo.auction.node.app.domain.Auction;
import com.hedera.demo.auction.node.app.repository.AuctionsRepository;
import com.hedera.demo.auction.node.app.repository.BidsRepository;
import io.vertx.ext.web.client.WebClient;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class BidsWatcher implements Runnable {

    private final int auctionId;
    private final WebClient webClient;
    private final BidsRepository bidsRepository;
    private final AuctionsRepository auctionsRepository;
    private final String refundKey;
    private final int mirrorQueryFrequency;
    private final String mirrorProvider = HederaClient.getMirrorProvider();

    public BidsWatcher(WebClient webClient, AuctionsRepository auctionsRepository, BidsRepository bidsRepository, int auctionId, String refundKey, int mirrorQueryFrequency) {
        this.webClient = webClient;
        this.bidsRepository = bidsRepository;
        this.auctionsRepository = auctionsRepository;
        this.auctionId = auctionId;
        this.refundKey = refundKey;
        this.mirrorQueryFrequency = mirrorQueryFrequency;
    }

    @SneakyThrows
    @Override
    public void run() {

        Auction auction = auctionsRepository.getAuction(auctionId);

        log.info("Watching auction account Id " + auction.getAuctionaccountid() + ", token Id " + auction.getTokenid());

        BidsWatcherInterface bidsWatcher;
        switch (mirrorProvider) {
            case "HEDERA":
                bidsWatcher = new HederaBidsWatcher(webClient, auctionsRepository, bidsRepository, auction, refundKey, mirrorQueryFrequency);
                break;
            case "DRAGONGLASS":
                bidsWatcher = new DragonglassBidsWatcher(webClient, auctionsRepository, bidsRepository, auction, refundKey, mirrorQueryFrequency);
                break;
            default:
                bidsWatcher = new KabutoBidsWatcher(webClient, auctionsRepository, bidsRepository, auction, refundKey, mirrorQueryFrequency);
                break;
        }
        bidsWatcher.watch();
    }
}
