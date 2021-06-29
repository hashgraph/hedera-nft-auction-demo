package com.hedera.demo.auction.app;

import com.hedera.demo.auction.AuctionEndTransfer;
import com.hedera.demo.auction.AuctionReadinessWatcher;
import com.hedera.demo.auction.AuctionsClosureWatcher;
import com.hedera.demo.auction.BidsWatcher;
import com.hedera.demo.auction.RefundChecker;
import com.hedera.demo.auction.Refunder;
import com.hedera.demo.auction.app.api.AdminApiVerticle;
import com.hedera.demo.auction.app.api.ApiVerticle;
import com.hedera.demo.auction.app.domain.Auction;
import com.hedera.demo.auction.app.repository.AuctionsRepository;
import com.hedera.demo.auction.app.repository.BidsRepository;
import com.hedera.demo.auction.app.subscriber.TopicSubscriber;
import com.hedera.hashgraph.sdk.TopicId;
import io.github.cdimascio.dotenv.Dotenv;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
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
    private String topicId = Optional.ofNullable(env.get("TOPIC_ID")).orElse("");
    private int mirrorQueryFrequency = Integer.parseInt(Optional.ofNullable(env.get("MIRROR_QUERY_FREQUENCY")).orElse("5000"));
    private boolean refund = Optional.ofNullable(env.get("REFUND")).map(Boolean::parseBoolean).orElse(false);
    private String postgresUrl = Optional.ofNullable(env.get("DATABASE_URL")).orElse("postgresql://localhost:5432/postgres");
    private String postgresUser = Optional.ofNullable(env.get("DATABASE_USERNAME")).orElse("postgres");
    private String postgresPassword = Optional.ofNullable(env.get("DATABASE_PASSWORD")).orElse("password");
    private boolean transferOnWin = Optional.ofNullable(env.get("TRANSFER_ON_WIN")).map(Boolean::parseBoolean).orElse(true);
    private String masterKey = Optional.ofNullable(env.get("MASTER_KEY")).orElse("");
    private final int refundThreads = Optional.ofNullable(env.get("REFUND_THREADS")).map(Integer::parseInt).orElse(10);
    private final String operatorKey = env.get("OPERATOR_KEY");
    private HederaClient hederaClient;

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
//    @Nullable
//    private ScheduleExecutor scheduleExecutor = null;
    @Nullable
    private Refunder refunder = null;

    public App() throws Exception {
        hederaClient = new HederaClient(env);
    }

    public static void main(String[] args) throws Exception {
        log.info("Starting app");
        App app = new App();
        app.runApp();
    }

    public void overrideEnv(HederaClient hederaClient, boolean restAPI, boolean adminAPI, boolean auctionNode, String topicId, boolean refund, String postgresUrl, String postgresUser, String postgresPassword, boolean transferOnWin, String masterKey) {
        this.hederaClient = hederaClient;

        this.restAPI = restAPI;
        this.restApiVerticleCount = 1;
        this.adminAPI = adminAPI;
        this.adminApiVerticleCount = 1;
        this.auctionNode = auctionNode;

        this.topicId = topicId;
        this.mirrorQueryFrequency = 10000;
        this.refund = refund;
        this.postgresUrl = postgresUrl.replaceAll("jdbc:", "");
        this.postgresUser = postgresUser;
        this.postgresPassword = postgresPassword;
        this.transferOnWin = transferOnWin;
        this.masterKey = masterKey;
    }

    public void runApp() throws Exception {

        log.info("applying database migrations");
        Flyway flyway = Flyway
                .configure()
                .dataSource("jdbc:".concat(postgresUrl), postgresUser, postgresPassword)
                .locations("classpath:migrations")
                .connectRetries(20)
                .load();
        flyway.migrate();

        if (restAPI) {
            log.info("starting client REST api");
            JsonObject config = new JsonObject()
                    .put("envFile",".env")
                    .put("envPath",".")
                    .put("topicId", this.topicId);
            DeploymentOptions options = new DeploymentOptions().setConfig(config).setInstances(restApiVerticleCount);
            vertx.deployVerticle(ApiVerticle.class.getName(), options);
        }

        if (adminAPI) {
            log.info("starting admin REST api");
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

            // perform a one off check for new auctions and bids
            startSubscription(auctionsRepository, /* runOnce= */ true);
            // check for completed refunds (one off)
            startRefundChecker(auctionsRepository, bidsRepository, /* runOnce= */ true);

            // now subscribe for new events
            // subscribe to topic to get new auction notifications
            startSubscription(auctionsRepository, /* runOnce= */ false);

            startAuctionReadinessWatchers(auctionsRepository);
            startAuctionsClosureWatcher(auctionsRepository);
            startBidWatchers(auctionsRepository, /* runOnce= */ false);
            if (refund) {
                // validator node, start the refunder thread
                startRefunder(auctionsRepository, bidsRepository, refundThreads);
            }
            startRefundChecker(auctionsRepository, bidsRepository, /* runOnce= */ false);
            if (transferOnWin) {
                startAuctionEndTransfers(auctionsRepository);
            }
        }
    }

    private void startAuctionsClosureWatcher(AuctionsRepository auctionsRepository) {
        // start a thread to monitor auction closures
        auctionsClosureWatcher = new AuctionsClosureWatcher(hederaClient, auctionsRepository, mirrorQueryFrequency, transferOnWin, masterKey);
        Thread auctionsClosureWatcherThread = new Thread(auctionsClosureWatcher);
        auctionsClosureWatcherThread.start();
    }
    private void startSubscription(AuctionsRepository auctionsRepository, boolean runOnce) {
        if (StringUtils.isEmpty(topicId)) {
            log.warn("No topic Id found in environment variables, not subscribing");
        } else {
            topicSubscriber = new TopicSubscriber(hederaClient, auctionsRepository,TopicId.fromString(topicId), mirrorQueryFrequency, masterKey, runOnce);
            if (runOnce) {
                // don't run as a thread
                topicSubscriber.run();
            } else {
                // start the thread to monitor bids
                Thread topicSubscriberThread = new Thread(topicSubscriber);
                topicSubscriberThread.start();
            }
        }
    }
    private void startBidWatchers(AuctionsRepository auctionsRepository, boolean runOnce) throws SQLException {
        for (Auction auction : auctionsRepository.getAuctionsList()) {
            if (! auction.isPending()) {
                // auction is not pending
                // start the thread to monitor bids
                BidsWatcher bidsWatcher = new BidsWatcher(hederaClient, auctionsRepository,  auction.getId(), mirrorQueryFrequency, runOnce);
                if (runOnce) {
                    // do not run as a thread
                    bidsWatcher.run();
                } else {
                    Thread t = new Thread(bidsWatcher);
                    t.start();
                    bidsWatchers.add(bidsWatcher);
                }
            }
        }
    }

    private void startRefundChecker(AuctionsRepository auctionsRepository, BidsRepository bidsRepository, boolean runOnce) {
        // start the thread to monitor bids
        refundChecker = new RefundChecker(hederaClient, auctionsRepository, bidsRepository, mirrorQueryFrequency, runOnce);
        if (runOnce) {
            // do not run as a thread
            refundChecker.run();
        } else {
            Thread refundCheckerThread = new Thread(refundChecker);
            refundCheckerThread.start();
        }
    }

    private void startAuctionReadinessWatchers(AuctionsRepository auctionsRepository) throws SQLException {
        for (Auction auction : auctionsRepository.getAuctionsList()) {
            if (auction.isPending()) {
                // start the thread to monitor token transfers to the auction account
                AuctionReadinessWatcher auctionReadinessWatcher = new AuctionReadinessWatcher(hederaClient, auctionsRepository, auction, mirrorQueryFrequency);
                Thread t = new Thread(auctionReadinessWatcher);
                t.start();
                auctionReadinessWatchers.add(auctionReadinessWatcher);
            }
        }
    }

    private void startAuctionEndTransfers(AuctionsRepository auctionsRepository) {
        // start the thread to monitor winning account association with token
        auctionEndTransfer = new AuctionEndTransfer(hederaClient, auctionsRepository, operatorKey, mirrorQueryFrequency);
        Thread auctionEndTransferThread = new Thread(auctionEndTransfer);
        auctionEndTransferThread.start();
    }

    private void startRefunder(AuctionsRepository auctionsRepository, BidsRepository bidsRepository, int refundThreads) {
        // start the thread to monitor winning account association with token
        refunder = new Refunder(hederaClient, auctionsRepository, bidsRepository, mirrorQueryFrequency, refundThreads);
        Thread refunderThread = new Thread(refunder);
        refunderThread.start();
    }

    public void stop() {
        for (String verticle : vertx.deploymentIDs()) {
            vertx.undeploy(verticle);
        }

        if (topicSubscriber != null) {
            topicSubscriber.stop();
        }
        if (auctionsClosureWatcher != null) {
            auctionsClosureWatcher.stop();
        }
//        if (scheduleExecutor != null) {
//            scheduleExecutor.stop();
//        }
        if (refundChecker != null) {
            refundChecker.stop();
        }

        if (auctionEndTransfer != null) {
            auctionEndTransfer.stop();
        }

        for (BidsWatcher bidsWatcher : bidsWatchers) {
            bidsWatcher.stop();
        }

        for (AuctionReadinessWatcher auctionReadinessWatcher : auctionReadinessWatchers) {
            auctionReadinessWatcher.stop();
        }

        if (refunder != null) {
            refunder.stop();
        }
    }
}
