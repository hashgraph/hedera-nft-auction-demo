package com.hedera.demo.auction.node.test.system.e2e;

import com.hedera.demo.auction.node.app.SqlConnectionManager;
import com.hedera.demo.auction.node.app.repository.AuctionsRepository;
import com.hedera.demo.auction.node.app.repository.BidsRepository;
import com.hedera.hashgraph.sdk.TopicId;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith({VertxExtension.class})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class SimpleE2ETest extends AbstractE2ETest {

    Vertx vertx;

    SimpleE2ETest() throws Exception {
        super();
    }

    @BeforeAll
    public void beforeAll() {
        postgres = new PostgreSQLContainer("postgres:12.6");
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
    }

    @Test
    public void testAdminRestApiTopic(VertxTestContext testContext) {

        GenericContainer container = appContainer(postgres,"", "hedera");
        int adminPort = container.getMappedPort(8082);

        webClient.post(adminPort, container.getHost(), "/v1/admin/topic")
                .send(testContext.succeeding(response -> testContext.verify(() -> {

                    assertNotNull(response.body());
                    JsonObject body = JsonObject.mapFrom(response.body());
                    assertNotNull(body);

                    assertNotEquals("", body.getString("topicId"));

                    topicId = TopicId.fromString(body.getString("topicId"));

                    getTopicInfo();

                    assertEquals(topicInfo.topicId, topicId);

                    testContext.completeNow();
                })));

    }

//    @ParameterizedTest
//    @JsonFileSource(resources = "/e2eSystemTests.json")
//    public void e2eTest(JsonObject test) throws Exception {
//        log.info("Starting e2e test : " + test.getString("testName"));
//
//        JsonArray mirrors = test.getJsonArray("mirrors");
//
//        long reserve = test.getJsonObject("auctionDetails").getJsonNumber("reserve").longValue();
//        long minimumBid = test.getJsonObject("auctionDetails").getJsonNumber("minimumBid").longValue();
//        boolean winnerCanBid = test.getJsonObject("auctionDetails").getBoolean("winnerCanBid", true);
//        boolean transferOnWin = test.getJsonObject("auctionDetails").getBoolean("transferOnWin", true);
//
//        for (JsonValue mirrorValue : mirrors) {
//            String mirror = ((JsonString) mirrorValue).getString();
//            log.info("  Using mirror " + mirror);
//            createAuction(reserve, minimumBid, winnerCanBid);
//            hederaClient.setMirrorProvider(mirror);
//            hederaClient.setClientMirror(dotenv);
//
//            App app = new App();
//            app.overrideEnv(hederaClient, /* restAPI= */false, /* adminAPI= */false, /* auctionNode= */true, topicId.toString(), hederaClient.operatorPrivateKey().toString(), postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword(), /* transferOnWin= */transferOnWin);
//            app.runApp();
//
//            // wait for auction to appear in database
//            await()
//                    .with()
//                    .pollInterval(Duration.ofSeconds(1))
//                    .await()
//                    .atMost(Duration.ofSeconds(15))
//                    .until(auctionsCountMatches(1));
//
//            // query repository for auctions
//            List<Auction> auctionsList = auctionsRepository.getAuctionsList();
//
//            assertEquals(1, auctionsList.size());
//            auction = auctionsList.get(0);
//
//            JsonArray tasks = test.getJsonArray("tasks");
//            for (JsonValue taskJson : tasks) {
//                JsonObject task = taskJson.asJsonObject();
//                String taskName = task.getString("name");
//
//                log.info("  running task " + taskName);
//
//                switch (taskName) {
//                    case "transferToken":
//                        // transfer the token to the auction
//                        transferToken();
//                        break;
//                    default:
//                        log.error("unknown task " + task);
//                }
//            }
//
//            JsonArray assertions = test.getJsonArray("assertions");
//            for (JsonValue assertionJson : assertions) {
//                JsonObject assertion = assertionJson.asJsonObject();
//
//                log.info("  asserting " + assertion.toString());
//
//                String object = assertion.getString("object");
//                String parameter = assertion.getString("parameter");
//                String value = assertion.getString("value");
//                String condition = assertion.getString("condition");
//
//                switch (object) {
//                    case "auction":
//                        await()
//                                .with()
//                                .pollInterval(Duration.ofSeconds(1))
//                                .await()
//                                .atMost(Duration.ofSeconds(30))
//                                .until(auctionValueAssert(parameter, value, condition));
//                        break;
//                    default:
//                        log.error("unknown assertion " + object);
//                }
//            }
//
//            app.stop();
//
//        }
//    }

//    @Test
//    public void testCreateAuctionKabutoMirror() throws Exception {
//
//        hederaClient.setMirrorProvider("kabuto");
//        hederaClient.setClientMirror(dotenv);
//        TopicSubscriber topicSubscriber = new TopicSubscriber(hederaClient, auctionsRepository, bidsRepository, null, topicId, hederaClient.operatorPrivateKey().toString(), 5000);
//        topicSubscriber.setSkipReadinessWatcher();
//        // start the thread to monitor bids
//        Thread t = new Thread(topicSubscriber);
//        t.start();
//
//        // wait for auction to appear in database
//        await()
//                .with()
//                .pollInterval(Duration.ofSeconds(1))
//                .await()
//                .atMost(Duration.ofSeconds(20))
//                .until(auctionsCountMatches(1));
//
//        topicSubscriber.stop();
//
//        // query repository for auctions
//        List<Auction> auctionsList = auctionsRepository.getAuctionsList();
//
//        assertEquals(1, auctionsList.size());
//        auction = auctionsList.get(0);
//
//        assertEquals(tokenId.toString(), auction.getTokenid());
//        assertEquals(auctionAccountId.toString(), auction.getAuctionaccountid());
//        assertNotNull(auction.getEndtimestamp());
//        assertEquals(reserve, auction.getReserve());
//        assertEquals("0.0", auction.getLastconsensustimestamp());
//        assertEquals(winnerCanBid, auction.getWinnerCanBid());
//        assertNull(auction.getTokenimage());
//        assertEquals(0, auction.getWinningbid());
//        assertEquals(minimumBid, auction.getMinimumbid());
//
//        // wait for token to be associated to auction account
//        await()
//                .with()
//                .pollInterval(Duration.ofSeconds(1))
//                .await()
//                .atMost(Duration.ofSeconds(10))
//                .until(tokenAssociatedNotTransferred());
//    }
}
