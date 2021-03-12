package com.hedera.demo.auction.node.app;

import com.hedera.demo.auction.node.app.auctionwatchers.AuctionReadinessWatcher;
import com.hedera.demo.auction.node.app.auctionwatchers.AuctionsClosureWatcher;
import com.hedera.demo.auction.node.app.auctionwatchers.BidsWatcher;
import com.hedera.demo.auction.node.app.domain.Auction;
import com.hedera.demo.auction.node.app.refunder.RefundChecker;
import com.hedera.demo.auction.node.app.repository.AuctionsRepository;
import com.hedera.demo.auction.node.app.repository.BidsRepository;
import com.hedera.demo.auction.node.app.subscriber.TopicSubscriber;
import com.hedera.hashgraph.sdk.TopicId;
import io.github.cdimascio.dotenv.Dotenv;
import io.vertx.core.Vertx;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;

import java.util.Optional;

public class App {
    private final static Dotenv env = Dotenv.configure().ignoreIfMissing().load();
    final SqlConnectionManager connectionManager = new SqlConnectionManager(env);

    final AuctionsRepository auctionsRepository = new AuctionsRepository(connectionManager);
    final BidsRepository bidsRepository = new BidsRepository(connectionManager);

    private final Vertx vertx = Vertx.vertx();
    private final static String topicId = Optional.ofNullable(env.get("TOPIC_ID")).orElse("");
    private final int mirrorQueryFrequency = Integer.parseInt(Optional.ofNullable(env.get("MIRROR_QUERY_FREQUENCY")).orElse("5000"));
    private final String refundKey = Optional.ofNullable(env.get("REFUND_KEY")).orElse("");
    private final TopicSubscriber topicSubscriber = new TopicSubscriber(auctionsRepository, bidsRepository, webClient(), TopicId.fromString(topicId), refundKey, mirrorQueryFrequency);

//    private final String dgApiKey = Optional.ofNullable(env.get("DG_API_KEY")).orElse("");

    private App() {
    }

    public static void main(String[] args) throws InterruptedException {
        var app = new App();
        // subscribe to topic to get new auction notifications
        app.startSubscription();
        app.startAuctionReadinessWatchers();
        app.startAuctionsClosureWatcher();
        app.startBidWatchers();
        app.startRefundChecker();
    }

    private WebClient webClient () {
        WebClientOptions webClientOptions = new WebClientOptions()
                .setUserAgent("HederaAuction/1.0")
                .setKeepAlive(false);
        return WebClient.create(vertx, webClientOptions);
    }

    private void startAuctionsClosureWatcher() {
        // start a thread to monitor auction closures
        Thread t = new Thread(new AuctionsClosureWatcher(webClient(), auctionsRepository, mirrorQueryFrequency));
        t.start();
    }
    private void startSubscription() {
        // start the thread to monitor bids
        Thread t = new Thread(topicSubscriber);
        t.start();

    }
    private void startBidWatchers() throws InterruptedException {
        for (Auction auction : auctionsRepository.getAuctionsList()) {
            if (! auction.isPending()) {
                // auction is open or closed
                // start the thread to monitor bids
                Thread t = new Thread(new BidsWatcher(webClient(), auctionsRepository, bidsRepository, auction, refundKey, mirrorQueryFrequency));
                t.start();
            }
        }
    }

    private void startRefundChecker() throws InterruptedException {
        // start the thread to monitor bids
        Thread t = new Thread(new RefundChecker(webClient(), bidsRepository, env));
        t.start();
    }

    private void startAuctionReadinessWatchers() {
        for (Auction auction : auctionsRepository.getAuctionsList()) {
            if (auction.isPending()) {
                // start the thread to monitor token transfers to the auction account
                Thread t = new Thread(new AuctionReadinessWatcher(webClient(), auctionsRepository, bidsRepository, auction, refundKey, mirrorQueryFrequency));
                t.start();
            }
        }
    }
}
