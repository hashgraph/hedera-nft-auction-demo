package com.hedera.demo.auction.node.test.integration.database;

import com.google.errorprone.annotations.Var;
import com.hedera.demo.auction.node.app.SqlConnectionManager;
import com.hedera.demo.auction.node.app.domain.Auction;
import com.hedera.demo.auction.node.app.repository.AuctionsRepository;
import com.hedera.demo.auction.node.test.integration.AbstractIntegrationTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AuctionDatabaseIntegrationTest extends AbstractIntegrationTest {

    public AuctionDatabaseIntegrationTest() {
    }

    private PostgreSQLContainer postgres;
    private AuctionsRepository auctionsRepository;
    private Auction auction;

    @BeforeAll
    public void beforeAll() {
        PostgreSQLContainer<?> postgres = new PostgreSQLContainer("postgres:12.6");
        postgres.start();
        migrate(postgres);
        SqlConnectionManager connectionManager = new SqlConnectionManager(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
        auctionsRepository = new AuctionsRepository(connectionManager);
        this.postgres = postgres;
    }
    @AfterAll
    public void afterAll() {
        this.postgres.close();
    }

    @BeforeEach
    public void beforeEach() throws SQLException {
        auction = testAuctionObject(1);
        auction = auctionsRepository.add(auction);
    }

    @AfterEach
    public void afterEach() throws SQLException {
        auctionsRepository.deleteAllAuctions();
    }

    @Test
    public void addAuctionTest() throws Exception {

        assertNotEquals(0, auction.getId());

        Auction getAuction = auctionsRepository.getAuction(auction.getId());

        testNewAuction(auction, getAuction);

    }

    @Test
    public void setAuctionActiveTest() throws Exception {

        auctionsRepository.setActive(auction, auction.getTokenowneraccount(), auction.getStarttimestamp());
        Auction getAuction = auctionsRepository.getAuction(auction.getId());

        assertEquals(Auction.ACTIVE, getAuction.getStatus());
        assertEquals(auction.getStarttimestamp(), getAuction.getStarttimestamp());

    }

    @Test
    public void setAuctionTransferringTest() throws Exception {

        auctionsRepository.setTransferPending(auction.getTokenid());
        Auction getAuction = auctionsRepository.getAuction(auction.getId());

        assertEquals(Auction.TRANSFER_STATUS_PENDING, getAuction.getTransferstatus());
        assertTrue(getAuction.isTransferPending());
    }

    @Test
    public void setTransferTransactionTest() throws Exception {
        auctionsRepository.setTransferTransactionByTokenId(auction.getTokenid(), auction.getTransfertxid(), auction.getTransfertxhash());
        Auction getAuction = auctionsRepository.getAuction(auction.getId());

        assertEquals(auction.getTransfertxid(), getAuction.getTransfertxid());
        assertEquals(auction.getTransfertxhash(), getAuction.getTransfertxhash());
        assertTrue(getAuction.isEnded());
        assertTrue(getAuction.isTransferComplete());
    }

    @Test
    public void setClosedByAuctionTest() throws Exception {

        auction = auctionsRepository.setClosed(auction);
        assertEquals(Auction.CLOSED, auction.getStatus());

        Auction getAuction = auctionsRepository.getAuction(auction.getId());
        assertEquals(Auction.CLOSED, getAuction.getStatus());
    }

    @Test
    public void setClosedByAuctionIdTest() throws Exception {

        auctionsRepository.setClosed(auction.getId());

        Auction getAuction = auctionsRepository.getAuction(auction.getId());
        assertEquals(Auction.CLOSED, getAuction.getStatus());
    }

    @Test
    public void saveTest() throws Exception {

        auction.setLastconsensustimestamp("updatedTimestamp");
        auction.setWinningaccount("updatedWinningAccount");
        auction.setWinningbid(100L);
        auction.setWinningtimestamp("updatedWinningTimestamp");
        auction.setWinningtxid("updatedWinningTxId");
        auction.setWinningtxhash("updatedWinningTxHash");

        auctionsRepository.save(auction);

        Auction getAuction = auctionsRepository.getAuction(auction.getId());
        assertEquals(auction.getLastconsensustimestamp(), getAuction.getLastconsensustimestamp());
        assertEquals(auction.getWinningaccount(), getAuction.getWinningaccount());
        assertEquals(auction.getWinningbid(), getAuction.getWinningbid());
        assertEquals(auction.getWinningtimestamp(), getAuction.getWinningtimestamp());
        assertEquals(auction.getWinningtxid(), getAuction.getWinningtxid());
        assertEquals(auction.getWinningtxhash(), getAuction.getWinningtxhash());

    }

    @Test
    public void openAndPendingAuctionsTest() throws SQLException {

        auctionsRepository.deleteAllAuctions();

        auction = testAuctionObject(2);
        auction.setStatus(Auction.PENDING);
        auctionsRepository.createComplete(auction);

        auction = testAuctionObject(3);
        auction.setStatus(Auction.ENDED);
        auctionsRepository.createComplete(auction);

        auction = testAuctionObject(4);
        auction.setStatus(Auction.CLOSED);
        auctionsRepository.createComplete(auction);

        auction = testAuctionObject(5);
        auction.setStatus(Auction.ACTIVE);
        auctionsRepository.createComplete(auction);

        Map<String, Integer> openPending = auctionsRepository.openAndPendingAuctions();

        assertEquals(2, openPending.size());
    }

    @Test
    public void getAuctionsListTest() throws SQLException {
        int testCount = 5;
        int[] ids = new int[testCount];

        auctionsRepository.deleteAllAuctions();

        for (int i=0; i < testCount; i++) {
            @Var Auction auctionTest = testAuctionObject(i);
            auctionTest = auctionsRepository.createComplete(auctionTest);
            ids[i] = auctionTest.getId();

        }
        List<Auction> auctions = auctionsRepository.getAuctionsList();

        for (int i=0; i < testCount; i++) {

            Auction auctionToTest = auctions.get(i);
            Auction testAuction = testAuctionObject(i);

            assertEquals(ids[i], auctionToTest.getId());

            assertEquals(testAuction.getWinningbid(), auctionToTest.getWinningbid());
            assertEquals(testAuction.getWinningaccount(), auctionToTest.getWinningaccount());
            assertEquals(testAuction.getWinningtimestamp(), auctionToTest.getWinningtimestamp());
            assertEquals(testAuction.getTokenid(), auctionToTest.getTokenid());
            assertEquals(testAuction.getAuctionaccountid(), auctionToTest.getAuctionaccountid());
            assertEquals(testAuction.getEndtimestamp(), auctionToTest.getEndtimestamp());
            assertEquals(testAuction.getReserve(), auctionToTest.getReserve());
            assertEquals(testAuction.getStatus(), auctionToTest.getStatus());
            assertEquals(testAuction.getWinnerCanBid(), auctionToTest.getWinnerCanBid());
            assertEquals(testAuction.getWinningtxid(), auctionToTest.getWinningtxid());
            assertEquals(testAuction.getWinningtxhash(), auctionToTest.getWinningtxhash());
            assertEquals(testAuction.getTokenimage(), auctionToTest.getTokenimage());
            assertEquals(testAuction.getMinimumbid(), auctionToTest.getMinimumbid());
            assertEquals(testAuction.getStarttimestamp(), auctionToTest.getStarttimestamp());
            assertEquals(testAuction.getTransfertxid(), auctionToTest.getTransfertxid());
            assertEquals(testAuction.getTransfertxhash(), auctionToTest.getTransfertxhash());
            assertEquals(testAuction.getLastconsensustimestamp(), auctionToTest.getLastconsensustimestamp());
            assertEquals(testAuction.getTransferstatus(), auctionToTest.getTransferstatus());
            assertEquals(testAuction.getTitle(), auctionToTest.getTitle());
            assertEquals(testAuction.getDescription(), auctionToTest.getDescription());
        }
    }
    @Test
    public void deleteAllAuctionsTest() throws SQLException {
        int testCount = 2;

        auctionsRepository.deleteAllAuctions();

        for (int i=0; i < testCount; i++) {
            Auction auctionCreate = testAuctionObject(i);
            auctionsRepository.add(auctionCreate);
        }
        @Var List<Auction> auctions = auctionsRepository.getAuctionsList();
        assertEquals(testCount, auctions.size());
        auctionsRepository.deleteAllAuctions();;
        auctions = auctionsRepository.getAuctionsList();
        assertEquals(0, auctions.size());
    }

    @Test
    public void duplicateAuctionTest() throws SQLException {
        // adding to the @beforeEach auction that's already created
        auctionsRepository.add(auction);

        List<Auction> auctions = auctionsRepository.getAuctionsList();
        assertEquals(1, auctions.size());
    }

    @Test
    public void auctionNotFoundTest() {
        assertThrows(Exception.class, () -> {
            auctionsRepository.getAuction(0);
        });
    }

    @Test
    public void setTransferTimestampTest() throws Exception {
        int auctionId = auction.getId();
        String timestamp = "123.345";
        String timestamp2 = "123123.345";
        auctionsRepository.setTransferTimestamp(auctionId, timestamp);
        @Var Auction checkAuction = auctionsRepository.getAuction(auctionId);
        assertEquals(timestamp, checkAuction.getTransfertimestamp());

        // negative test, should not update
        auctionsRepository.setTransferTimestamp(0, timestamp2);
        checkAuction = auctionsRepository.getAuction(auctionId);
        assertEquals(timestamp, checkAuction.getTransfertimestamp());
    }

    @Test
    public void getAuctionByAccountIdTest() throws Exception {
        Auction testAuction = auctionsRepository.getAuction(auction.getAuctionaccountid());
        assertEquals(auction.getAuctionaccountid(), testAuction.getAuctionaccountid());
    }

    @Test
    public void setTransferInProgressTest() throws Exception {
        auctionsRepository.setTransferInProgress(auction.getTokenid());
        Auction testAuction = auctionsRepository.getAuction(auction.getId());
        assertEquals(Auction.TRANSFER_STATUS_IN_PROGRESS, testAuction.getTransferstatus());
    }

    @Test
    public void setTransferTransactionByAuctionIdTest() throws Exception {
        auctionsRepository.setTransferTransactionByAuctionId(auction.getId(), "transactionId", "transactionHash");
        Auction testAuction = auctionsRepository.getAuction(auction.getId());
        assertEquals(Auction.TRANSFER_STATUS_COMPLETE, testAuction.getTransferstatus());
        assertEquals("transactionId", testAuction.getTransfertxid());
        assertEquals("transactionHash", testAuction.getTransfertxhash());
        assertEquals(Auction.ENDED, testAuction.getStatus());
    }

    @Test
    public void setEndedTest() throws Exception {
        auctionsRepository.setEnded(auction.getId());
        Auction testAuction = auctionsRepository.getAuction(auction.getId());
        assertEquals(Auction.ENDED, testAuction.getStatus());
    }

}
