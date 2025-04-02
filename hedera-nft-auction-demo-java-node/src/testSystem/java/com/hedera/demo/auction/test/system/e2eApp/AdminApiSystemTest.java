package com.hedera.demo.auction.test.system.e2eApp;

import com.google.errorprone.annotations.Var;
import com.hedera.demo.auction.app.App;
import com.hedera.demo.auction.app.ManageValidator;
import com.hedera.demo.auction.app.SqlConnectionManager;
import com.hedera.demo.auction.app.repository.AuctionsRepository;
import com.hedera.demo.auction.app.repository.BidsRepository;
import com.hedera.demo.auction.app.repository.ValidatorsRepository;
import com.hedera.demo.auction.test.system.AbstractApiTester;
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
import java.util.List;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith({VertxExtension.class})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class AdminApiSystemTest extends AbstractApiTester {

    App app = new App();
    String publicKey;
    String publicKey2;

    AdminApiSystemTest() throws Exception {
        super();
    }

    @BeforeAll
    public void beforeAll() {
        postgres = new PostgreSQLContainer<>("POSTGRES_CONTAINER_VERSION");
        postgres.setNetworkAliases(List.of("pgdb"));
        postgres.setNetwork(testContainersNetwork);
        postgres.start();
        migrate(postgres);
        var connectionManager = new SqlConnectionManager(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
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

        assertNotNull(topicId);
        new ManageValidator().setTopicId(topicId.toString());

        app.setRestApi(true);
        app.setHederaClient(hederaClient);
        app.setAdminApiKey("adminapikey");
        app.setAuctionNode(true);
        app.setTopicId(topicId.toString());
        app.setPostgresUrl(postgres.getJdbcUrl());
        app.setPostgresUser(postgres.getUsername());
        app.setPostgresUrl(postgres.getPassword());
        app.setTransferOnWin(true);
        app.setMasterKey(masterKey.toString());
        app.runApp();
        Thread.sleep(Duration.ofSeconds(5));

        @Var PrivateKey privateKey = PrivateKey.generateED25519();
        publicKey = privateKey.getPublicKey().toString();
        privateKey = PrivateKey.generateED25519();
        publicKey2 = privateKey.getPublicKey().toString();
    }

    @AfterEach
    public void afterEach() throws InterruptedException {
        app.stop();
        // allow 5s for app to close
        Thread.sleep(Duration.ofSeconds(5));
    }

    @Test
    public void testAdminRestAPITopic(VertxTestContext testContext) {
        adminRestApiTopic(testContext, "localhost");
    }
    @Test
    public void testAdminRestAPIToken(VertxTestContext testContext) {
        adminRestApiToken(testContext, "localhost");
    }

    @Test
    public void testAdminRestAPIAuctionAccount(VertxTestContext testContext) {
        adminRestApiAuctionAccount(testContext, "localhost");
    }

    @Test
    public void testAdminRestAPITransferToken(VertxTestContext testContext) throws Exception {
        adminRestApiTransferToken(testContext, "localhost");
    }

    @Test
    public void testAdminRestAPIAuction(VertxTestContext testContext) throws Exception {
        adminRestApiAuction(testContext, "localhost");
    }

    @Test
    public void testValidatorCreate(VertxTestContext testContext) throws Exception {
        var validatorJson = new JsonObject();
        var validators = new JsonArray();

        var validator = new JsonObject();
        validator.put("operation", "add");
        validator.put("name", "validatorName");
        validator.put("url", "https://hedera.com");
        validator.put("publicKey", publicKey);

        validators.add(validator);
        validatorJson.put("validators", validators);

        validatorApiCall(testContext, "localhost", validatorJson);

        await()
            .with()
            .pollInterval(Duration.ofSeconds(5))
            .await()
            .atMost(Duration.ofSeconds(30))
            .until(validatorAssert("validatorName", "https://hedera.com",publicKey));

    }

    @Test
    public void testValidatorUpdate(VertxTestContext testContext) throws Exception {

        String publicKey1 = PrivateKey.generateED25519().getPublicKey().toString();
        String publicKey2 = PrivateKey.generateED25519().getPublicKey().toString();

        var validatorsJson = new JsonArray();
        @Var JsonObject validatorJson = new JsonObject();
        validatorJson.put("name", "validatorName1");
        validatorJson.put("url", "https://hedera1.com");
        validatorJson.put("publicKey", publicKey1);
        validatorJson.put("operation", "add");
        validatorsJson.add(validatorJson);

        validatorsRepository.manage(validatorsJson);

        validatorJson = new JsonObject();
        var validators = new JsonArray();

        var updateValidator = new JsonObject();
        updateValidator.put("operation", "update");
        updateValidator.put("nameToUpdate", "validatorName1");
        updateValidator.put("name", "validatorName2");
        updateValidator.put("url", "https://hedera2.com");
        updateValidator.put("publicKey", publicKey2);

        validators.add(updateValidator);

        validatorJson.put("validators", validators);

        validatorApiCall(testContext, "localhost", validatorJson);

        await()
                .with()
                .pollInterval(Duration.ofSeconds(5))
                .await()
                .atMost(Duration.ofSeconds(30))
                .until(validatorAssert("validatorName2", "https://hedera2.com",publicKey2));

    }

    @Test
    public void testValidatorDelete(VertxTestContext testContext) throws Exception {

        String publicKey1 = PrivateKey.generateED25519().getPublicKey().toString();

        var validatorsJson = new JsonArray();
        @Var JsonObject validatorJson = new JsonObject();
        validatorJson.put("name", "validatorName1");
        validatorJson.put("url", "https://hedera1.com");
        validatorJson.put("publicKey", publicKey1);
        validatorJson.put("operation", "add");
        validatorsJson.add(validatorJson);

        validatorsRepository.manage(validatorsJson);

        validatorJson = new JsonObject();
        var validators = new JsonArray();

        var deleteValidator = new JsonObject();
        deleteValidator.put("operation", "delete");
        deleteValidator.put("name", "validatorName1");

        validators.add(deleteValidator);

        validatorJson.put("validators", validators);

        validatorApiCall(testContext, "localhost", validatorJson);

        await()
                .with()
                .pollInterval(Duration.ofSeconds(5))
                .await()
                .atMost(Duration.ofSeconds(30))
                .until(validatorsAssertCount());

    }
}
