package com.hedera.demo.auction.test.integration.subscriber;

import com.google.errorprone.annotations.Var;
import com.hedera.demo.auction.app.HederaClient;
import com.hedera.demo.auction.app.SqlConnectionManager;
import com.hedera.demo.auction.app.domain.Auction;
import com.hedera.demo.auction.app.mirrormapping.MirrorTopicMessage;
import com.hedera.demo.auction.app.mirrormapping.MirrorTopicMessages;
import com.hedera.demo.auction.app.repository.AuctionsRepository;
import com.hedera.demo.auction.app.subscriber.TopicSubscriber;
import com.hedera.demo.auction.test.integration.AbstractIntegrationTest;
import com.hedera.hashgraph.sdk.TopicId;
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
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class SubscriberIntegrationTest extends AbstractIntegrationTest {

    private PostgreSQLContainer postgres;
    private AuctionsRepository auctionsRepository;
    private final HederaClient hederaClient = HederaClient.emptyTestClient();
    private final static String tokenId = "0.0.10";
    private final static String auctionAccountId = "0.0.20";
    private final static TopicId topicId = TopicId.fromString("0.0.1");

    private JsonObject auctionJson;
    private TopicSubscriber topicSubscriber;
    private Instant consensusTimestamp;

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

        topicSubscriber = new TopicSubscriber(hederaClient, auctionsRepository, topicId, 5000, masterKey);
        topicSubscriber.setSkipReadinessWatcher();
        topicSubscriber.setTesting();

        consensusTimestamp = Instant.now();
    }

    @AfterAll
    public void afterAll() {
        this.postgres.close();
    }

    @BeforeEach
    public void beforeEach() {
        auctionJson = new JsonObject();
        auctionJson.put("endtimestamp", "");
        auctionJson.put("tokenid", tokenId);
        auctionJson.put("auctionaccountid", auctionAccountId);
        auctionJson.put("reserve", 100);
        auctionJson.put("winnercanbid", true);
        auctionJson.put("title", "auction title");
        auctionJson.put("description", "auction description");
    }

    @AfterEach
    public void afterEach() throws SQLException {
        auctionsRepository.deleteAllAuctions();
    }

    @Test
    public void testAuctionNoEndTimestamp() throws Exception {

        createTopicMessage();

        topicSubscriber.handle(mirrorTopicMessages);

        List<Auction> auctions = auctionsRepository.getAuctionsList();

        assertEquals(1, auctions.size());
        consensusTimestamp = consensusTimestamp.plus(2, ChronoUnit.DAYS);
        assertEquals(consensusTimeStampWithNanos(), auctions.get(0).getEndtimestamp());
        assertEquals(0, auctions.get(0).getWinningbid());
        assertEquals(tokenId, auctions.get(0).getTokenid());
        assertEquals(auctionAccountId, auctions.get(0).getAuctionaccountid());
        assertEquals(100, auctions.get(0).getReserve());
        assertTrue(auctions.get(0).getWinnerCanBid());
        assertEquals("auction title", auctions.get(0).getTitle());
        assertEquals("auction description", auctions.get(0).getDescription());

    }

    @Test
    public void testAuctionWithEndTimestamp() throws Exception {

        consensusTimestamp = consensusTimestamp.plus(5, ChronoUnit.DAYS);
        auctionJson.put("endtimestamp", String.valueOf(consensusTimestamp.getEpochSecond()));
        createTopicMessage();

        topicSubscriber.handle(mirrorTopicMessages);

        List<Auction> auctions = auctionsRepository.getAuctionsList();

        assertEquals(1, auctions.size());
        assertEquals(consensusTimeStampWithNanos(), auctions.get(0).getEndtimestamp());
        assertEquals(0, auctions.get(0).getWinningbid());
        assertEquals(tokenId, auctions.get(0).getTokenid());
        assertEquals(auctionAccountId, auctions.get(0).getAuctionaccountid());
        assertEquals(100, auctions.get(0).getReserve());
        assertTrue(auctions.get(0).getWinnerCanBid());
    }

    @Test
    public void testWinnerCantBid() throws Exception {

        auctionJson.put("winnercanbid", false);
        createTopicMessage();

        topicSubscriber.handle(mirrorTopicMessages);

        List<Auction> auctions = auctionsRepository.getAuctionsList();

        assertEquals(1, auctions.size());
        consensusTimestamp = consensusTimestamp.plus(2, ChronoUnit.DAYS);
        assertEquals(consensusTimeStampWithNanos(), auctions.get(0).getEndtimestamp());
        assertEquals(0, auctions.get(0).getWinningbid());
        assertEquals(tokenId, auctions.get(0).getTokenid());
        assertEquals(auctionAccountId, auctions.get(0).getAuctionaccountid());
        assertEquals(100, auctions.get(0).getReserve());
        assertFalse(auctions.get(0).getWinnerCanBid());
    }

    @Test
    public void testDefaults() throws Exception {

        auctionJson.remove("endtimestamp");
        auctionJson.remove("reserve");
        auctionJson.remove("winnercanbid");
        createTopicMessage();

        topicSubscriber.handle(mirrorTopicMessages);

        List<Auction> auctions = auctionsRepository.getAuctionsList();

        assertEquals(1, auctions.size());
        consensusTimestamp = consensusTimestamp.plus(2, ChronoUnit.DAYS);
        assertEquals(consensusTimeStampWithNanos(), auctions.get(0).getEndtimestamp());
        assertEquals(0, auctions.get(0).getWinningbid());
        assertEquals(tokenId, auctions.get(0).getTokenid());
        assertEquals(auctionAccountId, auctions.get(0).getAuctionaccountid());
        assertEquals(0, auctions.get(0).getReserve());
        assertFalse(auctions.get(0).getWinnerCanBid());
    }

    private void createTopicMessage() {

        byte[] contents = auctionJson.toString().getBytes(StandardCharsets.UTF_8);
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

    private String consensusTimeStampWithNanos() {
        return String.valueOf(consensusTimestamp.getEpochSecond()).concat(".000000000");
    }
}
