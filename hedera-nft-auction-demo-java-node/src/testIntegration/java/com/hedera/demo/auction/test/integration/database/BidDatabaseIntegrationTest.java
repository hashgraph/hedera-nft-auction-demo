package com.hedera.demo.auction.test.integration.database;

import com.google.errorprone.annotations.Var;
import com.hedera.demo.auction.app.SqlConnectionManager;
import com.hedera.demo.auction.app.domain.Auction;
import com.hedera.demo.auction.app.domain.Bid;
import com.hedera.demo.auction.app.repository.AuctionsRepository;
import com.hedera.demo.auction.app.repository.BidsRepository;
import com.hedera.demo.auction.test.integration.AbstractIntegrationTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static junit.framework.TestCase.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BidDatabaseIntegrationTest extends AbstractIntegrationTest {

    private PostgreSQLContainer postgres;
    private BidsRepository bidsRepository;
    private int auctionId;
    private Bid bid;

    public BidDatabaseIntegrationTest() {
    }

    @BeforeAll
    public void beforeAll() throws SQLException {
        PostgreSQLContainer<?> postgres = new PostgreSQLContainer("postgres:12.6");
        postgres.start();
        migrate(postgres);
        SqlConnectionManager connectionManager = new SqlConnectionManager(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
        AuctionsRepository auctionsRepository = new AuctionsRepository(connectionManager);
        bidsRepository = new BidsRepository(connectionManager);
        this.postgres = postgres;

        Auction auction = testAuctionObject(1);
        Auction newAuction = auctionsRepository.add(auction);
        this.auctionId =  newAuction.getId();;
    }

    @AfterAll
    public void afterAll() {
        this.postgres.close();
    }

    @BeforeEach
    public void beforeEach() throws SQLException {
        bid = testBidObject(1, auctionId);
        bidsRepository.add(bid);
    }
    @AfterEach
    public void afterEach() throws SQLException {
        bidsRepository.deleteAllBids();
    }
    @Test
    public void addBidTest() throws SQLException {

        List<Bid> bids = bidsRepository.getBidsList();
        assertNotNull(bids);
        assertEquals(1, bids.size());
        testNewBid(bid, bids.get(0));
    }

    @Test
    public void addDuplicateBidTest() throws SQLException {

        // adding to the @beforeEach bid that's already created
        bidsRepository.add(bid);

        List<Bid> bids = bidsRepository.getBidsList();
        assertNotNull(bids);
        assertEquals(1, bids.size());
    }

    @Test
    public void setRefundInProgressTest() throws SQLException {

        bidsRepository.setRefundPending(bid.getTransactionid());
//        bidsRepository.setRefundIssued(bid.getTimestamp(), "");
        bidsRepository.setRefundIssuing(bid.getTimestamp());

        @Var List<Bid> bids = bidsRepository.getBidsList();
        assertNotNull(bids);
        assertEquals(1, bids.size());
        assertTrue(bids.get(0).isRefundIssuing());

        bidsRepository.setRefundIssued(bid.getTimestamp(), "someTxId", "scheduleId");

        bids = bidsRepository.getBidsList();
        assertNotNull(bids);
        assertEquals(1, bids.size());
        assertTrue(bids.get(0).isRefundIssued());
        assertEquals("someTxId", bids.get(0).getRefundtxid());
        assertEquals("scheduleId", bids.get(0).getScheduleId());
    }

    @Test
    public void setRefundPendingTest() throws SQLException {

        bidsRepository.setRefundPending(bid.getTransactionid());

        List<Bid> bids = bidsRepository.getBidsList();
        assertNotNull(bids);
        assertEquals(1, bids.size());
        assertTrue(bids.get(0).isRefundPending());
        assertEquals("", bids.get(0).getRefundtxid());
        assertEquals("", bids.get(0).getRefundtxhash());
    }

    @Test
    public void setRefunded() throws SQLException {

        String transactionHash = "refundTransactionHash";
        String transactionId = "refundTransactionId";
        bidsRepository.setRefunded(bid.getTransactionid(), transactionId, transactionHash);

        List<Bid> bids = bidsRepository.getBidsList();
        assertNotNull(bids);
        assertEquals(1, bids.size());
        assertEquals(transactionId, bids.get(0).getRefundtxid());
        assertEquals(transactionHash, bids.get(0).getRefundtxhash());

    }

@Test
public void setRefundError() throws SQLException {

    @Var boolean updated = bidsRepository.setRefundError(bid.getTransactionid());
    assertTrue(updated);

    List<Bid> bids = bidsRepository.getBidsList();
    assertNotNull(bids);
    assertEquals(1, bids.size());
    assertEquals(Bid.REFUND_ERROR, bids.get(0).getRefundstatus());
    assertEquals("", bids.get(0).getScheduleId());
    assertEquals("", bids.get(0).getRefundtxhash());
    assertEquals("", bids.get(0).getRefundtxid());

    updated = bidsRepository.setRefundError("dummy transaction id");

    assertFalse(updated);
}


    @Test
    public void firstBidToRefundTest() throws SQLException {
        int numBids = 3;
        bidsRepository.deleteAllBids();
        List<Bid> bids = new ArrayList<>();

        for (int i=0; i < numBids; i++) {
            Bid bid = testBidObject(i, auctionId);
            bids.add(bid);
            bidsRepository.add(bid);
            bidsRepository.setRefundIssued(bid.getTimestamp(), "", "");
        }

        String firstBidToRefund = bidsRepository.getFirstBidToRefund(auctionId);
        assertNotNull(firstBidToRefund);
        assertEquals(bids.get(0).getTimestamp(), firstBidToRefund);

    }

    @Test
    public void deleteAllBidsTest() throws SQLException {

        for (int i=0; i < 5; i++) {
            Bid bid = testBidObject(i, auctionId);
            bidsRepository.add(bid);
        }

        bidsRepository.deleteAllBids();

        List<Bid> bids = bidsRepository.getBidsList();
        assertNotNull(bids);
        assertEquals(0, bids.size());
    }
}
