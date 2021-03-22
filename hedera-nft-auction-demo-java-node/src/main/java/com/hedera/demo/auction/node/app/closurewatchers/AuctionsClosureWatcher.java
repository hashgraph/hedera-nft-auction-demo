package com.hedera.demo.auction.node.app.closurewatchers;

import com.hedera.demo.auction.node.app.HederaClient;
import com.hedera.demo.auction.node.app.repository.AuctionsRepository;
import io.vertx.ext.web.client.WebClient;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;

import java.util.concurrent.atomic.AtomicReference;

@Log4j2
public class AuctionsClosureWatcher implements Runnable {

    private final WebClient webClient;
    private final AuctionsRepository auctionsRepository;
    private final int mirrorQueryFrequency;
    private String mirrorURL = "";
    private final String mirrorProvider = HederaClient.getMirrorProvider();

    public AuctionsClosureWatcher(WebClient webClient, AuctionsRepository auctionsRepository, int mirrorQueryFrequency) throws Exception {
        this.webClient = webClient;
        this.auctionsRepository = auctionsRepository;
        this.mirrorQueryFrequency = mirrorQueryFrequency;
        this.mirrorURL = HederaClient.getMirrorUrl();
    }

    @SneakyThrows
    @Override
    public void run() {
        AtomicReference<String> uri = new AtomicReference<>("");

        switch (mirrorProvider) {
            case "HEDERA":
                HederaAuctionsClosureWatcher hederaAuctionsClosureWatcher = new HederaAuctionsClosureWatcher(webClient, auctionsRepository, mirrorQueryFrequency);
                hederaAuctionsClosureWatcher.watch();
                break;
            case "DRAGONGLASS":
                DragonglassAuctionsClosureWatcher dragonglassAuctionsClosureWatcher = new DragonglassAuctionsClosureWatcher(webClient, auctionsRepository, mirrorQueryFrequency);
                dragonglassAuctionsClosureWatcher.watch();
                break;
            default:
                KabutoAuctionsClosureWatcher kabutoAuctionsClosureWatcher = new KabutoAuctionsClosureWatcher(webClient, auctionsRepository, mirrorQueryFrequency);
                kabutoAuctionsClosureWatcher.watch();
                break;
        }
    }
}
