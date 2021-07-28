package com.hedera.demo.auction.test.integration.subscriber;

import com.google.errorprone.annotations.Var;
import com.hedera.demo.auction.app.HederaClient;
import com.hedera.demo.auction.app.SqlConnectionManager;
import com.hedera.demo.auction.app.domain.Validator;
import com.hedera.demo.auction.app.mirrormapping.MirrorTopicMessage;
import com.hedera.demo.auction.app.mirrormapping.MirrorTopicMessages;
import com.hedera.demo.auction.app.repository.AuctionsRepository;
import com.hedera.demo.auction.app.repository.ValidatorsRepository;
import com.hedera.demo.auction.app.subscriber.TopicSubscriber;
import com.hedera.demo.auction.test.integration.AbstractIntegrationTest;
import com.hedera.hashgraph.sdk.PrivateKey;
import com.hedera.hashgraph.sdk.TopicId;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class SubscriberIntegrationTest extends AbstractIntegrationTest {

    private PostgreSQLContainer postgres;
    private AuctionsRepository auctionsRepository;
    private ValidatorsRepository validatorsRepository;
    private final HederaClient hederaClient = new HederaClient();
    private final static String tokenId = "0.0.10";
    private final static String auctionAccountId = "0.0.20";
    private final static TopicId topicId = TopicId.fromString("0.0.1");

    private TopicSubscriber topicSubscriber;
    private Instant consensusTimestamp;
    private String publicKey;
    private String publicKey2;

    private MirrorTopicMessages mirrorTopicMessages = new MirrorTopicMessages();

    public SubscriberIntegrationTest() throws Exception {
    }

    @BeforeAll
    public void beforeAll() {
        this.postgres = new PostgreSQLContainer("postgres:12.6");
        this.postgres.start();
        migrate(this.postgres);
        SqlConnectionManager connectionManager = new SqlConnectionManager(this.postgres.getJdbcUrl(), this.postgres.getUsername(), this.postgres.getPassword());
        auctionsRepository = new AuctionsRepository(connectionManager);
        validatorsRepository = new ValidatorsRepository(connectionManager);

        topicSubscriber = new TopicSubscriber(hederaClient, auctionsRepository, validatorsRepository, topicId, 5000, masterKey, /*runOnce= */ false);
        topicSubscriber.setSkipReadinessWatcher();

        consensusTimestamp = Instant.now();
    }

    @AfterAll
    public void afterAll() {
        this.postgres.close();
    }

    @BeforeEach
    public void beforeEach() {
        JsonObject auctionJson = new JsonObject();
        auctionJson.put("endtimestamp", "");
        auctionJson.put("tokenid", tokenId);
        auctionJson.put("auctionaccountid", auctionAccountId);
        auctionJson.put("reserve", 100);
        auctionJson.put("winnercanbid", true);
        auctionJson.put("title", "auction title");
        auctionJson.put("description", "auction description");

        @Var PrivateKey privateKey = PrivateKey.generate();
        publicKey = privateKey.getPublicKey().toString();
        privateKey = PrivateKey.generate();
        publicKey2 = privateKey.getPublicKey().toString();
    }

    @AfterEach
    public void afterEach() throws SQLException {
        topicSubscriber.stop();
        auctionsRepository.deleteAllAuctions();
        validatorsRepository.deleteAllValidators();
    }

//    @Test
//    public void testAuctionNoEndTimestamp() throws Exception {
//
//        createTopicMessage(auctionJson);
//
//        topicSubscriber.handle(mirrorTopicMessages);
//
//        List<Auction> auctions = auctionsRepository.getAuctionsList();
//
//        assertEquals(1, auctions.size());
//        consensusTimestamp = consensusTimestamp.plus(2, ChronoUnit.DAYS);
//        assertEquals(consensusTimeStampWithNanos(), auctions.get(0).getEndtimestamp());
//        assertEquals(0, auctions.get(0).getWinningbid());
//        assertEquals(tokenId, auctions.get(0).getTokenid());
//        assertEquals(auctionAccountId, auctions.get(0).getAuctionaccountid());
//        assertEquals(100, auctions.get(0).getReserve());
//        assertTrue(auctions.get(0).getWinnerCanBid());
//        assertEquals("auction title", auctions.get(0).getTitle());
//        assertEquals("auction description", auctions.get(0).getDescription());
//
//    }

//    @Test
//    public void testAuctionWithEndTimestamp() throws Exception {
//
//        consensusTimestamp = consensusTimestamp.plus(5, ChronoUnit.DAYS);
//        auctionJson.put("endtimestamp", String.valueOf(consensusTimestamp.getEpochSecond()));
//        createTopicMessage(auctionJson);
//
//        topicSubscriber.handle(mirrorTopicMessages);
//
//        List<Auction> auctions = auctionsRepository.getAuctionsList();
//
//        assertEquals(1, auctions.size());
//        assertEquals(consensusTimeStampWithNanos(), auctions.get(0).getEndtimestamp());
//        assertEquals(0, auctions.get(0).getWinningbid());
//        assertEquals(tokenId, auctions.get(0).getTokenid());
//        assertEquals(auctionAccountId, auctions.get(0).getAuctionaccountid());
//        assertEquals(100, auctions.get(0).getReserve());
//        assertTrue(auctions.get(0).getWinnerCanBid());
//    }

//    @Test
//    public void testWinnerCantBid() throws Exception {
//
//        auctionJson.put("winnercanbid", false);
//        createTopicMessage(auctionJson);
//
//        topicSubscriber.handle(mirrorTopicMessages);
//
//        List<Auction> auctions = auctionsRepository.getAuctionsList();
//
//        assertEquals(1, auctions.size());
//        consensusTimestamp = consensusTimestamp.plus(2, ChronoUnit.DAYS);
//        assertEquals(consensusTimeStampWithNanos(), auctions.get(0).getEndtimestamp());
//        assertEquals(0, auctions.get(0).getWinningbid());
//        assertEquals(tokenId, auctions.get(0).getTokenid());
//        assertEquals(auctionAccountId, auctions.get(0).getAuctionaccountid());
//        assertEquals(100, auctions.get(0).getReserve());
//        assertFalse(auctions.get(0).getWinnerCanBid());
//    }

//    @Test
//    public void testDefaults() throws Exception {
//
//        auctionJson.remove("endtimestamp");
//        auctionJson.remove("reserve");
//        auctionJson.remove("winnercanbid");
//        createTopicMessage(auctionJson);
//
//        topicSubscriber.handle(mirrorTopicMessages);
//
//        List<Auction> auctions = auctionsRepository.getAuctionsList();
//
//        assertEquals(1, auctions.size());
//        consensusTimestamp = consensusTimestamp.plus(2, ChronoUnit.DAYS);
//        assertEquals(consensusTimeStampWithNanos(), auctions.get(0).getEndtimestamp());
//        assertEquals(0, auctions.get(0).getWinningbid());
//        assertEquals(tokenId, auctions.get(0).getTokenid());
//        assertEquals(auctionAccountId, auctions.get(0).getAuctionaccountid());
//        assertEquals(0, auctions.get(0).getReserve());
//        assertFalse(auctions.get(0).getWinnerCanBid());
//    }

    @Test
    public void testAddValidator() throws SQLException {
        JsonObject validatorJson = new JsonObject();
        JsonArray validators = new JsonArray();
        JsonObject validator = new JsonObject();
        validator.put("name", "validatorName");
        validator.put("url", "https://hedera.com");
        validator.put("publicKey", publicKey);
        validator.put("operation", "add");
        validators.add(validator);
        validatorJson.put("validators", validators);

        createTopicMessage(validatorJson);

        topicSubscriber.handle(mirrorTopicMessages);

        List<Validator> dbValidators = validatorsRepository.getValidatorsList();

        assertEquals(1, dbValidators.size());
        assertEquals("validatorName", dbValidators.get(0).getName());
        assertEquals("https://hedera.com", dbValidators.get(0).getUrl());
        assertEquals(publicKey, dbValidators.get(0).getPublicKey());
    }

    @Test
    public void testAddValidators() throws SQLException {
        JsonObject validatorJson = new JsonObject();
        JsonArray validators = new JsonArray();
        JsonObject validator = new JsonObject();
        validator.put("name", "validatorName");
        validator.put("url", "https://hedera.com");
        validator.put("publicKey", publicKey);
        validator.put("operation", "add");
        validators.add(validator);

        JsonObject validator2 = new JsonObject();
        validator2.put("name", "validatorName2");
        validator2.put("url", "https://hedera2.com");
        validator2.put("publicKey", publicKey2);
        validator2.put("operation", "add");
        validators.add(validator2);

        validatorJson.put("validators", validators);

        createTopicMessage(validatorJson);

        topicSubscriber.handle(mirrorTopicMessages);

        List<Validator> dbValidators = validatorsRepository.getValidatorsList();

        assertEquals(2, dbValidators.size());
        assertEquals("validatorName", dbValidators.get(0).getName());
        assertEquals("https://hedera.com", dbValidators.get(0).getUrl());
        assertEquals(publicKey, dbValidators.get(0).getPublicKey());

        assertEquals("validatorName2", dbValidators.get(1).getName());
        assertEquals("https://hedera2.com", dbValidators.get(1).getUrl());
        assertEquals(publicKey2, dbValidators.get(1).getPublicKey());
    }

    @Test
    public void testDeleteValidator() throws SQLException {
        JsonObject validatorJson = new JsonObject();
        JsonArray validators = new JsonArray();
        JsonObject validator = new JsonObject();
        validator.put("name", "validatorName");
        validator.put("url", "https://hedera.com");
        validator.put("publicKey", publicKey);
        validator.put("operation", "add");
        validators.add(validator);
        validatorJson.put("validators", validators);

        createTopicMessage(validatorJson);

        topicSubscriber.handle(mirrorTopicMessages);

        JsonObject deleteValidatorJson = new JsonObject();
        JsonArray deleteValidators = new JsonArray();
        JsonObject deleteValidator = new JsonObject();
        deleteValidator.put("name", "validatorName");
        deleteValidator.put("operation", "delete");
        deleteValidators.add(deleteValidator);
        deleteValidatorJson.put("validators", deleteValidators);

        createTopicMessage(deleteValidatorJson);

        topicSubscriber.handle(mirrorTopicMessages);

        List<Validator> dbValidators = validatorsRepository.getValidatorsList();

        assertEquals(0, dbValidators.size());
    }

    @Test
    public void testUpdateValidator() throws SQLException {
        JsonObject validatorJson = new JsonObject();
        JsonArray validators = new JsonArray();
        JsonObject validator = new JsonObject();
        validator.put("name", "validatorName");
        validator.put("url", "https://hedera.com");
        validator.put("publicKey", publicKey);
        validator.put("operation", "add");
        validators.add(validator);
        validatorJson.put("validators", validators);

        createTopicMessage(validatorJson);

        topicSubscriber.handle(mirrorTopicMessages);

        JsonObject updateValidatorJson = new JsonObject();
        JsonArray updateValidators = new JsonArray();
        JsonObject updateValidator = new JsonObject();
        updateValidator.put("nameToUpdate", "validatorName");
        updateValidator.put("name", "validatorName2");
        updateValidator.put("url", "https://hedera2.com");
        updateValidator.put("publicKey", publicKey2);
        updateValidator.put("operation", "update");

        updateValidators.add(updateValidator);
        updateValidatorJson.put("validators", updateValidators);

        createTopicMessage(updateValidatorJson);

        topicSubscriber.handle(mirrorTopicMessages);

        List<Validator> dbValidators = validatorsRepository.getValidatorsList();

        assertEquals(1, dbValidators.size());

        assertEquals("validatorName2", dbValidators.get(0).getName());
        assertEquals("https://hedera2.com", dbValidators.get(0).getUrl());
        assertEquals(publicKey2, dbValidators.get(0).getPublicKey());
    }

    @Test
    public void testInvalidValidatorOperation() throws SQLException {
        // invalid operation
        JsonObject validatorJson = new JsonObject();
        JsonArray validators = new JsonArray();
        JsonObject validator = new JsonObject();
        validator.put("name", "validatorName");
        validator.put("url", "https://hedera.com");
        validator.put("publicKey", publicKey);
        validator.put("operation", "invalid");
        validators.add(validator);
        validatorJson.put("validators", validators);

        createTopicMessage(validatorJson);
        topicSubscriber.handle(mirrorTopicMessages);
        List<Validator> dbValidators = validatorsRepository.getValidatorsList();
        assertEquals(0, dbValidators.size());
    }
    @Test
    public void testInvalidValidatorAdd() throws SQLException {
        // add empty name
        JsonObject validatorJson = new JsonObject();
        JsonArray validators = new JsonArray();
        JsonObject validator = new JsonObject();
        validator.put("name", "");
        validator.put("url", "https://hedera.com");
        validator.put("publicKey", publicKey);
        validator.put("operation", "add");
        validators.add(validator);
        validatorJson.put("validators", validators);

        createTopicMessage(validatorJson);
        topicSubscriber.handle(mirrorTopicMessages);
        List<Validator> dbValidators = validatorsRepository.getValidatorsList();
        assertEquals(0, dbValidators.size());
    }
    @Test
    public void testInvalidValidatorDelete() throws SQLException {
        JsonObject validatorJson = new JsonObject();
        JsonArray validators = new JsonArray();
        JsonObject validator = new JsonObject();
        validator.put("name", "validatorName");
        validator.put("url", "https://hedera.com");
        validator.put("operation", "add");
        validators.add(validator);
        validatorJson.put("validators", validators);

        createTopicMessage(validatorJson);

        topicSubscriber.handle(mirrorTopicMessages);

        JsonObject deleteValidatorJson = new JsonObject();
        JsonArray deleteValidators = new JsonArray();
        JsonObject deleteValidator = new JsonObject();
        deleteValidator.put("name", "");
        deleteValidator.put("operation", "delete");
        deleteValidators.add(deleteValidator);
        deleteValidatorJson.put("validators", deleteValidators);

        createTopicMessage(deleteValidatorJson);

        topicSubscriber.handle(mirrorTopicMessages);

        List<Validator> dbValidators = validatorsRepository.getValidatorsList();

        assertEquals(1, dbValidators.size());
    }

    @Test
    public void testInvalidValidatorUpdateEmptyName() throws SQLException {
        JsonObject validatorJson = new JsonObject();
        JsonArray validators = new JsonArray();
        JsonObject validator = new JsonObject();
        validator.put("name", "validatorName");
        validator.put("url", "https://hedera.com");
        validator.put("publicKey", publicKey);
        validator.put("operation", "add");
        validators.add(validator);
        validatorJson.put("validators", validators);

        createTopicMessage(validatorJson);

        topicSubscriber.handle(mirrorTopicMessages);

        JsonObject deleteValidatorJson = new JsonObject();
        JsonArray deleteValidators = new JsonArray();
        JsonObject deleteValidator = new JsonObject();
        deleteValidator.put("nameToUpdate", "validatorName");
        deleteValidator.put("name", "");
        deleteValidator.put("operation", "update");
        deleteValidators.add(deleteValidator);
        deleteValidatorJson.put("validators", deleteValidators);

        createTopicMessage(deleteValidatorJson);

        topicSubscriber.handle(mirrorTopicMessages);

        List<Validator> dbValidators = validatorsRepository.getValidatorsList();

        assertEquals(1, dbValidators.size());
        assertEquals("validatorName", dbValidators.get(0).getName());
        assertEquals("https://hedera.com", dbValidators.get(0).getUrl());
        assertEquals(publicKey, dbValidators.get(0).getPublicKey());
    }

    @Test
    public void testInvalidValidatorUpdateEmptyNameToUpdate() throws SQLException {
        JsonObject validatorJson = new JsonObject();
        JsonArray validators = new JsonArray();
        JsonObject validator = new JsonObject();
        validator.put("name", "validatorName");
        validator.put("url", "https://hedera.com");
        validator.put("publicKey", publicKey);
        validator.put("operation", "add");
        validators.add(validator);
        validatorJson.put("validators", validators);

        createTopicMessage(validatorJson);

        topicSubscriber.handle(mirrorTopicMessages);

        JsonObject deleteValidatorJson = new JsonObject();
        JsonArray deleteValidators = new JsonArray();
        JsonObject deleteValidator = new JsonObject();
        deleteValidator.put("nameToUpdate", "");
        deleteValidator.put("name", "validatorName2");
        deleteValidator.put("operation", "update");
        deleteValidators.add(deleteValidator);
        deleteValidatorJson.put("validators", deleteValidators);

        createTopicMessage(deleteValidatorJson);

        topicSubscriber.handle(mirrorTopicMessages);

        List<Validator> dbValidators = validatorsRepository.getValidatorsList();

        assertEquals(1, dbValidators.size());
        assertEquals("validatorName", dbValidators.get(0).getName());
        assertEquals("https://hedera.com", dbValidators.get(0).getUrl());
        assertEquals(publicKey, dbValidators.get(0).getPublicKey());
    }

    @Test
    public void testInvalidOperation() throws SQLException {
        JsonObject validatorJson = new JsonObject();
        JsonArray validators = new JsonArray();
        JsonObject validator = new JsonObject();
        validator.put("name", "validatorName");
        validator.put("url", "https://hedera.com");
        validator.put("publicKey", publicKey);
        validator.put("operation", "testing");
        validators.add(validator);
        validatorJson.put("validators", validators);

        createTopicMessage(validatorJson);

        topicSubscriber.handle(mirrorTopicMessages);

        List<Validator> dbValidators = validatorsRepository.getValidatorsList();

        assertEquals(0, dbValidators.size());
    }

    @Test
    public void testInvalidURL() throws SQLException {
        JsonObject validatorJson = new JsonObject();
        JsonArray validators = new JsonArray();
        JsonObject validator = new JsonObject();
        validator.put("name", "validatorName");
        validator.put("url", "not a valid url");
        validator.put("publicKey", publicKey);
        validator.put("operation", "add");
        validators.add(validator);
        validatorJson.put("validators", validators);

        createTopicMessage(validatorJson);

        topicSubscriber.handle(mirrorTopicMessages);

        List<Validator> dbValidators = validatorsRepository.getValidatorsList();

        assertEquals(0, dbValidators.size());
    }

    private void createTopicMessage(JsonObject messageJson) {

        byte[] contents = messageJson.toString().getBytes(StandardCharsets.UTF_8);
        String base64Contents = Base64.getEncoder().encodeToString(contents);
        JsonObject topicMessage = new JsonObject();
        @Var String timeStamp = String.valueOf(consensusTimestamp.getEpochSecond());
        timeStamp = timeStamp.concat(".");
        timeStamp = timeStamp.concat(String.valueOf(consensusTimestamp.getNano()));

        topicMessage.put("consensus_timestamp", timeStamp);
        topicMessage.put("message", base64Contents);

        MirrorTopicMessage mirrorTopicMessage = topicMessage.mapTo(MirrorTopicMessage.class);
        mirrorTopicMessages = new MirrorTopicMessages();
        mirrorTopicMessages.messages.add(mirrorTopicMessage);
    }
}
