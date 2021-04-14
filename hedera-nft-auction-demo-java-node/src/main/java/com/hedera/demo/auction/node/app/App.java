package com.hedera.demo.auction.node.app;

import com.hedera.demo.auction.node.app.api.AdminApiVerticle;
import com.hedera.demo.auction.node.app.api.ApiVerticle;
import com.hedera.demo.auction.node.app.bidwatcher.BidsWatcher;
import com.hedera.demo.auction.node.app.closurewatcher.AuctionsClosureWatcher;
import com.hedera.demo.auction.node.app.domain.Auction;
import com.hedera.demo.auction.node.app.readinesswatcher.AuctionReadinessWatcher;
import com.hedera.demo.auction.node.app.refundChecker.RefundChecker;
import com.hedera.demo.auction.node.app.repository.AuctionsRepository;
import com.hedera.demo.auction.node.app.repository.BidsRepository;
import com.hedera.demo.auction.node.app.subscriber.TopicSubscriber;
import com.hedera.demo.auction.node.app.winnertokentransfer.WinnerTokenTransfer;
import com.hedera.demo.auction.node.app.winnertokentransferwatcher.WinnerTokenTransferWatcher;
import com.hedera.hashgraph.sdk.TopicId;
import io.github.cdimascio.dotenv.Dotenv;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import org.flywaydb.core.Flyway;

import java.sql.SQLException;
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
    private final static boolean transferOnWin = Optional.ofNullable(env.get("TRANSFER_ON_WIN")).map(Boolean::parseBoolean).orElse(true);

    private static HederaClient hederaClient;

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

        hederaClient = new HederaClient(env);

        if (restAPI) {
            JsonObject config = new JsonObject()
                    .put("envFile",".env")
                    .put("envPath",".");
            DeploymentOptions options = new DeploymentOptions().setConfig(config).setInstances(restApiVerticleCount);
            Vertx.vertx().deployVerticle(ApiVerticle.class.getName(), options);
        }

        if (adminAPI) {
            JsonObject config = new JsonObject()
                    .put("envFile",".env")
                    .put("envPath",".");
            DeploymentOptions options = new DeploymentOptions().setConfig(config).setInstances(adminApiVerticleCount);
            Vertx.vertx().deployVerticle(AdminApiVerticle.class.getName(), options);
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
            startAuctionReadinessWatchers(hederaClient, webClient, auctionsRepository, bidsRepository);
            startAuctionsClosureWatcher(hederaClient, webClient, auctionsRepository, transferOnWin);
            startBidWatchers(hederaClient, webClient, auctionsRepository, bidsRepository);
            startRefundChecker(hederaClient, webClient, bidsRepository);
            if (transferOnWin) {
                startWinnerTokenTransfers(hederaClient, webClient, auctionsRepository);
                startWinnerTokenTransferWatcher(hederaClient, webClient, auctionsRepository);
            }
        }
    }

    private static void startAuctionsClosureWatcher(HederaClient hederaClient, WebClient webClient, AuctionsRepository auctionsRepository, boolean transferOnWin) {
        // start a thread to monitor auction closures
        Thread t = new Thread(new AuctionsClosureWatcher(hederaClient, webClient, auctionsRepository, mirrorQueryFrequency, transferOnWin, refundKey));
        t.start();
    }
    private static void startSubscription(WebClient webClient, AuctionsRepository auctionsRepository, BidsRepository bidsRepository) {
        TopicSubscriber topicSubscriber = new TopicSubscriber(hederaClient, auctionsRepository, bidsRepository, webClient, TopicId.fromString(topicId), refundKey, mirrorQueryFrequency);
        // start the thread to monitor bids
        Thread t = new Thread(topicSubscriber);
        t.start();

    }
    private static void startBidWatchers(HederaClient hederaClient, WebClient webClient, AuctionsRepository auctionsRepository, BidsRepository bidsRepository) throws SQLException {
        for (Auction auction : auctionsRepository.getAuctionsList()) {
            if (! auction.isPending()) {
                // auction is not pending
                // start the thread to monitor bids
                Thread t = new Thread(new BidsWatcher(hederaClient, webClient, auctionsRepository, bidsRepository, auction.getId(), refundKey, mirrorQueryFrequency));
                t.start();
            }
        }
    }

    private static void startRefundChecker(HederaClient hederaClient, WebClient webClient, BidsRepository bidsRepository) {
        // start the thread to monitor bids
        Thread t = new Thread(new RefundChecker(hederaClient, webClient, bidsRepository, mirrorQueryFrequency));
        t.start();
    }

    private static void startAuctionReadinessWatchers(HederaClient hederaClient, WebClient webClient, AuctionsRepository auctionsRepository, BidsRepository bidsRepository) throws SQLException {
        for (Auction auction : auctionsRepository.getAuctionsList()) {
            if (auction.isPending()) {
                // start the thread to monitor token transfers to the auction account
                Thread t = new Thread(new AuctionReadinessWatcher(hederaClient, webClient, auctionsRepository, bidsRepository, auction, refundKey, mirrorQueryFrequency));
                t.start();
            }
        }
    }

    private static void startWinnerTokenTransfers(HederaClient hederaClient, WebClient webClient, AuctionsRepository auctionsRepository) {
        // start the thread to monitor winning account association with token
        Thread t = new Thread(new WinnerTokenTransfer(hederaClient, webClient, auctionsRepository, refundKey, mirrorQueryFrequency));
        t.start();
    }

    private static void startWinnerTokenTransferWatcher(HederaClient hederaClient, WebClient webClient, AuctionsRepository auctionsRepository) {
        // start the thread to monitor winning account association with token
        Thread t = new Thread(new WinnerTokenTransferWatcher(hederaClient, webClient, auctionsRepository, mirrorQueryFrequency));
        t.start();
    }
}
