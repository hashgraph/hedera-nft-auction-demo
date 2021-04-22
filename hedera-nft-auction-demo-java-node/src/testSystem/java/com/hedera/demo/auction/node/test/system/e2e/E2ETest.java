package com.hedera.demo.auction.node.test.system.e2e;

import com.hedera.demo.auction.node.app.App;
import com.hedera.demo.auction.node.app.SqlConnectionManager;
import com.hedera.demo.auction.node.app.domain.Auction;
import com.hedera.demo.auction.node.app.repository.AuctionsRepository;
import com.hedera.demo.auction.node.app.repository.BidsRepository;
import com.hedera.demo.auction.node.test.system.AbstractSystemTest;
import lombok.extern.log4j.Log4j2;
import net.joshka.junit.json.params.JsonFileSource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.testcontainers.containers.PostgreSQLContainer;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonString;
import javax.json.JsonValue;
import java.time.Duration;
import java.util.List;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Log4j2
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class E2ETest extends AbstractSystemTest {

    E2ETest() throws Exception {
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
    @ParameterizedTest
    @JsonFileSource(resources = "/e2eSystemTests.json")
    public void e2eTest(JsonObject test) throws Exception {
        log.info("Starting e2e test : " + test.getString("testName"));

        JsonArray mirrors = test.getJsonArray("mirrors");

        long reserve = test.getJsonObject("auctionDetails").getJsonNumber("reserve").longValue();
        long minimumBid = test.getJsonObject("auctionDetails").getJsonNumber("minimumBid").longValue();
        boolean winnerCanBid = test.getJsonObject("auctionDetails").getBoolean("winnerCanBid", true);
        boolean transferOnWin = test.getJsonObject("auctionDetails").getBoolean("transferOnWin", true);

        for (JsonValue mirrorValue : mirrors) {
            String mirror = ((JsonString) mirrorValue).getString();
            log.info("  Using mirror " + mirror);
            createAuction(reserve, minimumBid, winnerCanBid);
            hederaClient.setMirrorProvider(mirror);
            hederaClient.setClientMirror(dotenv);

            App app = new App();
            app.overrideEnv(hederaClient, /* restAPI= */false, /* adminAPI= */false, /* auctionNode= */true, topicId.toString(), hederaClient.operatorPrivateKey().toString(), postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword(), /* transferOnWin= */transferOnWin);
            app.runApp();

            // wait for auction to appear in database
            await()
                    .with()
                    .pollInterval(Duration.ofSeconds(1))
                    .await()
                    .atMost(Duration.ofSeconds(15))
                    .until(auctionsCountMatches(1));

            // query repository for auctions
            List<Auction> auctionsList = auctionsRepository.getAuctionsList();

            assertEquals(1, auctionsList.size());
            auction = auctionsList.get(0);

            JsonArray tasks = test.getJsonArray("tasks");
            for (JsonValue taskJson : tasks) {
                JsonObject task = taskJson.asJsonObject();
                String taskName = task.getString("name");

                log.info("  running task " + taskName);

                switch (taskName) {
                    case "transferToken":
                        // transfer the token to the auction
                        transferToken();
                        break;
                    default:
                        log.error("unknown task " + task);
                }
            }

            JsonArray assertions = test.getJsonArray("assertions");
            for (JsonValue assertionJson : assertions) {
                JsonObject assertion = assertionJson.asJsonObject();

                log.info("  asserting " + assertion.toString());

                String object = assertion.getString("object");
                String parameter = assertion.getString("parameter");
                String value = assertion.getString("value");
                String condition = assertion.getString("condition");

                switch (object) {
                    case "auction":
                        await()
                                .with()
                                .pollInterval(Duration.ofSeconds(1))
                                .await()
                                .atMost(Duration.ofSeconds(30))
                                .until(auctionValueAssert(parameter, value, condition));
                        break;
                    default:
                        log.error("unknown assertion " + object);
                }
            }

            app.stop();

        }
    }

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
