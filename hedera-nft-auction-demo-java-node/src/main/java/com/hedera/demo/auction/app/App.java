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
import com.hedera.demo.auction.app.repository.ValidatorsRepository;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * This is the starting point for the application
 */
@Log4j2
public final class App {
    private final Vertx vertx = Vertx.vertx();
    private final Dotenv env = Dotenv.configure().ignoreIfMissing().load();
    @SuppressWarnings("FieldMissingNullable")
    private boolean restAPI = Optional.ofNullable(env.get("REST_API")).map(Boolean::parseBoolean).orElse(false);
    @SuppressWarnings("FieldMissingNullable")
    private int restApiVerticleCount = Optional.ofNullable(env.get("API_VERTICLE_COUNT")).map(Integer::parseInt).orElse(2);
    @SuppressWarnings("FieldMissingNullable")
    private String adminAPIKey = Optional.ofNullable(env.get("X_API_KEY")).orElse("");
    @SuppressWarnings("FieldMissingNullable")
    private int adminApiVerticleCount = Optional.ofNullable(env.get("ADMIN_API_VERTICLE_COUNT")).map(Integer::parseInt).orElse(2);
    @SuppressWarnings("FieldMissingNullable")
    private boolean auctionNode = Optional.ofNullable(env.get("AUCTION_NODE")).map(Boolean::parseBoolean).orElse(false);
    @SuppressWarnings("FieldMissingNullable")
    private String topicId = Optional.ofNullable(env.get("TOPIC_ID")).orElse("");
    private int mirrorQueryFrequency = Integer.parseInt(Optional.ofNullable(env.get("MIRROR_QUERY_FREQUENCY")).orElse("5000"));
    @SuppressWarnings("FieldMissingNullable")
    private String postgresUrl = env.get("DATABASE_URL");
    @SuppressWarnings("FieldMissingNullable")
    private final String postgresDatabase = env.get("POSTGRES_DB");
    @SuppressWarnings("FieldMissingNullable")
    private String postgresUser = env.get("POSTGRES_USER");
    @SuppressWarnings("FieldMissingNullable")
    private String postgresPassword = env.get("POSTGRES_PASSWORD");
    @SuppressWarnings("FieldMissingNullable")
    private boolean transferOnWin = Optional.ofNullable(env.get("TRANSFER_ON_WIN")).map(Boolean::parseBoolean).orElse(true);
    @SuppressWarnings("FieldMissingNullable")
    private String masterKey = Optional.ofNullable(env.get("MASTER_KEY")).orElse("");
    @SuppressWarnings("FieldMissingNullable")
    private final int refundThreads = Optional.ofNullable(env.get("REFUND_THREADS")).map(Integer::parseInt).orElse(20);
    @SuppressWarnings("FieldMissingNullable")
    private final String operatorKey = env.get("OPERATOR_KEY");
    @SuppressWarnings("FieldMissingNullable")
    private final String filesPath = Optional.ofNullable(env.get("FILES_LOCATION")).orElse("./sample-files");

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
    @Nullable
    private Refunder refunder = null;

    /**
     * Constructor
     * Sets up the Hedera client from the environment variables
     *
     * @throws Exception in the event of an exception
     */
    public App() throws Exception {
        hederaClient = new HederaClient(env);
        Path testPath = Path.of(filesPath, ".env");
        if (Files.exists(testPath)) {
            throw new Exception("FILES_LOCATION path contains a .env file");
        }
    }

    /**
     * Starts the application
     * @param args arguments for starting the application (none required)
     *
     * @throws Exception in the event of an exception
     */
    public static void main(String[] args) throws Exception {
        log.info("Starting app");
        App app = new App();
        app.runApp();
    }

    /**
     * Overrides the environment variables for testing purposes
     *
     * @param hederaClient the HederaClient to use for connecting to Hedera
     * @param restAPI whether to enable the REST api or not
     * @param adminAPIKey admin API key, if set, admin api is enabled
     * @param auctionNode whether to process auction bids or not
     * @param topicId the topicId to use
     * @param postgresUrl the details of the connection to the database
     * @param postgresUser the details of the connection to the database
     * @param postgresPassword the details of the connection to the database
     * @param transferOnWin whether to transfer the token to the winner on auction closure
     * @param masterKey the master key
     */
    public void overrideEnv(HederaClient hederaClient, boolean restAPI, String adminAPIKey, boolean auctionNode, String topicId, String postgresUrl, String postgresUser, String postgresPassword, boolean transferOnWin, String masterKey) {
        this.hederaClient = hederaClient;

        this.restAPI = restAPI;
        this.restApiVerticleCount = 1;
        this.adminAPIKey = adminAPIKey;
        this.adminApiVerticleCount = 1;
        this.auctionNode = auctionNode;

        this.topicId = topicId;
        this.mirrorQueryFrequency = 10000;
        this.postgresUrl = postgresUrl.replaceAll("jdbc:", "");
        this.postgresUser = postgresUser;
        this.postgresPassword = postgresPassword;
        this.transferOnWin = transferOnWin;
        this.masterKey = masterKey;
    }

