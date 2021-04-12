package com.hedera.demo.auction.node.test.integration.database;

import com.hedera.demo.auction.node.app.SqlConnectionManager;
import com.hedera.demo.auction.node.app.domain.Auction;
import com.hedera.demo.auction.node.app.domain.Bid;
import com.hedera.demo.auction.node.app.repository.AuctionsRepository;
import com.hedera.demo.auction.node.app.repository.BidsRepository;
import com.hedera.demo.auction.node.test.integration.AbstractIntegrationTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BidDatabaseTest extends AbstractIntegrationTest {

    private PostgreSQLContainer postgres;
    private BidsRepository bidsRepository;
    private int auctionId;


    public BidDatabaseTest() {
    }

    @BeforeAll
    public void setupDatabase() throws SQLException {
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
    public void stopDatabase() {
        this.postgres.close();
    }

    @Test
    public void addBidTest() throws SQLException {

        Bid bid = testBidObject(1, auctionId);
        bidsRepository.add(bid);

        List<Bid> bids = bidsRepository.getBidsList();
        assertNotNull(bids);
        assertEquals(1, bids.size());
        testNewBid(bid, bids.get(0));
        bidsRepository.deleteAllBids();
    }

    @Test
    public void addDuplicateBidTest() throws SQLException {

        Bid bid = testBidObject(1, auctionId);
        bidsRepository.add(bid);
        bidsRepository.add(bid);

        List<Bid> bids = bidsRepository.getBidsList();
        assertNotNull(bids);
        assertEquals(1, bids.size());
        bidsRepository.deleteAllBids();
    }

    @Test
    public void setStatusTest() throws SQLException {
        Bid bid = testBidObject(1, auctionId);
        bidsRepository.add(bid);

        String newStatus = "test status update";
        bid.setStatus(newStatus);

        bidsRepository.setStatus(bid);

        List<Bid> bids = bidsRepository.getBidsList();
        assertNotNull(bids);
        assertEquals(1, bids.size());
        assertEquals(newStatus, bids.get(0).getStatus());

        bidsRepository.deleteAllBids();
    }

    @Test
    public void setRefundInProgressTest() throws SQLException {
        Bid bid = testBidObject(1, auctionId);
        bidsRepository.add(bid);

        String transactionId = "refundTransactionId";
        String transactionHash = "refundTransactionHash";
        bidsRepository.setRefundInProgress(bid.getTimestamp(), transactionId, transactionHash);

        List<Bid> bids = bidsRepository.getBidsList();
        assertNotNull(bids);
        assertEquals(1, bids.size());
        assertEquals(transactionId, bids.get(0).getRefundtxid());
        assertEquals(transactionHash, bids.get(0).getRefundtxhash());

        bidsRepository.deleteAllBids();
    }

    @Test
    public void setRefunded() throws SQLException {
        Bid bid = testBidObject(1, auctionId);
        bidsRepository.add(bid);

        String transactionHash = "refundTransactionHash";
        bidsRepository.setRefunded(bid.getTimestamp(), transactionHash);

        List<Bid> bids = bidsRepository.getBidsList();
        assertNotNull(bids);
        assertEquals(1, bids.size());
        assertEquals(transactionHash, bids.get(0).getTransactionhash());

        bidsRepository.deleteAllBids();
    }

    @Test
    public void bidsRefundToConfirmTest() throws SQLException {
        int numBids = 3;

        for (int i=0; i < numBids; i++) {
            Bid bid = testBidObject(i, auctionId);
            bidsRepository.add(bid);
            String transactionId = "refundTransactionId";
            String transactionHash = "refundTransactionHash";
            bidsRepository.setRefundInProgress(bid.getTimestamp(), transactionId, transactionHash);
        }

        Map<String, String> bids = bidsRepository.bidsRefundToConfirm();
        assertNotNull(bids);
        assertEquals(numBids, bids.size());

        bidsRepository.deleteAllBids();
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
