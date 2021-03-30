package com.hedera.demo.auction.node.app;

import com.hedera.demo.auction.node.app.api.AdminApiVerticle;
import com.hedera.demo.auction.node.app.api.ApiVerticle;
import com.hedera.demo.auction.node.app.bidwatcher.BidsWatcher;
import com.hedera.demo.auction.node.app.closurewatcher.AuctionsClosureWatcher;
import com.hedera.demo.auction.node.app.domain.Auction;
import com.hedera.demo.auction.node.app.winnertokentransfer.WinnerTokenTransfer;
import com.hedera.demo.auction.node.app.readinesswatcher.AuctionReadinessWatcher;
import com.hedera.demo.auction.node.app.refundChecker.RefundChecker;
import com.hedera.demo.auction.node.app.repository.AuctionsRepository;
import com.hedera.demo.auction.node.app.repository.BidsRepository;
import com.hedera.demo.auction.node.app.subscriber.TopicSubscriber;
import com.hedera.demo.auction.node.app.winnertokentransferwatcher.WinnerTokenTransferWatcher;
import com.hedera.hashgraph.sdk.TopicId;
import io.github.cdimascio.dotenv.Dotenv;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import org.flywaydb.core.Flyway;

import java.util.Optional;

public final class App {
    private final static Dotenv env = Dotenv.configure().ignoreIfMissing().load();
    private final static boolean restAPI = Optional.ofNullable(env.get("REST_API")).map(Boolean::parseBoolean).orElse(false);
    private final static int restApiVerticleCount = Optional.ofNullable(env.get("API_VERTICLE_COUNT")).map(Integer::parseInt).orElse(2);
    private final static boolean adminAPI = Optional.ofNullable(env.get("ADMIN_API")).map(Boolean::parseBoolean).orElse(false);
    private final static int adminApiVerticleCount = Optional.ofNullable(env.get("ADMIN_API_VERTICLE_COUNT")).map(Integer::parseInt).orElse(2);
    private final static boolean auctionNode = Optional.ofNullable(env.get("AUCTION_NODE")).map(Boolean::parseBoolean).orElse(false);

    private final static String topicId = Optional.ofNullable(env.get("VUE_APP_TOPIC_ID")).orElse("");
    private final static int mirrorQueryFrequency = Integer.parseInt(Optional.ofNullable(env.get("MIRROR_QUERY_FREQUENCY")).orElse("5000"));
    private final static String refundKey = Optional.ofNullable(env.get("REFUND_KEY")).orElse("");
    private final static String postgresUrl = Optional.ofNullable(env.get("DATABASE_URL")).orElse("postgresql://localhost:5432/postgres");
    private final static String postgresUser = Optional.ofNullable(env.get("DATABASE_USERNAME")).orElse("postgres");
    private final static String postgresPassword = Optional.ofNullable(env.get("DATABASE_PASSWORD")).orElse("password");
    private final static boolean transferOnWin = Optional.ofNullable((env.get("TRANSFER_ON_WIN")).map(Boolean::parseBoolean).orElse(true);

    //    private final static String dgApiKey = Optional.ofNullable(env.get("DG_API_KEY")).orElse("");

    private App() {
    }

    public static void main(String[] args) throws Exception {
        Flyway flyway = Flyway
                .configure()
                .dataSource("jdbc:".concat(postgresUrl), postgresUser, postgresPassword)
                .locations("classpath:migrations")
                .connectRetries(20)
                .load();
        flyway.migrate();

        if (restAPI) {
            Vertx.vertx().deployVerticle(
                    ApiVerticle.class.getName(),
                    new DeploymentOptions().setInstances(restApiVerticleCount));
        }

        if (adminAPI) {
            Vertx.vertx().deployVerticle(
                    AdminApiVerticle.class.getName(),
                    new DeploymentOptions().setInstances(adminApiVerticleCount));
        }

        if (auctionNode) {
            SqlConnectionManager connectionManager = new SqlConnectionManager(env);
            AuctionsRepository auctionsRepository = new AuctionsRepository(connectionManager);
            BidsRepository bidsRepository = new BidsRepository(connectionManager);

            WebClientOptions webClientOptions = new WebClientOptions()
                    .setUserAgent("HederaAuction/1.0")
                    .setKeepAlive(false);
            WebClient webClient = WebClient.create(Vertx.vertx(), webClientOptions);

            // subscribe to topic to get new auction notifications
            startSubscription(webClient, auctionsRepository, bidsRepository);
            startAuctionReadinessWatchers(webClient, auctionsRepository, bidsRepository);
            startAuctionsClosureWatcher(webClient, auctionsRepository, transferOnWin);
            startBidWatchers(webClient, auctionsRepository, bidsRepository);
            startRefundChecker(webClient, bidsRepository);
            if (transferOnWin) {
                startWinnerTokenTransfers(webClient, auctionsRepository);
                startWinnerTokenTransferWatcher(webClient, auctionsRepository);
            }
        }
    }

    private static void startAuctionsClosureWatcher(WebClient webClient, AuctionsRepository auctionsRepository, boolean transferOnWin) {
        // start a thread to monitor auction closures
        Thread t = new Thread(new AuctionsClosureWatcher(webClient, auctionsRepository, mirrorQueryFrequency, transferOnWin));
        t.start();
    }
    private static void startSubscription(WebClient webClient, AuctionsRepository auctionsRepository, BidsRepository bidsRepository) {
        TopicSubscriber topicSubscriber = new TopicSubscriber(auctionsRepository, bidsRepository, webClient, TopicId.fromString(topicId), refundKey, mirrorQueryFrequency);
        // start the thread to monitor bids
        Thread t = new Thread(topicSubscriber);
        t.start();

    }
    private static void startBidWatchers(WebClient webClient, AuctionsRepository auctionsRepository, BidsRepository bidsRepository) {
        for (Auction auction : auctionsRepository.getAuctionsList()) {
            if (! auction.isPending()) {
                // auction is not pending
                // start the thread to monitor bids
                Thread t = new Thread(new BidsWatcher(webClient, auctionsRepository, bidsRepository, auction.getId(), refundKey, mirrorQueryFrequency));
                t.start();
            }
        }
    }

    private static void startRefundChecker(WebClient webClient, BidsRepository bidsRepository) throws Exception {
        // start the thread to monitor bids
        Thread t = new Thread(new RefundChecker(webClient, bidsRepository, env));
        t.start();
    }

    private static void startAuctionReadinessWatchers(WebClient webClient, AuctionsRepository auctionsRepository, BidsRepository bidsRepository) {
        for (Auction auction : auctionsRepository.getAuctionsList()) {
            if (auction.isPending()) {
                // start the thread to monitor token transfers to the auction account
                Thread t = new Thread(new AuctionReadinessWatcher(webClient, auctionsRepository, bidsRepository, auction, refundKey, mirrorQueryFrequency));
                t.start();
            }
        }
    }

    private static void startWinnerTokenTransfers(WebClient webClient, AuctionsRepository auctionsRepository) throws Exception {
        // start the thread to monitor winning account association with token
        Thread t = new Thread(new WinnerTokenTransfer(webClient, auctionsRepository, refundKey, mirrorQueryFrequency));
        t.start();
    }

    private static void startWinnerTokenTransferWatcher(WebClient webClient, AuctionsRepository auctionsRepository) {
        // start the thread to monitor winning account association with token
        Thread t = new Thread(new WinnerTokenTransferWatcher(webClient, auctionsRepository, mirrorQueryFrequency));
        t.start();
    }
}
