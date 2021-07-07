package com.hedera.demo.auction.test.system.e2eApp;

import com.hedera.demo.auction.app.App;
import com.hedera.demo.auction.app.ManageValidator;
import com.hedera.demo.auction.app.SqlConnectionManager;
import com.hedera.demo.auction.app.repository.AuctionsRepository;
import com.hedera.demo.auction.app.repository.BidsRepository;
import com.hedera.demo.auction.app.repository.ValidatorsRepository;
import com.hedera.demo.auction.test.system.AbstractAPITester;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
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

import java.time.Duration;

import static org.awaitility.Awaitility.await;

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
        validatorsRepository = new ValidatorsRepository(connectionManager);

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
        validatorsRepository.deleteAllValidators();
        topicId = createTopic.create();
        hederaClient.setMirrorProvider("hedera");

        new ManageValidator().setTopicId(topicId.toString());

        app.overrideEnv(hederaClient, /*restAPI= */ true, /*adminAPI= */true, /*auctionNode= */ true, topicId.toString(), postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword(), /*transferOnWin= */true, masterKey.toString());
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

    @Test
    public void testValidatorCreate(VertxTestContext testContext) throws Exception {
        JsonObject validatorJson = new JsonObject();
        JsonArray validators = new JsonArray();

        JsonObject validator = new JsonObject();
        validator.put("operation", "add");
        validator.put("name", "validatorName");
        validator.put("url", "validatorUrl");
        validator.put("publicKey", "validatorPublicKey");

        validators.add(validator);
        validatorJson.put("validators", validators);

        validatorAPICall(testContext, "localhost", validatorJson);

        await()
            .with()
            .pollInterval(Duration.ofSeconds(5))
            .await()
            .atMost(Duration.ofSeconds(30))
            .until(validatorAssert("validatorName", "validatorUrl","validatorPublicKey"));

    }

    @Test
    public void testValidatorUpdate(VertxTestContext testContext) throws Exception {

        // create a validator directly in the database
        validatorsRepository.add("validatorName", "validatorUrl", "validatorPublicKey");

        JsonObject validatorJson = new JsonObject();
        JsonArray validators = new JsonArray();

        JsonObject updateValidator = new JsonObject();
        updateValidator.put("operation", "update");
        updateValidator.put("nameToUpdate", "validatorName");
        updateValidator.put("name", "validatorName2");
        updateValidator.put("url", "validatorUrl2");
        updateValidator.put("publicKey", "validatorPublicKey2");

        validators.add(updateValidator);

        validatorJson.put("validators", validators);

        validatorAPICall(testContext, "localhost", validatorJson);

        await()
                .with()
                .pollInterval(Duration.ofSeconds(5))
                .await()
                .atMost(Duration.ofSeconds(30))
                .until(validatorAssert("validatorName2", "validatorUrl2","validatorPublicKey2"));

    }

    @Test
    public void testValidatorDelete(VertxTestContext testContext) throws Exception {

        // create a validator directly in the database
        validatorsRepository.add("validatorName", "validatorUrl", "validatorPublicKey");

        JsonObject validatorJson = new JsonObject();
        JsonArray validators = new JsonArray();

        JsonObject deleteValidator = new JsonObject();
        deleteValidator.put("operation", "delete");
        deleteValidator.put("name", "validatorName");

        validators.add(deleteValidator);

        validatorJson.put("validators", validators);

        validatorAPICall(testContext, "localhost", validatorJson);

        await()
                .with()
                .pollInterval(Duration.ofSeconds(5))
                .await()
                .atMost(Duration.ofSeconds(30))
                .until(validatorsAssertCount(0));

    }
}
