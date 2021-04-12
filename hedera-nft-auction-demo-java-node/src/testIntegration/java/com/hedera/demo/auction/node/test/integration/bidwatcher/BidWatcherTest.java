package com.hedera.demo.auction.node.test.integration.bidwatcher;

import com.hedera.demo.auction.node.app.HederaClient;
import com.hedera.demo.auction.node.app.SqlConnectionManager;
import com.hedera.demo.auction.node.app.bidwatcher.AbstractBidsWatcher;
import com.hedera.demo.auction.node.app.domain.Auction;
import com.hedera.demo.auction.node.app.repository.AuctionsRepository;
import com.hedera.demo.auction.node.app.repository.BidsRepository;
import com.hedera.demo.auction.node.test.integration.AbstractIntegrationTest;
import io.vertx.ext.web.client.WebClient;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BidWatcherTest extends AbstractIntegrationTest {

    public BidWatcherTest() {
    }

    private PostgreSQLContainer postgres;
    private AuctionsRepository auctionsRepository;
    private HederaClient hederaClient;

    static class ReadinessTester extends AbstractBidsWatcher {

        protected ReadinessTester(HederaClient hederaClient, WebClient webClient, AuctionsRepository auctionsRepository, BidsRepository bidsRepository, int auctionId, String refundKey, int mirrorQueryFrequency) throws Exception {
            super(hederaClient, webClient, auctionsRepository, bidsRepository, auctionId, refundKey, mirrorQueryFrequency);
        }
    }

    @BeforeAll
    public void setupDatabase() throws Exception {
        PostgreSQLContainer<?> postgres = new PostgreSQLContainer("postgres:12.6");
        postgres.start();
        migrate(postgres);
        SqlConnectionManager connectionManager = new SqlConnectionManager(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
        auctionsRepository = new AuctionsRepository(connectionManager);
        this.postgres = postgres;
        this.hederaClient = HederaClient.emptyTestClient();
    }

    @AfterAll
    public void stopDatabase() {
        this.postgres.close();
    }

    @Test
    public void testMemo() throws Exception {

        Auction auction = testAuctionObject(1);
        Auction newAuction = auctionsRepository.add(auction);

        ReadinessTester readinessTester = new ReadinessTester(hederaClient, webClient, auctionsRepository, null, newAuction.getId(), "", 5000);

        assertTrue(readinessTester.checkMemos("CREATEAUCTION"));
        assertTrue(readinessTester.checkMemos("FUNDACCOUNT"));
        assertTrue(readinessTester.checkMemos("TRANSFERTOAUCTION"));
        assertTrue(readinessTester.checkMemos("ASSOCIATE"));
        assertTrue(readinessTester.checkMemos("AUCTION REFUND"));

        assertTrue(readinessTester.checkMemos("createauction"));
        assertTrue(readinessTester.checkMemos("fundaccount"));
        assertTrue(readinessTester.checkMemos("transfertoauction"));
        assertTrue(readinessTester.checkMemos("associate"));
        assertTrue(readinessTester.checkMemos("auction refund"));

        assertFalse(readinessTester.checkMemos(""));
        assertFalse(readinessTester.checkMemos("memo"));


        auctionsRepository.deleteAllAuctions();
    }
}
