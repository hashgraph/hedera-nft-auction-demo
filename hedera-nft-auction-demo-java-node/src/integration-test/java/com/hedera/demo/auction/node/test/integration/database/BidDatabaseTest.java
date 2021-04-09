package com.hedera.demo.auction.node.test.integration.database;

import com.hedera.demo.auction.node.app.SqlConnectionManager;
import com.hedera.demo.auction.node.app.domain.Auction;
import com.hedera.demo.auction.node.app.domain.Bid;
import com.hedera.demo.auction.node.app.repository.AuctionsRepository;
import com.hedera.demo.auction.node.app.repository.BidsRepository;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Testcontainers
class BidDatabaseTest extends AbstractDatabaseTest {

    public BidDatabaseTest() {
    }

    private int createAuction(AuctionsRepository auctionsRepository) {
        Auction auction = testAuctionObject(1);
        Auction newAuction = auctionsRepository.add(auction);
        return newAuction.getId();

    }
    @Test
    public void addBidTest() {

        try (PostgreSQLContainer<?> postgres = new PostgreSQLContainer("postgres:12.6")) {
            postgres.start();
            migrate(postgres);

            SqlConnectionManager connectionManager = new SqlConnectionManager(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
            AuctionsRepository auctionsRepository = new AuctionsRepository(connectionManager);
            BidsRepository bidsRepository = new BidsRepository(connectionManager);

            int auctionId = createAuction(auctionsRepository);

            Bid bid = testBidObject(1, auctionId);
            bidsRepository.add(bid);

            List<Bid> bids = bidsRepository.getBidsList();
            assertNotNull(bids);
            assertEquals(1, bids.size());
            testNewBid(bid, bids.get(0));
        }
    }

    @Test
    public void addDuplicateBidTest() {

        try (PostgreSQLContainer<?> postgres = new PostgreSQLContainer("postgres:12.6")) {
            postgres.start();
            migrate(postgres);

            SqlConnectionManager connectionManager = new SqlConnectionManager(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
            AuctionsRepository auctionsRepository = new AuctionsRepository(connectionManager);
            BidsRepository bidsRepository = new BidsRepository(connectionManager);

            int auctionId = createAuction(auctionsRepository);

            Bid bid = testBidObject(1, auctionId);
            bidsRepository.add(bid);
            bidsRepository.add(bid);

            List<Bid> bids = bidsRepository.getBidsList();
            assertNotNull(bids);
            assertEquals(1, bids.size());
        }
    }

    @Test
    public void setStatusTest() {
        try (PostgreSQLContainer<?> postgres = new PostgreSQLContainer("postgres:12.6")) {
            postgres.start();
            migrate(postgres);

            SqlConnectionManager connectionManager = new SqlConnectionManager(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
            AuctionsRepository auctionsRepository = new AuctionsRepository(connectionManager);
            BidsRepository bidsRepository = new BidsRepository(connectionManager);

            int auctionId = createAuction(auctionsRepository);

            Bid bid = testBidObject(1, auctionId);
            bidsRepository.add(bid);

            String newStatus = "test status update";
            bid.setStatus(newStatus);

            bidsRepository.setStatus(bid);

            List<Bid> bids = bidsRepository.getBidsList();
            assertNotNull(bids);
            assertEquals(1, bids.size());
            assertEquals(newStatus, bids.get(0).getStatus());
        }
    }

    @Test
    public void setRefundInProgressTest() {
        try (PostgreSQLContainer<?> postgres = new PostgreSQLContainer("postgres:12.6")) {
            postgres.start();
            migrate(postgres);

            SqlConnectionManager connectionManager = new SqlConnectionManager(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
            AuctionsRepository auctionsRepository = new AuctionsRepository(connectionManager);
            BidsRepository bidsRepository = new BidsRepository(connectionManager);

            int auctionId = createAuction(auctionsRepository);

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
        }
    }

    @Test
    public void setRefunded() {
        try (PostgreSQLContainer<?> postgres = new PostgreSQLContainer("postgres:12.6")) {
            postgres.start();
            migrate(postgres);

            SqlConnectionManager connectionManager = new SqlConnectionManager(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
            AuctionsRepository auctionsRepository = new AuctionsRepository(connectionManager);
            BidsRepository bidsRepository = new BidsRepository(connectionManager);

            int auctionId = createAuction(auctionsRepository);

            Bid bid = testBidObject(1, auctionId);
            bidsRepository.add(bid);

            String transactionHash = "refundTransactionHash";
            bidsRepository.setRefunded(bid.getTimestamp(), transactionHash);

            List<Bid> bids = bidsRepository.getBidsList();
            assertNotNull(bids);
            assertEquals(1, bids.size());
            assertEquals(transactionHash, bids.get(0).getTransactionhash());
        }
    }

    @Test
    public void bidsRefundToConfirmTest() {
        try (PostgreSQLContainer<?> postgres = new PostgreSQLContainer("postgres:12.6")) {
            postgres.start();
            migrate(postgres);

            SqlConnectionManager connectionManager = new SqlConnectionManager(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
            AuctionsRepository auctionsRepository = new AuctionsRepository(connectionManager);
            BidsRepository bidsRepository = new BidsRepository(connectionManager);

            int auctionId = createAuction(auctionsRepository);
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
        }
    }

    @Test
    public void deleteAllBidsTest() {
        try (PostgreSQLContainer<?> postgres = new PostgreSQLContainer("postgres:12.6")) {
            postgres.start();
            migrate(postgres);

            SqlConnectionManager connectionManager = new SqlConnectionManager(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
            AuctionsRepository auctionsRepository = new AuctionsRepository(connectionManager);
            BidsRepository bidsRepository = new BidsRepository(connectionManager);

            int auctionId = createAuction(auctionsRepository);

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
}
