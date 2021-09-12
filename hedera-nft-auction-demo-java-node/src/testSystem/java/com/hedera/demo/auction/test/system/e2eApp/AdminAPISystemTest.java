package com.hedera.demo.auction.test.system.e2eApp;

import com.google.errorprone.annotations.Var;
import com.hedera.demo.auction.app.App;
import com.hedera.demo.auction.app.ManageValidator;
import com.hedera.demo.auction.app.SqlConnectionManager;
import com.hedera.demo.auction.app.repository.AuctionsRepository;
import com.hedera.demo.auction.app.repository.BidsRepository;
import com.hedera.demo.auction.app.repository.ValidatorsRepository;
import com.hedera.demo.auction.test.system.AbstractAPITester;
import com.hedera.hashgraph.sdk.PrivateKey;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
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

    App app = new App();
    String publicKey;
    String publicKey2;

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

        app.overrideEnv(hederaClient, /*restAPI= */ true, "adminapikey", /*auctionNode= */ true, topicId.toString(), postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword(), /*transferOnWin= */true, masterKey.toString());
        app.runApp();
        Thread.sleep(5000);

        @Var PrivateKey privateKey = PrivateKey.generate();
        publicKey = privateKey.getPublicKey().toString();
        privateKey = PrivateKey.generate();
        publicKey2 = privateKey.getPublicKey().toString();
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
        validator.put("url", "https://hedera.com");
        validator.put("publicKey", publicKey);

        validators.add(validator);
        validatorJson.put("validators", validators);

        validatorAPICall(testContext, "localhost", validatorJson);

        await()
            .with()
            .pollInterval(Duration.ofSeconds(5))
            .await()
            .atMost(Duration.ofSeconds(30))
            .until(validatorAssert("validatorName", "https://hedera.com",publicKey));

    }

    @Test
    public void testValidatorUpdate(VertxTestContext testContext) throws Exception {

        String publicKey1 = PrivateKey.generate().getPublicKey().toString();
        String publicKey2 = PrivateKey.generate().getPublicKey().toString();

        JsonArray validatorsJson = new JsonArray();
        @Var JsonObject validatorJson = new JsonObject();
        validatorJson.put("name", "validatorName1");
        validatorJson.put("url", "https://hedera1.com");
        validatorJson.put("publicKey", publicKey1);
        validatorJson.put("operation", "add");
        validatorsJson.add(validatorJson);

        validatorsRepository.manage(validatorsJson);

        validatorJson = new JsonObject();
        JsonArray validators = new JsonArray();

        JsonObject updateValidator = new JsonObject();
        updateValidator.put("operation", "update");
        updateValidator.put("nameToUpdate", "validatorName1");
        updateValidator.put("name", "validatorName2");
        updateValidator.put("url", "https://hedera2.com");
        updateValidator.put("publicKey", publicKey2);

        validators.add(updateValidator);

        validatorJson.put("validators", validators);

        validatorAPICall(testContext, "localhost", validatorJson);

        await()
                .with()
                .pollInterval(Duration.ofSeconds(5))
                .await()
                .atMost(Duration.ofSeconds(30))
                .until(validatorAssert("validatorName2", "https://hedera2.com",publicKey2));

    }

    @Test
    public void testValidatorDelete(VertxTestContext testContext) throws Exception {

        String publicKey1 = PrivateKey.generate().getPublicKey().toString();

        JsonArray validatorsJson = new JsonArray();
        @Var JsonObject validatorJson = new JsonObject();
        validatorJson.put("name", "validatorName1");
        validatorJson.put("url", "https://hedera1.com");
        validatorJson.put("publicKey", publicKey1);
        validatorJson.put("operation", "add");
        validatorsJson.add(validatorJson);

        validatorsRepository.manage(validatorsJson);

        validatorJson = new JsonObject();
        JsonArray validators = new JsonArray();

        JsonObject deleteValidator = new JsonObject();
        deleteValidator.put("operation", "delete");
        deleteValidator.put("name", "validatorName1");

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
