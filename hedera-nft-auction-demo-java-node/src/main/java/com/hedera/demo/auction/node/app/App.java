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
import com.hedera.demo.auction.node.app.auctionendtransfer.AuctionEndTransfer;
import com.hedera.demo.auction.node.app.auctionendtransferwatcher.AuctionEndTransferWatcher;
import com.hedera.hashgraph.sdk.TopicId;
import io.github.cdimascio.dotenv.Dotenv;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import lombok.extern.log4j.Log4j2;
import org.flywaydb.core.Flyway;
import org.jooq.tools.StringUtils;

import javax.annotation.Nullable;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Log4j2
public final class App {
    Vertx vertx = Vertx.vertx();
    private final Dotenv env = Dotenv.configure().ignoreIfMissing().load();
    private boolean restAPI = Optional.ofNullable(env.get("REST_API")).map(Boolean::parseBoolean).orElse(false);
    private int restApiVerticleCount = Optional.ofNullable(env.get("API_VERTICLE_COUNT")).map(Integer::parseInt).orElse(2);
    private boolean adminAPI = Optional.ofNullable(env.get("ADMIN_API")).map(Boolean::parseBoolean).orElse(false);
    private int adminApiVerticleCount = Optional.ofNullable(env.get("ADMIN_API_VERTICLE_COUNT")).map(Integer::parseInt).orElse(2);
    private boolean auctionNode = Optional.ofNullable(env.get("AUCTION_NODE")).map(Boolean::parseBoolean).orElse(false);

    private String topicId = Optional.ofNullable(env.get("VUE_APP_TOPIC_ID")).orElse("");
    private int mirrorQueryFrequency = Integer.parseInt(Optional.ofNullable(env.get("MIRROR_QUERY_FREQUENCY")).orElse("5000"));
    private String refundKey = Optional.ofNullable(env.get("REFUND_KEY")).orElse("");
    private String postgresUrl = Optional.ofNullable(env.get("DATABASE_URL")).orElse("postgresql://localhost:5432/postgres");
    private String postgresUser = Optional.ofNullable(env.get("DATABASE_USERNAME")).orElse("postgres");
    private String postgresPassword = Optional.ofNullable(env.get("DATABASE_PASSWORD")).orElse("password");
    private boolean transferOnWin = Optional.ofNullable(env.get("TRANSFER_ON_WIN")).map(Boolean::parseBoolean).orElse(true);

    private HederaClient hederaClient = new HederaClient(env);

    @Nullable
    private TopicSubscriber topicSubscriber = null;
    @Nullable
    private AuctionsClosureWatcher auctionsClosureWatcher = null;
    private final List<BidsWatcher> bidsWatchers = new ArrayList<>(0);
    @Nullable
    private RefundChecker refundChecker = null;
    private final List<AuctionReadinessWatcher> auctionReadinessWatchers = new ArrayList<>(0);
    @Nullable
    private AuctionEndTransfer auctionEndTransfer = null;
    @Nullable
    private AuctionEndTransferWatcher auctionEndTransferWatcher = null;

    public App() throws Exception {
    }

    //    private final static String dgApiKey = Optional.ofNullable(env.get("DG_API_KEY")).orElse("");

    public static void main(String[] args) throws Exception {
        App app = new App();
        app.runApp();
    }

    public void overrideEnv(HederaClient hederaClient, boolean restAPI, boolean adminAPI, boolean auctionNode, String topicId, String refundKey, String postgresUrl, String postgresUser, String postgresPassword, boolean transferOnWin) {
        this.hederaClient = hederaClient;

        this.restAPI = restAPI;
        this.restApiVerticleCount = 1;
        this.adminAPI = adminAPI;
        this.adminApiVerticleCount = 1;
        this.auctionNode = auctionNode;

        this.topicId = topicId;
        this.mirrorQueryFrequency = 1000;
        this.refundKey = refundKey;
        this.postgresUrl = postgresUrl.replaceAll("jdbc:", "");
        this.postgresUser = postgresUser;
        this.postgresPassword = postgresPassword;
        this.transferOnWin = transferOnWin;
    }

