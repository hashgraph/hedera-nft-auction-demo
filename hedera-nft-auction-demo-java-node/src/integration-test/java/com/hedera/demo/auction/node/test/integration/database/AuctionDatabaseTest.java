package com.hedera.demo.auction.node.test.integration.database;

import com.google.errorprone.annotations.Var;
import com.hedera.demo.auction.node.app.SqlConnectionManager;
import com.hedera.demo.auction.node.app.domain.Auction;
import com.hedera.demo.auction.node.app.repository.AuctionsRepository;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
class AuctionDatabaseTest extends AbstractAuction {

    public AuctionDatabaseTest() {
    }

    @Test
    public void addAuctionTest() throws Exception {

        try (PostgreSQLContainer<?> postgres = new PostgreSQLContainer("postgres:12.6")) {
            postgres.start();
            migrate(postgres);

            SqlConnectionManager connectionManager = new SqlConnectionManager(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
            AuctionsRepository auctionsRepository = new AuctionsRepository(connectionManager);

            Auction auction = testAuctionObject(1);
            Auction newAuction = auctionsRepository.add(auction);

            assertNotEquals(0, newAuction.getId());

            Auction getAuction = auctionsRepository.getAuction(newAuction.getId());

            testNewAuction(auction, getAuction);
        }
    }

    @Test
    public void setAuctionActiveTest() throws Exception {
        try (PostgreSQLContainer<?> postgres = new PostgreSQLContainer("postgres:12.6")) {
            postgres.start();
            migrate(postgres);

            SqlConnectionManager connectionManager = new SqlConnectionManager(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
            AuctionsRepository auctionsRepository = new AuctionsRepository(connectionManager);

            Auction auction = testAuctionObject(1);
            Auction newAuction = auctionsRepository.add(auction);

            auctionsRepository.setActive(newAuction, auction.getStarttimestamp());
            Auction getAuction = auctionsRepository.getAuction(newAuction.getId());

            assertEquals(Auction.active(), getAuction.getStatus());
            assertEquals(auction.getStarttimestamp(), getAuction.getStarttimestamp());
        }
    }

    @Test
    public void setAuctionTransferringTest() throws Exception {
        try (PostgreSQLContainer<?> postgres = new PostgreSQLContainer("postgres:12.6")) {
            postgres.start();
            migrate(postgres);

            SqlConnectionManager connectionManager = new SqlConnectionManager(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
            AuctionsRepository auctionsRepository = new AuctionsRepository(connectionManager);

            Auction auction = testAuctionObject(1);
            Auction newAuction = auctionsRepository.add(auction);

            auctionsRepository.setTransferring(auction.getTokenid());
            Auction getAuction = auctionsRepository.getAuction(newAuction.getId());

            assertEquals(Auction.transfer(), getAuction.getStatus());
        }
    }

    @Test
    public void setTransferTransactionTest() throws Exception {
        try (PostgreSQLContainer<?> postgres = new PostgreSQLContainer("postgres:12.6")) {
            postgres.start();
            migrate(postgres);

            SqlConnectionManager connectionManager = new SqlConnectionManager(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
            AuctionsRepository auctionsRepository = new AuctionsRepository(connectionManager);

            Auction auction = testAuctionObject(1);
            Auction newAuction = auctionsRepository.add(auction);

            auctionsRepository.setTransferTransaction(newAuction.getId(), auction.getTransfertxid(), auction.getTransfertxhash());
            Auction getAuction = auctionsRepository.getAuction(newAuction.getId());

            assertEquals(auction.getTransfertxid(), getAuction.getTransfertxid());
            assertEquals(auction.getTransfertxhash(), getAuction.getTransfertxhash());
        }
    }

    @Test
    public void setEndedTest() throws Exception {
        try (PostgreSQLContainer<?> postgres = new PostgreSQLContainer("postgres:12.6")) {
            postgres.start();
            migrate(postgres);

            SqlConnectionManager connectionManager = new SqlConnectionManager(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
            AuctionsRepository auctionsRepository = new AuctionsRepository(connectionManager);

            Auction auction = testAuctionObject(1);
            Auction newAuction = auctionsRepository.add(auction);

            auctionsRepository.setEnded(newAuction.getId(), auction.getTransfertxhash());
            Auction getAuction = auctionsRepository.getAuction(newAuction.getId());

            assertEquals(auction.getTransfertxhash(), getAuction.getTransfertxhash());
        }
    }
    @Test
    public void setClosedByAuctionTest() throws Exception {
        try (PostgreSQLContainer<?> postgres = new PostgreSQLContainer("postgres:12.6")) {
            postgres.start();
            migrate(postgres);

            SqlConnectionManager connectionManager = new SqlConnectionManager(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
            AuctionsRepository auctionsRepository = new AuctionsRepository(connectionManager);

            Auction auction = testAuctionObject(1);
            @Var Auction newAuction = auctionsRepository.add(auction);

            newAuction = auctionsRepository.setClosed(newAuction);
            assertEquals(Auction.closed(), newAuction.getStatus());

            Auction getAuction = auctionsRepository.getAuction(newAuction.getId());
            assertEquals(Auction.closed(), getAuction.getStatus());
        }
    }

    @Test
    public void setClosedByAuctionIdTest() throws Exception {
        try (PostgreSQLContainer<?> postgres = new PostgreSQLContainer("postgres:12.6")) {
            postgres.start();
            migrate(postgres);

            SqlConnectionManager connectionManager = new SqlConnectionManager(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
            AuctionsRepository auctionsRepository = new AuctionsRepository(connectionManager);

            Auction auction = testAuctionObject(1);
            Auction newAuction = auctionsRepository.add(auction);

            auctionsRepository.setClosed(newAuction.getId());

            Auction getAuction = auctionsRepository.getAuction(newAuction.getId());
            assertEquals(Auction.closed(), getAuction.getStatus());
        }
    }

    @Test
    public void saveTest() throws Exception {
        try (PostgreSQLContainer<?> postgres = new PostgreSQLContainer("postgres:12.6")) {
            postgres.start();
            migrate(postgres);

            SqlConnectionManager connectionManager = new SqlConnectionManager(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
            AuctionsRepository auctionsRepository = new AuctionsRepository(connectionManager);

            Auction auction = testAuctionObject(1);
            Auction newAuction = auctionsRepository.add(auction);

            auction.setLastconsensustimestamp("updatedTimestamp");
            auction.setWinningaccount("updatedWinningAccount");
            auction.setWinningbid(100L);
            auction.setWinningtimestamp("updatedWinningTimestamp");
            auction.setWinningtxid("updatedWinningTxId");
            auction.setWinningtxhash("updatedWinningTxHash");

            assertTrue(auctionsRepository.save(auction));

            Auction getAuction = auctionsRepository.getAuction(newAuction.getId());
            assertEquals(auction.getLastconsensustimestamp(), getAuction.getLastconsensustimestamp());
            assertEquals(auction.getWinningaccount(), getAuction.getWinningaccount());
            assertEquals(auction.getWinningbid(), getAuction.getWinningbid());
            assertEquals(auction.getWinningtimestamp(), getAuction.getWinningtimestamp());
            assertEquals(auction.getWinningtxid(), getAuction.getWinningtxid());
            assertEquals(auction.getWinningtxhash(), getAuction.getWinningtxhash());
        }
    }
    @Test
    public void openAndPendingAuctionsTest() {
        try (PostgreSQLContainer<?> postgres = new PostgreSQLContainer("postgres:12.6")) {
            postgres.start();
            migrate(postgres);

            SqlConnectionManager connectionManager = new SqlConnectionManager(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
            AuctionsRepository auctionsRepository = new AuctionsRepository(connectionManager);

            @Var Auction auction = testAuctionObject(1);
            auction.setStatus(Auction.transfer());
            auctionsRepository.createComplete(auction);

            auction = testAuctionObject(2);
            auction.setStatus(Auction.pending());
            auctionsRepository.createComplete(auction);

            auction = testAuctionObject(3);
            auction.setStatus(Auction.ended());
            auctionsRepository.createComplete(auction);

            auction = testAuctionObject(4);
            auction.setStatus(Auction.closed());
            auctionsRepository.createComplete(auction);

            auction = testAuctionObject(5);
            auction.setStatus(Auction.active());
            auctionsRepository.createComplete(auction);

            Map<String, Integer> openPending = auctionsRepository.openAndPendingAuctions();

            assertEquals(2, openPending.size());
        }
    }

    @Test
    public void getAuctionsListTest() {
        try (PostgreSQLContainer<?> postgres = new PostgreSQLContainer("postgres:12.6")) {
            postgres.start();
            migrate(postgres);

            SqlConnectionManager connectionManager = new SqlConnectionManager(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
            AuctionsRepository auctionsRepository = new AuctionsRepository(connectionManager);

            int testCount = 5;
            int[] ids = new int[testCount];

            for (int i=0; i < testCount; i++) {
                @Var Auction auction = testAuctionObject(i);
                auction = auctionsRepository.createComplete(auction);
                ids[i] = auction.getId();

            }
            List<Auction> auctions = auctionsRepository.getAuctionsList();

            for (int i=0; i < testCount; i++) {

                Auction auction = auctions.get(i);
                Auction testAuction = testAuctionObject(i);

                assertEquals(ids[i], auction.getId());

                assertEquals(testAuction.getWinningbid(), auction.getWinningbid());
                assertEquals(testAuction.getWinningaccount(), auction.getWinningaccount());
                assertEquals(testAuction.getWinningtimestamp(), auction.getWinningtimestamp());
                assertEquals(testAuction.getTokenid(), auction.getTokenid());
                assertEquals(testAuction.getAuctionaccountid(), auction.getAuctionaccountid());
                assertEquals(testAuction.getEndtimestamp(), auction.getEndtimestamp());
                assertEquals(testAuction.getReserve(), auction.getReserve());
                assertEquals(testAuction.getStatus(), auction.getStatus());
                assertEquals(true, auction.getWinnerCanBid());
                assertEquals(testAuction.getWinningtxid(), auction.getWinningtxid());
                assertEquals(testAuction.getWinningtxhash(), auction.getWinningtxhash());
                assertEquals(testAuction.getTokenimage(), auction.getTokenimage());
                assertEquals(testAuction.getMinimumbid(), auction.getMinimumbid());
                assertEquals(testAuction.getStarttimestamp(), auction.getStarttimestamp());
                assertEquals(testAuction.getTransfertxid(), auction.getTransfertxid());
                assertEquals(testAuction.getTransfertxhash(), auction.getTransfertxhash());
                assertEquals(testAuction.getLastconsensustimestamp(), auction.getLastconsensustimestamp());
            }
        }
    }
    @Test
    public void deleteAllAuctionsTest() {
        try (PostgreSQLContainer<?> postgres = new PostgreSQLContainer("postgres:12.6")) {
            postgres.start();
            migrate(postgres);

            SqlConnectionManager connectionManager = new SqlConnectionManager(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
            AuctionsRepository auctionsRepository = new AuctionsRepository(connectionManager);

            int testCount = 2;

            for (int i=0; i < testCount; i++) {
                Auction auction = testAuctionObject(i);
                auctionsRepository.add(auction);
            }
            @Var List<Auction> auctions = auctionsRepository.getAuctionsList();
            assertEquals(testCount, auctions.size());
            auctionsRepository.deleteAllAuctions();;
            auctions = auctionsRepository.getAuctionsList();
            assertEquals(0, auctions.size());
        }
    }
    @Test
    public void duplicateAuctionTest() {
        try (PostgreSQLContainer<?> postgres = new PostgreSQLContainer("postgres:12.6")) {
            postgres.start();
            migrate(postgres);

            SqlConnectionManager connectionManager = new SqlConnectionManager(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
            AuctionsRepository auctionsRepository = new AuctionsRepository(connectionManager);

            Auction auction = testAuctionObject(1);
            auctionsRepository.add(auction);
            auctionsRepository.add(auction);

            List<Auction> auctions = auctionsRepository.getAuctionsList();
            assertEquals(1, auctions.size());
        }
    }
}