    /**
     * Starts the various components of the application depending on environment variables
     *
     * Applies migrations to the database for seamless upgrades
     * If the client REST API is required, it is started
     * If the admin REST API is required, it is started
     *
     * If this is an auction node, open a connection to the database and initialise the necessary repositories
     * - Query the mirror node for messages on the topic id once in order to add any new auctions to the database
     * - Check for any refunds that have already been completed once
     *
     * - Start the topic subscriber thread
     * - Start the threads to watch for auction readiness
     * - Start the thread to watch for auction closure
     * - Start the threads to watch for bids on auctions
     * - Start the refund checkere thread
     * If refunding, start the refunding thread
     * If transferring tokens on auction end, start the thread to do so
     *
     *
     * @throws Exception in the event of an error
     */
    public void runApp() throws Exception {

        String apiKey = env.get("X_API_KEY");

        if (StringUtils.isEmpty(postgresUrl)) {
            String error = "DATABASE_URL environment variable is missing";
            log.error(error);
            throw new Exception(error);
        }
        if (StringUtils.isEmpty(postgresDatabase)) {
            String error = "POSTGRES_DB environment variable is missing";
            log.error(error);
            throw new Exception(error);
        }
        if (StringUtils.isEmpty(postgresUser)) {
            String error = "POSTGRES_USER environment variable is missing";
            log.error(error);
            throw new Exception(error);
        }
        if (StringUtils.isEmpty(postgresPassword)) {
            String error = "POSTGRES_PASSWORD environment variable is missing";
            log.error(error);
            throw new Exception(error);
        }

        log.info("applying database migrations");
        Flyway flyway = Flyway
                .configure()
                .dataSource("jdbc:".concat(postgresUrl).concat(postgresDatabase), postgresUser, postgresPassword)
                .locations("classpath:migrations")
                .connectRetries(20)
                .load();
        flyway.migrate();

        JsonObject config = new JsonObject()
                .put("envFile",".env")
                .put("envPath",".");

        String keyOrPass = env.get("HTTPS_KEY_OR_PASS");
        String certificate = env.get("HTTPS_CERTIFICATE");

        if ( ! StringUtils.isEmpty(keyOrPass) || ! StringUtils.isEmpty(certificate)) {
            if (StringUtils.isEmpty(certificate)) {
                String error = "HTTPS_KEY_OR_PASS provided without HTTPS_CERTIFICATE";
                log.error(error);
                throw new Exception(error);
            }

            if (StringUtils.isEmpty(keyOrPass)) {
                String error = "HTTPS_CERTIFICATE provided without HTTPS_KEY_OR_PASS";
                log.error(error);
                throw new Exception(error);
            }

            if ( ! certificate.endsWith(".jks") && ! certificate.endsWith(".pfx") && ! certificate.endsWith(".pem") && ! certificate.endsWith(".p12")) {
                String error = "HTTPS_KEY_OR_PASS should be a .jks, .pfx, .p12 or .pem file";
                log.error(error);
                throw new Exception(error);
            }

            if ( ! Files.exists(Path.of(certificate))) {
                String error = "HTTPS_CERTIFICATE file cannot be found";
                log.error(error);
                throw new Exception(error);
            }

            if (certificate.endsWith(".pem")) {
                if ( ! Files.exists(Path.of(keyOrPass))) {
                    String error = "HTTPS_KEY_OR_PASS file cannot be found";
                    log.error(error);
                    throw new Exception(error);
                }
            }

            log.info("setting up api servers to use https");
            config.put("server-key-pass", keyOrPass);
            config.put("server-certificate", certificate);
        } else {
            log.info("setting up api servers to use http");
            config.put("server-key-pass", "");
            config.put("server-certificate", "");
        }

        if (restAPI) {
            log.info("starting client REST api");
            config.put("topicId", this.topicId);
            DeploymentOptions options = new DeploymentOptions().setConfig(config).setInstances(restApiVerticleCount);
            vertx
                    .deployVerticle(ApiVerticle.class.getName(), options)
                    .onFailure(error -> {
                        log.error(error, error);
                    });
        }

        if (! StringUtils.isEmpty(adminAPIKey)) {
            log.info("starting admin REST api");
            config.put("filesPath", filesPath);
            config.put("x-api-key", apiKey);
            DeploymentOptions options = new DeploymentOptions().setConfig(config).setInstances(adminApiVerticleCount);
            vertx
                    .deployVerticle(AdminApiVerticle.class.getName(), options)
                    .onFailure(error -> {
                        log.error(error, error);
                    });
        }

        if (auctionNode) {
            SqlConnectionManager connectionManager = new SqlConnectionManager(this.postgresUrl.concat(this.postgresDatabase), this.postgresUser, this.postgresPassword);
            AuctionsRepository auctionsRepository = new AuctionsRepository(connectionManager);
            BidsRepository bidsRepository = new BidsRepository(connectionManager);
            ValidatorsRepository validatorsRepository = new ValidatorsRepository(connectionManager);

            // perform a one off check for new auctions and bids
            startSubscription(auctionsRepository, validatorsRepository, /* runOnce= */ true);
            // check for completed refunds (one off)
            startRefundChecker(auctionsRepository, bidsRepository, /* runOnce= */ true);

            // now subscribe for new events
            // subscribe to topic to get new auction notifications
            startSubscription(auctionsRepository, validatorsRepository, /* runOnce= */ false);

            startAuctionReadinessWatchers(auctionsRepository);
            startAuctionsClosureWatcher(auctionsRepository);
            startBidWatchers(auctionsRepository, /* runOnce= */ false);
            startRefunder(auctionsRepository, bidsRepository, refundThreads);
            startRefundChecker(auctionsRepository, bidsRepository, /* runOnce= */ false);
            if (transferOnWin) {
                startAuctionEndTransfers(auctionsRepository);
            }
        }
    }