    public void runApp() throws Exception {
        Flyway flyway = Flyway
                .configure()
                .dataSource("jdbc:".concat(postgresUrl), postgresUser, postgresPassword)
                .locations("classpath:migrations")
                .connectRetries(20)
                .load();
        flyway.migrate();

        if (restAPI) {
            JsonObject config = new JsonObject()
                    .put("envFile",".env")
                    .put("envPath",".");
            DeploymentOptions options = new DeploymentOptions().setConfig(config).setInstances(restApiVerticleCount);
            vertx.deployVerticle(ApiVerticle.class.getName(), options);
        }

        if (adminAPI) {
            JsonObject config = new JsonObject()
                    .put("envFile",".env")
                    .put("envPath",".");
            DeploymentOptions options = new DeploymentOptions().setConfig(config).setInstances(adminApiVerticleCount);
            vertx.deployVerticle(AdminApiVerticle.class.getName(), options);
        }

        if (auctionNode) {
            SqlConnectionManager connectionManager = new SqlConnectionManager(this.postgresUrl, this.postgresUser, this.postgresPassword);
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
                startAuctionEndTransfers(hederaClient, webClient, auctionsRepository);
                startAuctionEndTransferWatcher(hederaClient, webClient, auctionsRepository);
            }
        }
    }

    private void startAuctionsClosureWatcher(HederaClient hederaClient, WebClient webClient, AuctionsRepository auctionsRepository, boolean transferOnWin) {
        // start a thread to monitor auction closures
        auctionsClosureWatcher = new AuctionsClosureWatcher(hederaClient, webClient, auctionsRepository, mirrorQueryFrequency, transferOnWin, refundKey);
        Thread auctionsClosureWatcherThread = new Thread(auctionsClosureWatcher);
        auctionsClosureWatcherThread.start();
    }
    private void startSubscription(WebClient webClient, AuctionsRepository auctionsRepository, BidsRepository bidsRepository) {
        if (StringUtils.isEmpty(topicId)) {
            log.warn("No topic Id found in environment variables, not subscribing");
        } else {
            topicSubscriber = new TopicSubscriber(hederaClient, auctionsRepository, bidsRepository, webClient, TopicId.fromString(topicId), refundKey, mirrorQueryFrequency);
            // start the thread to monitor bids
            Thread topicSubscriberThread = new Thread(topicSubscriber);
            topicSubscriberThread.start();
        }
    }
    private void startBidWatchers(HederaClient hederaClient, WebClient webClient, AuctionsRepository auctionsRepository, BidsRepository bidsRepository) throws SQLException {
        for (Auction auction : auctionsRepository.getAuctionsList()) {
            if (! auction.isPending()) {
                // auction is not pending
                // start the thread to monitor bids
                BidsWatcher bidsWatcher = new BidsWatcher(hederaClient, webClient, auctionsRepository, bidsRepository, auction.getId(), refundKey, mirrorQueryFrequency);
                Thread t = new Thread(bidsWatcher);
                t.start();
                bidsWatchers.add(bidsWatcher);
            }
        }
    }

    private void startRefundChecker(HederaClient hederaClient, WebClient webClient, BidsRepository bidsRepository) {
        // start the thread to monitor bids
        refundChecker = new RefundChecker(hederaClient, webClient, bidsRepository, mirrorQueryFrequency);
        Thread refundCheckerThread = new Thread(refundChecker);
        refundCheckerThread.start();
    }

    private void startAuctionReadinessWatchers(HederaClient hederaClient, WebClient webClient, AuctionsRepository auctionsRepository, BidsRepository bidsRepository) throws SQLException {
        for (Auction auction : auctionsRepository.getAuctionsList()) {
            if (auction.isPending()) {
                // start the thread to monitor token transfers to the auction account
                AuctionReadinessWatcher auctionReadinessWatcher = new AuctionReadinessWatcher(hederaClient, webClient, auctionsRepository, bidsRepository, auction, refundKey, mirrorQueryFrequency);
                Thread t = new Thread(auctionReadinessWatcher);
                t.start();
                auctionReadinessWatchers.add(auctionReadinessWatcher);
            }
        }
    }

    private void startAuctionEndTransfers(HederaClient hederaClient, WebClient webClient, AuctionsRepository auctionsRepository) {
        // start the thread to monitor winning account association with token
        auctionEndTransfer = new AuctionEndTransfer(hederaClient, webClient, auctionsRepository, refundKey, mirrorQueryFrequency);
        Thread auctionEndTransferThread = new Thread(auctionEndTransfer);
        auctionEndTransferThread.start();
    }

    private void startAuctionEndTransferWatcher(HederaClient hederaClient, WebClient webClient, AuctionsRepository auctionsRepository) {
        // start the thread to monitor winning account association with token
        auctionEndTransferWatcher = new AuctionEndTransferWatcher(hederaClient, webClient, auctionsRepository, mirrorQueryFrequency);
        Thread auctionEndTransferWatcherThread = new Thread(auctionEndTransferWatcher);
        auctionEndTransferWatcherThread.start();
    }

    public void stop() {
        if (topicSubscriber != null) {
            topicSubscriber.stop();
        }
        if (auctionsClosureWatcher != null) {
            auctionsClosureWatcher.stop();
        }
        if (refundChecker != null) {
            refundChecker.stop();
        }

        if (auctionEndTransfer != null) {
            auctionEndTransfer.stop();
        }

        if (auctionEndTransferWatcher != null) {
            auctionEndTransferWatcher.stop();
        }

        for (BidsWatcher bidsWatcher : bidsWatchers) {
            bidsWatcher.stop();
        }

        for (AuctionReadinessWatcher auctionReadinessWatcher : auctionReadinessWatchers) {
            auctionReadinessWatcher.stop();
        }

        for (String verticle : vertx.deploymentIDs()) {
            vertx.undeploy(verticle);
        }
    }
}
