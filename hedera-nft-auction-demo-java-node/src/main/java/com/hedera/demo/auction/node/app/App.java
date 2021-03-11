package com.hedera.demo.auction.node.app;

import com.hedera.demo.auction.node.app.auctionwatchers.AuctionReadinessWatcher;
import com.hedera.demo.auction.node.app.auctionwatchers.BidsWatcher;
import com.hedera.demo.auction.node.app.domain.Auction;
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
    private final TopicSubscriber topicSubscriber = new TopicSubscriber(auctionsRepository, bidsRepository, webClient(), TopicId.fromString(topicId), env);

//    private final String dgApiKey = Optional.ofNullable(env.get("DG_API_KEY")).orElse("");

    private App() {
    }

    public static void main(String[] args) {
        var app = new App();
        // subscribe to topic to get new auction notifications
        app.startSubscription();
        app.startAuctionReadinessWatchers();
        app.startBidWatchers();
    }

    private WebClient webClient () {
        WebClientOptions webClientOptions = new WebClientOptions()
                .setUserAgent("HederaAuction/1.0")
                .setKeepAlive(false);
        return WebClient.create(vertx, webClientOptions);
    }

    private void startSubscription() {
        // start the thread to monitor bids
        Thread t = new Thread(topicSubscriber);
        t.start();

    }
    private void startBidWatchers() {
        for (Auction auction : auctionsRepository.getAuctionsList()) {
            if (auction.isActive()) {
                // start the thread to monitor bids
                Thread t = new Thread(new BidsWatcher(webClient(), auctionsRepository, bidsRepository, auction, env));
                t.start();
            }
        }
    }

    private void startAuctionReadinessWatchers() {
        for (Auction auction : auctionsRepository.getAuctionsList()) {
            if (auction.isPending()) {
                // start the thread to monitor bids
                Thread t = new Thread(new AuctionReadinessWatcher(webClient(), auctionsRepository, bidsRepository, auction, env));
                t.start();
            }
        }
    }
}