    /**
     * Starts the thread to watch for auction closures
     *
     * @param auctionsRepository the repository for auctions on the database
     */
    private void startAuctionsClosureWatcher(AuctionsRepository auctionsRepository) {
        // start a thread to monitor auction closures
        auctionsClosureWatcher = new AuctionsClosureWatcher(hederaClient, auctionsRepository, mirrorQueryFrequency, transferOnWin, masterKey);
        Thread auctionsClosureWatcherThread = new Thread(auctionsClosureWatcher);
        auctionsClosureWatcherThread.start();
    }

    /**
     * Starts a subscription to a topic id, optionally once only
     *
     * @param auctionsRepository the repository for auctions on the database
     * @param validatorsRepository the repository for validators on the database
     * @param runOnce true to run the subscription once and not loop
     */
    private void startSubscription(AuctionsRepository auctionsRepository, ValidatorsRepository validatorsRepository, boolean runOnce) {
        if (StringUtils.isEmpty(topicId)) {
            log.warn("No topic Id found in environment variables, not subscribing");
        } else {
            topicSubscriber = new TopicSubscriber(hederaClient, auctionsRepository, validatorsRepository, TopicId.fromString(topicId), mirrorQueryFrequency, masterKey, runOnce);
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

    /**
     * Starts a thread for each open auction to watch for bids
     *
     * @param auctionsRepository the repository of auctions on the database
     * @param runOnce true to check for new bids only once
     * @throws SQLException in the event of a database error
     */
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

    /**
     * Starts a thread to check for refund completion
     *
     * @param auctionsRepository the repository of auctions on the database
     * @param bidsRepository the repository of bids on the database
     * @param runOnce true to check for refunds only once
     */
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

    /**
     * Starts threads to watch for auction readiness if an auction is pending
     *
     * @param auctionsRepository the repository of auctions on the database
     * @throws SQLException in the event of a database error
     */
    private void startAuctionReadinessWatchers(AuctionsRepository auctionsRepository) throws SQLException {
        for (Auction auction : auctionsRepository.getAuctionsList()) {
            if (auction.isPending()) {
                // start the thread to monitor token transfers to the auction account
                AuctionReadinessWatcher auctionReadinessWatcher = new AuctionReadinessWatcher(hederaClient, auctionsRepository, auction, mirrorQueryFrequency, /*runOnce= */ false);
                Thread t = new Thread(auctionReadinessWatcher);
                t.start();
                auctionReadinessWatchers.add(auctionReadinessWatcher);
            }
        }
    }

    /**
     * Starts a thread to monitor winning account association with a token
     *
     * @param auctionsRepository the repository of auctions on the database
     */
    private void startAuctionEndTransfers(AuctionsRepository auctionsRepository) {
        auctionEndTransfer = new AuctionEndTransfer(hederaClient, auctionsRepository, operatorKey, mirrorQueryFrequency);
        Thread auctionEndTransferThread = new Thread(auctionEndTransfer);
        auctionEndTransferThread.start();
    }

    /**
     * Starts threads to refund bids that are due for refund
     *
     * @param auctionsRepository the repository of auctions on the database
     * @param bidsRepository the repository of bids on the database
     * @param refundThreads the number of threads to run in parallel
     */
    private void startRefunder(AuctionsRepository auctionsRepository, BidsRepository bidsRepository, int refundThreads) {
        refunder = new Refunder(hederaClient, auctionsRepository, bidsRepository, mirrorQueryFrequency, refundThreads);
        Thread refunderThread = new Thread(refunder);
        refunderThread.start();
    }

    /**
     * Stops all the threads cleanly
     */
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
