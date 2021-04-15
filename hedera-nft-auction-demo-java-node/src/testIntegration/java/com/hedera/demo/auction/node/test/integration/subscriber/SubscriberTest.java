package com.hedera.demo.auction.node.test.integration.subscriber;

import com.hedera.demo.auction.node.app.HederaClient;
import com.hedera.demo.auction.node.app.SqlConnectionManager;
import com.hedera.demo.auction.node.app.domain.Auction;
import com.hedera.demo.auction.node.app.repository.AuctionsRepository;
import com.hedera.demo.auction.node.app.subscriber.TopicMessageWrapper;
import com.hedera.demo.auction.node.app.subscriber.TopicSubscriber;
import com.hedera.demo.auction.node.test.integration.AbstractIntegrationTest;
import com.hedera.hashgraph.sdk.TopicId;
import com.hedera.hashgraph.sdk.TransactionId;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
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
import java.util.List;

@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class SubscriberTest extends AbstractIntegrationTest {

    private PostgreSQLContainer postgres;
    private AuctionsRepository auctionsRepository;
    private final HederaClient hederaClient = HederaClient.emptyTestClient();
    private final static String tokenId = "0.0.10";
    private final static String auctionAccountId = "0.0.20";
    private final static TopicId topicId = TopicId.fromString("0.0.1");

    private TopicMessageWrapper topicMessageWrapper;
    private JsonObject auctionJson;
    private TopicSubscriber topicSubscriber;
    private Instant consensusTimestamp;

    private final static byte[] runningHash = new byte[0];
    private final static long sequenceNumber = 0;
    private final static TransactionId transactionId = null;

    public SubscriberTest() throws Exception {
    }

    @BeforeAll
    public void beforeAll() {
        this.postgres = new PostgreSQLContainer("postgres:12.6");
        this.postgres.start();
        migrate(this.postgres);
        SqlConnectionManager connectionManager = new SqlConnectionManager(this.postgres.getJdbcUrl(), this.postgres.getUsername(), this.postgres.getPassword());
        auctionsRepository = new AuctionsRepository(connectionManager);

        topicSubscriber = new TopicSubscriber(hederaClient, auctionsRepository, null, null, topicId, "", 5000);
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
    }

    @AfterEach
    public void afterEach() throws SQLException {
        auctionsRepository.deleteAllAuctions();
    }

    @Test
    public void testAuctionNoEndTimestamp() throws Exception {

        createTopicMessageWrapper();

        topicSubscriber.handle(hederaClient.client(), topicMessageWrapper);

        List<Auction> auctions = auctionsRepository.getAuctionsList();

        Assertions.assertEquals(1, auctions.size());
        consensusTimestamp = consensusTimestamp.plus(2, ChronoUnit.DAYS);
        Assertions.assertEquals(consensusTimeStampWithNanos(), auctions.get(0).getEndtimestamp());
        Assertions.assertEquals(0, auctions.get(0).getWinningbid());
        Assertions.assertEquals(tokenId, auctions.get(0).getTokenid());
        Assertions.assertEquals(auctionAccountId, auctions.get(0).getAuctionaccountid());
        Assertions.assertEquals(100, auctions.get(0).getReserve());
        Assertions.assertTrue(auctions.get(0).getWinnerCanBid());
    }

    @Test
    public void testAuctionWithEndTimestamp() throws Exception {

        consensusTimestamp = consensusTimestamp.plus(5, ChronoUnit.DAYS);
        auctionJson.put("endtimestamp", String.valueOf(consensusTimestamp.getEpochSecond()));
        createTopicMessageWrapper();

        topicSubscriber.handle(hederaClient.client(), topicMessageWrapper);

        List<Auction> auctions = auctionsRepository.getAuctionsList();

        Assertions.assertEquals(1, auctions.size());
        Assertions.assertEquals(consensusTimeStampWithNanos(), auctions.get(0).getEndtimestamp());
        Assertions.assertEquals(0, auctions.get(0).getWinningbid());
        Assertions.assertEquals(tokenId, auctions.get(0).getTokenid());
        Assertions.assertEquals(auctionAccountId, auctions.get(0).getAuctionaccountid());
        Assertions.assertEquals(100, auctions.get(0).getReserve());
        Assertions.assertTrue(auctions.get(0).getWinnerCanBid());
    }

    @Test
    public void testWinnerCantBid() throws Exception {

        auctionJson.put("winnercanbid", false);
        createTopicMessageWrapper();

        topicSubscriber.handle(hederaClient.client(), topicMessageWrapper);

        List<Auction> auctions = auctionsRepository.getAuctionsList();

        Assertions.assertEquals(1, auctions.size());
        consensusTimestamp = consensusTimestamp.plus(2, ChronoUnit.DAYS);
        Assertions.assertEquals(consensusTimeStampWithNanos(), auctions.get(0).getEndtimestamp());
        Assertions.assertEquals(0, auctions.get(0).getWinningbid());
        Assertions.assertEquals(tokenId, auctions.get(0).getTokenid());
        Assertions.assertEquals(auctionAccountId, auctions.get(0).getAuctionaccountid());
        Assertions.assertEquals(100, auctions.get(0).getReserve());
        Assertions.assertFalse(auctions.get(0).getWinnerCanBid());
    }

    @Test
    public void testDefaults() throws Exception {

        auctionJson.remove("endtimestamp");
        auctionJson.remove("reserve");
        auctionJson.remove("winnercanbid");
        createTopicMessageWrapper();

        topicSubscriber.handle(hederaClient.client(), topicMessageWrapper);

        List<Auction> auctions = auctionsRepository.getAuctionsList();

        Assertions.assertEquals(1, auctions.size());
        consensusTimestamp = consensusTimestamp.plus(2, ChronoUnit.DAYS);
        Assertions.assertEquals(consensusTimeStampWithNanos(), auctions.get(0).getEndtimestamp());
        Assertions.assertEquals(0, auctions.get(0).getWinningbid());
        Assertions.assertEquals(tokenId, auctions.get(0).getTokenid());
        Assertions.assertEquals(auctionAccountId, auctions.get(0).getAuctionaccountid());
        Assertions.assertEquals(0, auctions.get(0).getReserve());
        Assertions.assertFalse(auctions.get(0).getWinnerCanBid());
    }

    private void createTopicMessageWrapper() {
        byte[] contents = auctionJson.toString().getBytes(StandardCharsets.UTF_8);

        topicMessageWrapper = new TopicMessageWrapper(consensusTimestamp, contents, runningHash, sequenceNumber, transactionId);
    }

    private String consensusTimeStampWithNanos() {
        return String.valueOf(consensusTimestamp.getEpochSecond()).concat(".000000000");

    }
}
