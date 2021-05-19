package com.hedera.demo.auction.node.test.system.e2eApp;

import com.hedera.demo.auction.node.app.App;
import com.hedera.demo.auction.node.app.SqlConnectionManager;
import com.hedera.demo.auction.node.app.repository.AuctionsRepository;
import com.hedera.demo.auction.node.app.repository.BidsRepository;
import com.hedera.demo.auction.node.test.system.AbstractAPITester;
import com.hedera.hashgraph.sdk.TopicId;
import io.vertx.core.Vertx;
import io.vertx.ext.web.client.WebClient;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testcontainers.containers.PostgreSQLContainer;

@ExtendWith({VertxExtension.class})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class AdminAPISystemTest extends AbstractAPITester {

    Vertx vertx;
    App app = new App();

    AdminAPISystemTest() throws Exception {
        super();
    }

    @BeforeAll
    public void beforeAll() {
        postgres = (PostgreSQLContainer) new PostgreSQLContainer("postgres:12.6").withNetworkAliases("pgdb").withNetwork(testContainersNetwork);
        postgres.start();
        migrate(postgres);
        SqlConnectionManager connectionManager = new SqlConnectionManager(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
        auctionsRepository = new AuctionsRepository(connectionManager);
        bidsRepository = new BidsRepository(connectionManager);

        this.vertx = Vertx.vertx();
        this.webClient = WebClient.create(this.vertx);
    }

    @AfterAll
    public void afterAll() {
        this.postgres.close();
    }

    @BeforeEach
    public void beforeEach() throws Exception {
        bidsRepository.deleteAllBids();
        auctionsRepository.deleteAllAuctions();
        TopicId topicId = createTopic.create();
        hederaClient.setMirrorProvider("hedera");
        hederaClient.setClientMirror();

        app.overrideEnv(hederaClient, /*restAPI= */ true, /*adminAPI= */true, /*auctionNode= */ true, topicId.toString(), hederaClient.operatorPrivateKey().toString(), postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword(), /*transferOnWin= */true, masterKey);
        app.runApp();
        Thread.sleep(5000);
    }

    @AfterEach
    public void afterEach() throws InterruptedException {
        app.stop();
        // allow 5s for app to close
        Thread.sleep(5000);
    }

    @Test
    public void testAdminRestAPITopic(VertxTestContext testContext) {
        adminRestAPITopic(testContext, "localhost");
    }
    @Test
    public void testAdminRestAPIToken(VertxTestContext testContext) {
        adminRestAPIToken(testContext, "localhost");
    }

    @Test
    public void testAdminRestAPIAuctionAccount(VertxTestContext testContext) {
        adminRestAPIAuctionAccount(testContext, "localhost");
    }

    @Test
    public void testAdminRestAPITransferToken(VertxTestContext testContext) throws Exception {
        adminRestAPITransferToken(testContext, "localhost");
    }

    @Test
    public void testAdminRestAPIAuction(VertxTestContext testContext) throws Exception {
        adminRestAPIAuction(testContext, "localhost");
    }
}
