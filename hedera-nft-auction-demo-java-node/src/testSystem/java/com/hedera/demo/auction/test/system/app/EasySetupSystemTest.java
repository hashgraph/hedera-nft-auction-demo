package com.hedera.demo.auction.test.system.app;

import com.hedera.demo.auction.app.SqlConnectionManager;
import com.hedera.demo.auction.app.domain.Auction;
import com.hedera.demo.auction.app.repository.AuctionsRepository;
import com.hedera.demo.auction.app.repository.BidsRepository;
import com.hedera.demo.auction.app.repository.ScheduledOperationsRepository;
import com.hedera.demo.auction.app.subscriber.TopicSubscriber;
import com.hedera.demo.auction.test.system.AbstractSystemTest;
import com.hedera.hashgraph.sdk.TopicId;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class EasySetupSystemTest extends AbstractSystemTest {

    EasySetupSystemTest() throws Exception {
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
        scheduledOperationsRepository = new ScheduledOperationsRepository(connectionManager);
    }

    @AfterAll
    public void afterAll() {
        this.postgres.close();
    }

    @BeforeEach
    public void beforeEach() throws Exception {
        bidsRepository.deleteAllBids();
        auctionsRepository.deleteAllAuctions();
        String[] args = new String[0];
        String topic = easySetup.setup(args);
        topicId = TopicId.fromString(topic);
    }
    @Test
    public void testCreateAuctionHederaMirror() throws Exception {

        hederaClient.setMirrorProvider("hedera");
        hederaClient.setClientMirror();
        TopicSubscriber topicSubscriber = new TopicSubscriber(hederaClient, auctionsRepository, bidsRepository,  null, topicId, 5000, masterKey.toString());

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

        assertNotNull(auction.getTokenid());
        assertNotNull(auction.getAuctionaccountid());
        assertNotNull(auction.getEndtimestamp());
        assertEquals(0, auction.getReserve());
        assertEquals("0.0", auction.getLastconsensustimestamp());
        assertTrue(auction.getWinnerCanBid());
        assertNotNull(auction.getTokenmetadata());
        assertEquals(0, auction.getWinningbid());
        assertEquals(1000000, auction.getMinimumbid());

    }
}