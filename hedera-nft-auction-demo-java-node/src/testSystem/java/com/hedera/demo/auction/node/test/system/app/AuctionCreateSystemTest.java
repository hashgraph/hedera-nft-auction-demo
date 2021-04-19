package com.hedera.demo.auction.node.test.system.app;

import com.hedera.demo.auction.node.app.SqlConnectionManager;
import com.hedera.demo.auction.node.app.domain.Auction;
import com.hedera.demo.auction.node.app.repository.AuctionsRepository;
import com.hedera.demo.auction.node.app.repository.BidsRepository;
import com.hedera.demo.auction.node.app.subscriber.TopicSubscriber;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.testcontainers.containers.PostgreSQLContainer;

import java.time.Duration;
import java.util.List;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class AuctionCreateSystemTest extends AbstractSystemTest {

    private static final long reserve = 1000000;
    private static final long minimumBid = 0;
    private static final boolean winnerCanBid = true;

    AuctionCreateSystemTest() throws Exception {
    }

    @BeforeAll
    public void beforeAll() throws Exception {
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
        createAuction(reserve, minimumBid, winnerCanBid);
    }
    @Test
    public void testCreateAuctionHederaMirror() throws Exception {

        hederaClient.setMirrorProvider("hedera");
        hederaClient.setClientMirror(dotenv);
        TopicSubscriber topicSubscriber = new TopicSubscriber(hederaClient, auctionsRepository, bidsRepository, null, topicId, hederaClient.operatorPrivateKey().toString(), 5000);
        topicSubscriber.setSkipReadinessWatcher();
        // start the thread to monitor bids
        Thread t = new Thread(topicSubscriber);
        t.start();

        // wait for auction to appear in database
        await()
                .with()
                .pollInterval(Duration.ofSeconds(1))
                .await()
                .atMost(Duration.ofSeconds(15))
                .until(auctionsCountMatches(1));

        topicSubscriber.stop();

        // query repository for auctions
        List<Auction> auctionsList = auctionsRepository.getAuctionsList();

        assertEquals(1, auctionsList.size());
        auction = auctionsList.get(0);

        assertEquals(tokenId.toString(), auction.getTokenid());
        assertEquals(auctionAccountId.toString(), auction.getAuctionaccountid());
        assertNotNull(auction.getEndtimestamp());
        assertEquals(reserve, auction.getReserve());
        assertEquals("0.0", auction.getLastconsensustimestamp());
        assertEquals(winnerCanBid, auction.getWinnerCanBid());
        assertNull(auction.getTokenimage());
        assertEquals(0, auction.getWinningbid());
        assertEquals(minimumBid, auction.getMinimumbid());

    }
}
