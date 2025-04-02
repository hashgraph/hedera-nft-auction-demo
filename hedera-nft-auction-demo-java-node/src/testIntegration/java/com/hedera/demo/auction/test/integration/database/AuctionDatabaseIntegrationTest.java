package com.hedera.demo.auction.test.integration.database;

import com.google.errorprone.annotations.Var;
import com.hedera.demo.auction.app.SqlConnectionManager;
import com.hedera.demo.auction.app.domain.Auction;
import com.hedera.demo.auction.app.repository.AuctionsRepository;
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
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AuctionDatabaseIntegrationTest extends AbstractIntegrationTest {

    public AuctionDatabaseIntegrationTest() {
    }

    private PostgreSQLContainer<?> postgres;
    private AuctionsRepository auctionsRepository;
    private Auction auction;

    @BeforeAll
    public void beforeAll() {
        PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("POSTGRES_CONTAINER_VERSION");
        postgres.start();
        migrate(postgres);
        var connectionManager = new SqlConnectionManager(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
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

        auctionsRepository.setActive(auction, auction.getTokenOwnerAccount(), auction.getStartTimestamp());
        Auction getAuction = auctionsRepository.getAuction(auction.getId());

        assertEquals(Auction.ACTIVE, getAuction.getStatus());
        assertEquals(auction.getStartTimestamp(), getAuction.getStartTimestamp());

    }

    @Test
    public void setAuctionTransferringTest() throws Exception {

        auctionsRepository.setTransferPending(auction.getTokenId());
        Auction getAuction = auctionsRepository.getAuction(auction.getId());

        assertEquals(Auction.TRANSFER_STATUS_PENDING, getAuction.getTransferStatus());
        assertTrue(getAuction.isTransferPending());
    }

    @Test
    public void setTransferTransactionTest() throws Exception {
        auctionsRepository.setTransferPending(auction.getTokenId());
        auctionsRepository.setTransferInProgress(auction.getTokenId());
        auctionsRepository.setTransferTransactionByTokenId(auction.getTokenId(), auction.getTransferTxId(), auction.getTransferTxHash());
        Auction getAuction = auctionsRepository.getAuction(auction.getId());

        assertEquals(auction.getTransferTxId(), getAuction.getTransferTxId());
        assertEquals(auction.getTransferTxHash(), getAuction.getTransferTxHash());
        assertTrue(getAuction.isEnded());
        assertTrue(getAuction.isTransferComplete());
    }

    @Test
    public void setClosedByAuctionTest() throws Exception {

        auctionsRepository.setActive(auction, auction.getTokenOwnerAccount(), auction.getStartTimestamp());
        auction = auctionsRepository.setClosed(auction);
        assertEquals(Auction.CLOSED, auction.getStatus());

        Auction getAuction = auctionsRepository.getAuction(auction.getId());
        assertEquals(Auction.CLOSED, getAuction.getStatus());
    }

    @Test
    public void setClosedByAuctionIdTest() throws Exception {

        auctionsRepository.setActive(auction, auction.getTokenOwnerAccount(), auction.getStartTimestamp());
        auctionsRepository.setClosed(auction.getId());

        Auction getAuction = auctionsRepository.getAuction(auction.getId());
        assertEquals(Auction.CLOSED, getAuction.getStatus());
    }

    @Test
    public void saveTest() throws Exception {

        auction.setLastConsensusTimestamp("updatedTimestamp");
        auction.setWinningAccount("updatedWinningAccount");
        auction.setWinningBid(100L);
        auction.setWinningTimestamp("updatedWinningTimestamp");
        auction.setWinningTxId("updatedWinningTxId");
        auction.setWinningTxHash("updatedWinningTxHash");

        auctionsRepository.save(auction);

        Auction getAuction = auctionsRepository.getAuction(auction.getId());
        assertEquals(auction.getLastConsensusTimestamp(), getAuction.getLastConsensusTimestamp());
        assertEquals(auction.getWinningAccount(), getAuction.getWinningAccount());
        assertEquals(auction.getWinningBid(), getAuction.getWinningBid());
        assertEquals(auction.getWinningTimestamp(), getAuction.getWinningTimestamp());
        assertEquals(auction.getWinningTxId(), getAuction.getWinningTxId());
        assertEquals(auction.getWinningTxHash(), getAuction.getWinningTxHash());

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

            assertEquals(testAuction.getWinningBid(), auctionToTest.getWinningBid());
            assertEquals(testAuction.getWinningAccount(), auctionToTest.getWinningAccount());
            assertEquals(testAuction.getWinningTimestamp(), auctionToTest.getWinningTimestamp());
            assertEquals(testAuction.getTokenId(), auctionToTest.getTokenId());
            assertEquals(testAuction.getAuctionAccountId(), auctionToTest.getAuctionAccountId());
            assertEquals(testAuction.getEndTimestamp(), auctionToTest.getEndTimestamp());
            assertEquals(testAuction.getReserve(), auctionToTest.getReserve());
            assertEquals(testAuction.getStatus(), auctionToTest.getStatus());
            assertEquals(testAuction.getWinnerCanBid(), auctionToTest.getWinnerCanBid());
            assertEquals(testAuction.getWinningTxId(), auctionToTest.getWinningTxId());
            assertEquals(testAuction.getWinningTxHash(), auctionToTest.getWinningTxHash());
            assertEquals(testAuction.getTokenMetadata(), auctionToTest.getTokenMetadata());
            assertEquals(testAuction.getMinimumBid(), auctionToTest.getMinimumBid());
            assertEquals(testAuction.getStartTimestamp(), auctionToTest.getStartTimestamp());
            assertEquals(testAuction.getTransferTxId(), auctionToTest.getTransferTxId());
            assertEquals(testAuction.getTransferTxHash(), auctionToTest.getTransferTxHash());
            assertEquals(testAuction.getLastConsensusTimestamp(), auctionToTest.getLastConsensusTimestamp());
            assertEquals(testAuction.getTransferStatus(), auctionToTest.getTransferStatus());
            assertEquals(testAuction.getTitle(), auctionToTest.getTitle());
            assertEquals(testAuction.getDescription(), auctionToTest.getDescription());
            assertEquals(testAuction.getCreateAuctionTxId(), auctionToTest.getCreateAuctionTxId());
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
    public void getAuctionByAccountIdTest() throws Exception {
        Auction testAuction = auctionsRepository.getAuction(auction.getAuctionAccountId());
        assertNotNull(testAuction);
        assertNotNull(testAuction.getAuctionAccountId());
        assertEquals(auction.getAuctionAccountId(), testAuction.getAuctionAccountId());
    }

    @Test
    public void setTransferInProgressTest() throws Exception {
        auctionsRepository.setTransferPending(auction.getTokenId());
        auctionsRepository.setTransferInProgress(auction.getTokenId());
        Auction testAuction = auctionsRepository.getAuction(auction.getId());
        assertEquals(Auction.TRANSFER_STATUS_IN_PROGRESS, testAuction.getTransferStatus());
    }

    @Test
    public void setTransferTransactionByAuctionIdTest() throws Exception {
        auctionsRepository.setActive(auction, auction.getTokenOwnerAccount(), auction.getStartTimestamp());
        auctionsRepository.setClosed(auction.getId());
        auctionsRepository.setTransferTransactionByAuctionId(auction.getId(), "transactionId", "transactionHash");
        Auction testAuction = auctionsRepository.getAuction(auction.getId());
        assertEquals(Auction.TRANSFER_STATUS_COMPLETE, testAuction.getTransferStatus());
        assertEquals("transactionId", testAuction.getTransferTxId());
        assertEquals("transactionHash", testAuction.getTransferTxHash());
        assertEquals(Auction.ENDED, testAuction.getStatus());
    }

    @Test
    public void setEndedTest() throws Exception {
        auctionsRepository.setActive(auction, auction.getTokenOwnerAccount(), auction.getStartTimestamp());
        auctionsRepository.setEnded(auction.getId());
        Auction testAuction = auctionsRepository.getAuction(auction.getId());
        assertEquals(Auction.ENDED, testAuction.getStatus());
    }

    @Test
    public void auctionMustBeNotBeClosedBeforeActive() throws Exception {
        auctionsRepository.setActive(auction, auction.getTokenOwnerAccount(), auction.getStartTimestamp());
        auctionsRepository.setClosed(auction.getId());
        try {
            auctionsRepository.setActive(auction, auction.getTokenOwnerAccount(), auction.getStartTimestamp());
        } catch (Exception e) {
            assertEquals("auction cannot be set to ACTIVE, it's not PENDING", e.getMessage());
        }
        Auction testAuction = auctionsRepository.getAuction(auction.getId());
        assertEquals(Auction.CLOSED, testAuction.getStatus());
    }

    @Test
    public void auctionMustBeNotBeEndedBeforeActive() throws Exception {
        auctionsRepository.setActive(auction, auction.getTokenOwnerAccount(), auction.getStartTimestamp());
        auctionsRepository.setEnded(auction.getId());
        try {
            auctionsRepository.setActive(auction, auction.getTokenOwnerAccount(), auction.getStartTimestamp());
        } catch (Exception e) {
            assertEquals("auction cannot be set to ACTIVE, it's not PENDING", e.getMessage());
        }
        Auction testAuction = auctionsRepository.getAuction(auction.getId());
        assertEquals(Auction.ENDED, testAuction.getStatus());
    }

    @Test
    public void auctionMustBeActiveBeforeClosed() throws Exception {
        try {
            auctionsRepository.setClosed(auction.getId());
        } catch (Exception e) {
            // do nothing
        }
        Auction testAuction = auctionsRepository.getAuction(auction.getId());
        assertEquals(Auction.PENDING, testAuction.getStatus());
    }

    @Test
    public void auctionMustBeClosedBeforeEnded() throws Exception {
        try {
            auctionsRepository.setEnded(auction.getId());
        } catch (Exception e) {
            // do nothing
        }
        Auction testAuction = auctionsRepository.getAuction(auction.getId());
        assertEquals(Auction.PENDING, testAuction.getStatus());
    }
}
