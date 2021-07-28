package com.hedera.demo.auction.test.integration.bidwatcher;

import com.google.errorprone.annotations.Var;
import com.hedera.demo.auction.app.HederaClient;
import com.hedera.demo.auction.app.SqlConnectionManager;
import com.hedera.demo.auction.app.Utils;
import com.hedera.demo.auction.BidsWatcher;
import com.hedera.demo.auction.app.domain.Auction;
import com.hedera.demo.auction.app.domain.Bid;
import com.hedera.demo.auction.app.mirrormapping.MirrorTransactions;
import com.hedera.demo.auction.app.repository.AuctionsRepository;
import com.hedera.demo.auction.app.repository.BidsRepository;
import com.hedera.demo.auction.test.integration.AbstractIntegrationTest;
import com.hedera.demo.auction.test.integration.HederaJson;
import io.vertx.core.json.JsonObject;
import org.apache.commons.codec.binary.Hex;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BidWatcherIntegrationTest extends AbstractIntegrationTest {

    public BidWatcherIntegrationTest() throws Exception {
    }

    private PostgreSQLContainer postgres;
    private AuctionsRepository auctionsRepository;
    private BidsRepository bidsRepository;
    private final HederaClient hederaClient = new HederaClient();
    private BidsWatcher bidWatcher;
    private Auction auction = testAuctionObject(1);
    private static final long bidAmount = 1000000000;
    private static final String fromAccount = "0.0.100";

    @BeforeAll
    public void beforeAll() throws Exception {
        PostgreSQLContainer<?> postgres = new PostgreSQLContainer("postgres:12.6");
        postgres.start();
        migrate(postgres);
        SqlConnectionManager connectionManager = new SqlConnectionManager(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
        auctionsRepository = new AuctionsRepository(connectionManager);
        bidsRepository = new BidsRepository(connectionManager);
        this.postgres = postgres;
    }

    @AfterAll
    public void afterAll() {
        this.postgres.close();
    }

    @BeforeEach
    public void beforeEach() throws Exception {
        auction.setWinningbid(0L);
        auction = auctionsRepository.add(auction);
        bidWatcher = new BidsWatcher(hederaClient, auctionsRepository, auction.getId(), 5000, /*runOnce= */false);
    }

    @AfterEach
    public void afterEach() throws SQLException {
        bidWatcher.stop();
        bidsRepository.deleteAllBids();
        auctionsRepository.deleteAllAuctions();
    }

    @Test
    public void testMemo() {

        assertTrue(bidWatcher.checkMemos("CREATEAUCTION"));
        assertTrue(bidWatcher.checkMemos("FUNDACCOUNT"));
        assertTrue(bidWatcher.checkMemos("TRANSFERTOAUCTION"));
        assertTrue(bidWatcher.checkMemos("ASSOCIATE"));
        assertTrue(bidWatcher.checkMemos("AUCTION REFUND"));

        assertTrue(bidWatcher.checkMemos("createauction"));
        assertTrue(bidWatcher.checkMemos("fundaccount"));
        assertTrue(bidWatcher.checkMemos("transfertoauction"));
        assertTrue(bidWatcher.checkMemos("associate"));
        assertTrue(bidWatcher.checkMemos("auction refund"));

        assertFalse(bidWatcher.checkMemos(""));
        assertFalse(bidWatcher.checkMemos("memo"));
    }

    @Test
    public void testAuctionTimestamp() throws Exception {

        JsonObject transaction1 = HederaJson.singleTransaction();
        transaction1.put("consensus_timestamp", "1");
        JsonObject transaction2 = HederaJson.singleTransaction();
        transaction2.put("consensus_timestamp", "2");

        @Var JsonObject response = HederaJson.mirrorTransactions(transaction1);
        response = HederaJson.mirrorTransactions(transaction2, response);
        MirrorTransactions mirrorTransactions = response.mapTo(MirrorTransactions.class);
        bidWatcher.handleResponse(mirrorTransactions);

        Auction updatedAuction = auctionsRepository.getAuction(auction.getId());
        assertEquals("2", updatedAuction.getLastconsensustimestamp());
    }

    @Test
    public void testBidFromAuctionAccountId() throws Exception {

        BidsWatcher bidsWatcher = new BidsWatcher(hederaClient, auctionsRepository, auction.getId(), 5000, /*runOnce= */false);

        JsonObject transaction = HederaJson.singleTransaction();
        String transactionId = auction.getAuctionaccountid().concat("-1617786650-796134000");
        transaction.put("transaction_id", transactionId);

        JsonObject response = HederaJson.mirrorTransactions(transaction);

        MirrorTransactions mirrorTransactions = response.mapTo(MirrorTransactions.class);
        bidsWatcher.handleResponse(mirrorTransactions);

        List<Bid> bids = bidsRepository.getBidsList();
        assertEquals(0, bids.size());

        testUpdatedAuctionNotChanged(auction);
        bidsWatcher.stop();
    }

    @Test
    public void testBidUnsuccessfulTransaction() throws Exception {

        BidsWatcher bidsWatcher = new BidsWatcher(hederaClient, auctionsRepository, auction.getId(), 5000, /*runOnce= */false);

        JsonObject transaction = HederaJson.singleTransaction();
        transaction.put("result", "Error");

        JsonObject response = HederaJson.mirrorTransactions(transaction);

        MirrorTransactions mirrorTransactions = response.mapTo(MirrorTransactions.class);
        bidsWatcher.handleResponse(mirrorTransactions);

        List<Bid> bids = bidsRepository.getBidsList();
        assertEquals(0, bids.size());

        testUpdatedAuctionNotChanged(auction);
        bidsWatcher.stop();
    }

    @Test
    public void testBidMemo() throws Exception {

        BidsWatcher bidsWatcher = new BidsWatcher(hederaClient, auctionsRepository, auction.getId(), 5000, /*runOnce= */false);

        JsonObject transaction = HederaJson.singleTransaction();
        String memo = "CREATEAUCTION";
        String memoBase64 = Base64.getEncoder().encodeToString(memo.getBytes(StandardCharsets.UTF_8));
        transaction.put("memo_base64", memoBase64);

        JsonObject response = HederaJson.mirrorTransactions(transaction);
        MirrorTransactions mirrorTransactions = response.mapTo(MirrorTransactions.class);
        bidsWatcher.handleResponse(mirrorTransactions);

        List<Bid> bids = bidsRepository.getBidsList();
        assertEquals(0, bids.size());

        testUpdatedAuctionNotChanged(auction);
        bidsWatcher.stop();
    }

    @Test
    public void testPastEnd() throws Exception {

        BidsWatcher bidsWatcher = new BidsWatcher(hederaClient, auctionsRepository, auction.getId(), 5000, /*runOnce= */false);

        JsonObject transaction = HederaJson.transactionWithTransfers(fromAccount, auction.getAuctionaccountid(), bidAmount );
        transaction.put("consensus_timestamp", "z");

        JsonObject response = HederaJson.mirrorTransactions(transaction);
        MirrorTransactions mirrorTransactions = response.mapTo(MirrorTransactions.class);
        bidsWatcher.handleResponse(mirrorTransactions);

        List<Bid> bids = bidsRepository.getBidsList();
        assertEquals(1, bids.size());
        testBidValues("Auction is closed", bidAmount, auction, fromAccount, transaction, bids.get(0));
        testUpdatedAuctionNotChanged(auction);
        bidsWatcher.stop();
    }

    @Test
    public void testBeforeStart() throws Exception {

        auctionsRepository.setActive(auction, auction.getTokenowneraccount(), "z");
        BidsWatcher bidsWatcher = new BidsWatcher(hederaClient, auctionsRepository, auction.getId(), 5000, /*runOnce= */false);

        JsonObject transaction = HederaJson.transactionWithTransfers("0.0.100", auction.getAuctionaccountid(), bidAmount );
        transaction.put("consensus_timestamp", "a");

        JsonObject response = HederaJson.mirrorTransactions(transaction);
        MirrorTransactions mirrorTransactions = response.mapTo(MirrorTransactions.class);
        bidsWatcher.handleResponse(mirrorTransactions);

        List<Bid> bids = bidsRepository.getBidsList();
        assertEquals(1, bids.size());

        testBidValues("Auction has not started yet", bidAmount, auction, fromAccount, transaction, bids.get(0));
        testUpdatedAuctionNotChanged(auction);
        bidsWatcher.stop();

    }

    @Test
    public void testWinnerCantBid() throws Exception {

        auctionsRepository.deleteAllAuctions();
        // if index below set to 0, winner can't bid
        @Var Auction winnerCantBidAuction = testAuctionObject(0);
        winnerCantBidAuction = auctionsRepository.add(winnerCantBidAuction);
        auctionsRepository.setActive(winnerCantBidAuction, winnerCantBidAuction.getTokenowneraccount(), "a");

        BidsWatcher bidsWatcher = new BidsWatcher(hederaClient, auctionsRepository, winnerCantBidAuction.getId(), 5000, /*runOnce= */false);

        // create a first bid and current winner (bidAmount - 10)
        JsonObject transaction1 = HederaJson.transactionWithTransfers(fromAccount, winnerCantBidAuction.getAuctionaccountid(), bidAmount-10 );
        transaction1.put("consensus_timestamp", "b");
        @Var JsonObject response = HederaJson.mirrorTransactions(transaction1);
        @Var MirrorTransactions mirrorTransactions = response.mapTo(MirrorTransactions.class);
        bidsWatcher.handleResponse(mirrorTransactions);

        // create a second bid for current winner (bidAmount)
        JsonObject transaction2 = HederaJson.transactionWithTransfers(fromAccount, winnerCantBidAuction.getAuctionaccountid(), bidAmount );
        transaction2.put("consensus_timestamp", "c");
        response = HederaJson.mirrorTransactions(transaction2);
        mirrorTransactions = response.mapTo(MirrorTransactions.class);
        bidsWatcher.handleResponse(mirrorTransactions);

        List<Bid> bids = bidsRepository.getBidsList();
        assertEquals(2, bids.size());

        winnerCantBidAuction = auctionsRepository.getAuction(winnerCantBidAuction.getId());

        testBidValues("Winner can't bid again", bidAmount, winnerCantBidAuction, fromAccount, transaction2, bids.get(1));

        // check auction has not been updated with second bid
        assertEquals(transaction1.getString("consensus_timestamp"), winnerCantBidAuction.getWinningtimestamp());
        assertEquals(fromAccount, winnerCantBidAuction.getWinningaccount());
        assertEquals(bidAmount-10, winnerCantBidAuction.getWinningbid());
        assertEquals(transaction1.getString("transaction_id"), winnerCantBidAuction.getWinningtxid());
        String txHash = Utils.base64toStringHex(transaction1.getString("transaction_hash"));
        assertEquals(txHash, winnerCantBidAuction.getWinningtxhash());
        bidsWatcher.stop();
    }

    @Test
    public void testWinnerCanBid() throws Exception {
        auctionsRepository.deleteAllAuctions();
        // if index below set to 1, winner can bid
        @Var Auction winnerCanBidAuction = testAuctionObject(1);
        winnerCanBidAuction.setMinimumbid(1L);
        winnerCanBidAuction = auctionsRepository.add(winnerCanBidAuction);
        auctionsRepository.setActive(winnerCanBidAuction, winnerCanBidAuction.getTokenowneraccount(), "a");

        BidsWatcher bidsWatcher = new BidsWatcher(hederaClient, auctionsRepository, winnerCanBidAuction.getId(), 5000, /*runOnce= */false);

        // create a first bid and current winner (bidAmount - 10)
        JsonObject transaction1 = HederaJson.transactionWithTransfers(fromAccount, winnerCanBidAuction.getAuctionaccountid(), bidAmount-10 );
        transaction1.put("consensus_timestamp", "b");
        @Var JsonObject response = HederaJson.mirrorTransactions(transaction1);

        // create a second bid for current winner (bidAmount)
        JsonObject transaction2 = HederaJson.transactionWithTransfers(fromAccount, winnerCanBidAuction.getAuctionaccountid(), bidAmount );
        transaction2.put("consensus_timestamp", "c");
        response = HederaJson.mirrorTransactions(transaction2, response);
        MirrorTransactions mirrorTransactions = response.mapTo(MirrorTransactions.class);
        bidsWatcher.handleResponse(mirrorTransactions);

        List<Bid> bids = bidsRepository.getBidsList();
        assertEquals(2, bids.size());

        winnerCanBidAuction = auctionsRepository.getAuction(winnerCanBidAuction.getId());

        testBidValues("", bidAmount, winnerCanBidAuction, fromAccount, transaction2, bids.get(1));

        // check auction has been updated with second bid
        assertEquals(transaction2.getString("consensus_timestamp"), winnerCanBidAuction.getWinningtimestamp());
        assertEquals(fromAccount, winnerCanBidAuction.getWinningaccount());
        assertEquals(bidAmount, winnerCanBidAuction.getWinningbid());
        assertEquals(transaction2.getString("transaction_id"), winnerCanBidAuction.getWinningtxid());
        String txHash = Utils.base64toStringHex(transaction2.getString("transaction_hash"));
        assertEquals(txHash, winnerCanBidAuction.getWinningtxhash());
        bidsWatcher.stop();
    }

    @Test
    public void testBidIncreaseTooSmallFirstBid() throws Exception {
        auctionsRepository.deleteAllAuctions();
        @Var Auction smallIncreaseAuction = testAuctionObject(1);
        smallIncreaseAuction.setReserve(0L);
        smallIncreaseAuction.setWinningbid(0L);
        smallIncreaseAuction.setMinimumbid(bidAmount * 2);
        smallIncreaseAuction = auctionsRepository.add(smallIncreaseAuction);
        auctionsRepository.setActive(smallIncreaseAuction, smallIncreaseAuction.getTokenowneraccount(), "a");

        BidsWatcher bidsWatcher = new BidsWatcher(hederaClient, auctionsRepository,  smallIncreaseAuction.getId(), 5000, /*runOnce= */false);

        JsonObject transaction = HederaJson.transactionWithTransfers(fromAccount, smallIncreaseAuction.getAuctionaccountid(), bidAmount);
        transaction.put("consensus_timestamp", "b");
        JsonObject response = HederaJson.mirrorTransactions(transaction);
        MirrorTransactions mirrorTransactions = response.mapTo(MirrorTransactions.class);
        bidsWatcher.handleResponse(mirrorTransactions);

        List<Bid> bids = bidsRepository.getBidsList();
        assertEquals(1, bids.size());

        smallIncreaseAuction = auctionsRepository.getAuction(smallIncreaseAuction.getId());

        testBidValues("Bid increase too small", bidAmount, smallIncreaseAuction, fromAccount, transaction, bids.get(0));

        // check auction has not been updated with bid
        testUpdatedAuctionNotChanged(smallIncreaseAuction);
        bidsWatcher.stop();
    }

    @Test
    public void testBidIncreaseTooSmallSecondBid() throws Exception {
        auctionsRepository.deleteAllAuctions();
        @Var Auction smallIncreaseAuction = testAuctionObject(1);
        smallIncreaseAuction.setReserve(0L);
        smallIncreaseAuction.setWinningbid(0L);
        smallIncreaseAuction.setMinimumbid(100L);
        smallIncreaseAuction = auctionsRepository.add(smallIncreaseAuction);
        auctionsRepository.setActive(smallIncreaseAuction, smallIncreaseAuction.getTokenowneraccount(), "a");

        BidsWatcher bidsWatcher = new BidsWatcher(hederaClient, auctionsRepository, smallIncreaseAuction.getId(), 5000, /*runOnce= */false);

        // create a first bid and current winner (bidAmount)
        JsonObject transaction1 = HederaJson.transactionWithTransfers(fromAccount, smallIncreaseAuction.getAuctionaccountid(), bidAmount );
        transaction1.put("consensus_timestamp", "b");
        @Var JsonObject response = HederaJson.mirrorTransactions(transaction1);
        @Var MirrorTransactions mirrorTransactions = response.mapTo(MirrorTransactions.class);
        bidsWatcher.handleResponse(mirrorTransactions);

        JsonObject transaction2 = HederaJson.transactionWithTransfers(fromAccount, smallIncreaseAuction.getAuctionaccountid(), bidAmount + 10);
        transaction2.put("consensus_timestamp", "c");
        response = HederaJson.mirrorTransactions(transaction2);
        mirrorTransactions = response.mapTo(MirrorTransactions.class);
        bidsWatcher.handleResponse(mirrorTransactions);

        List<Bid> bids = bidsRepository.getBidsList();
        assertEquals(2, bids.size());

        smallIncreaseAuction = auctionsRepository.getAuction(smallIncreaseAuction.getId());

        testBidValues("Bid increase too small", bidAmount+10, smallIncreaseAuction, fromAccount, transaction2, bids.get(1));

        // check auction has not been updated with second bid
        // check auction has been updated with second bid
        assertEquals(transaction1.getString("consensus_timestamp"), smallIncreaseAuction.getWinningtimestamp());
        assertEquals(fromAccount, smallIncreaseAuction.getWinningaccount());
        assertEquals(bidAmount, smallIncreaseAuction.getWinningbid());
        assertEquals(transaction1.getString("transaction_id"), smallIncreaseAuction.getWinningtxid());
        String txHash = Utils.base64toStringHex(transaction1.getString("transaction_hash"));
        assertEquals(txHash, smallIncreaseAuction.getWinningtxhash());
        bidsWatcher.stop();
    }

    @Test
    public void testBidBelowReserve() throws Exception {
        auctionsRepository.deleteAllAuctions();
        @Var Auction belowReserveAuction = testAuctionObject(1);
        belowReserveAuction.setReserve(bidAmount * 2);
        belowReserveAuction.setWinningbid(0L);
        belowReserveAuction = auctionsRepository.add(belowReserveAuction);
        auctionsRepository.setActive(belowReserveAuction, belowReserveAuction.getTokenowneraccount(), "a");

        BidsWatcher bidsWatcher = new BidsWatcher(hederaClient, auctionsRepository, belowReserveAuction.getId(), 5000, /*runOnce= */false);

        JsonObject transaction = HederaJson.transactionWithTransfers(fromAccount, belowReserveAuction.getAuctionaccountid(), bidAmount);
        transaction.put("consensus_timestamp", "c");
        JsonObject response = HederaJson.mirrorTransactions(transaction);
        MirrorTransactions mirrorTransactions = response.mapTo(MirrorTransactions.class);
        bidsWatcher.handleResponse(mirrorTransactions);

        List<Bid> bids = bidsRepository.getBidsList();
        assertEquals(1, bids.size());

        belowReserveAuction = auctionsRepository.getAuction(belowReserveAuction.getId());

        testBidValues("Bid below reserve", bidAmount, belowReserveAuction, fromAccount, transaction, bids.get(0));

        // check auction has not been updated with  bid
        testUpdatedAuctionNotChanged(belowReserveAuction);
        bidsWatcher.stop();
    }

    @Test
    public void testUnderBid() throws Exception {
        auctionsRepository.deleteAllAuctions();
        @Var Auction smallIncreaseAuction = testAuctionObject(1);
        smallIncreaseAuction.setReserve(0L);
        smallIncreaseAuction.setWinningbid(0L);
        smallIncreaseAuction.setMinimumbid(100L);
        smallIncreaseAuction = auctionsRepository.add(smallIncreaseAuction);
        auctionsRepository.setActive(smallIncreaseAuction, smallIncreaseAuction.getTokenowneraccount(), "a");

        BidsWatcher bidsWatcher = new BidsWatcher(hederaClient, auctionsRepository, smallIncreaseAuction.getId(), 5000, /*runOnce= */false);

        // create a first bid and current winner (bidAmount)
        JsonObject transaction1 = HederaJson.transactionWithTransfers(fromAccount, smallIncreaseAuction.getAuctionaccountid(), bidAmount);
        transaction1.put("consensus_timestamp", "b");

        @Var JsonObject response = HederaJson.mirrorTransactions(transaction1);
        @Var MirrorTransactions mirrorTransactions = response.mapTo(MirrorTransactions.class);
        bidsWatcher.handleResponse(mirrorTransactions);

        // create a second bid (bidAmount-10)
        JsonObject transaction2 = HederaJson.transactionWithTransfers(fromAccount, smallIncreaseAuction.getAuctionaccountid(), bidAmount - 10);
        transaction2.put("consensus_timestamp", "c");
        response = HederaJson.mirrorTransactions(transaction2);
        mirrorTransactions = response.mapTo(MirrorTransactions.class);
        bidsWatcher.handleResponse(mirrorTransactions);

        List<Bid> bids = bidsRepository.getBidsList();
        assertEquals(2, bids.size());

        smallIncreaseAuction = auctionsRepository.getAuction(smallIncreaseAuction.getId());

        testBidValues("Under bid", bidAmount-10, smallIncreaseAuction, fromAccount, transaction2, bids.get(1));

        // check auction has not been updated with second bid
        // check auction has been updated with second bid
        assertEquals(transaction1.getString("consensus_timestamp"), smallIncreaseAuction.getWinningtimestamp());
        assertEquals(fromAccount, smallIncreaseAuction.getWinningaccount());
        assertEquals(bidAmount, smallIncreaseAuction.getWinningbid());
        assertEquals(transaction1.getString("transaction_id"), smallIncreaseAuction.getWinningtxid());
        String txHash = Utils.base64toStringHex(transaction1.getString("transaction_hash"));
        assertEquals(txHash, smallIncreaseAuction.getWinningtxhash());
        bidsWatcher.stop();
    }

    @Test
    public void testPriorBidUpdated() throws Exception {
        auctionsRepository.deleteAllAuctions();
        @Var Auction priorBidUpdateAuction = testAuctionObject(1);
        priorBidUpdateAuction.setReserve(0L);
        priorBidUpdateAuction.setWinningbid(0L);
        priorBidUpdateAuction.setMinimumbid(1L);
        priorBidUpdateAuction = auctionsRepository.add(priorBidUpdateAuction);
        auctionsRepository.setActive(priorBidUpdateAuction, priorBidUpdateAuction.getTokenowneraccount(), "a");

        BidsWatcher bidsWatcher = new BidsWatcher(hederaClient, auctionsRepository, priorBidUpdateAuction.getId(), 5000, /*runOnce= */false);

        // create a first bid and current winner (bidAmount -10)
        JsonObject transaction1 = HederaJson.transactionWithTransfers(fromAccount, priorBidUpdateAuction.getAuctionaccountid(), bidAmount-10);
        transaction1.put("consensus_timestamp", "b");
        @Var JsonObject response = HederaJson.mirrorTransactions(transaction1);
        @Var MirrorTransactions mirrorTransactions = response.mapTo(MirrorTransactions.class);
        bidsWatcher.handleResponse(mirrorTransactions);

        // create a second bid and winner (bidAmount)
        JsonObject transaction2 = HederaJson.transactionWithTransfers("winner", priorBidUpdateAuction.getAuctionaccountid(), bidAmount);
        transaction2.put("consensus_timestamp", "c");
        response = HederaJson.mirrorTransactions(transaction2);
        mirrorTransactions = response.mapTo(MirrorTransactions.class);
        bidsWatcher.handleResponse(mirrorTransactions);

        List<Bid> bids = bidsRepository.getBidsList();
        assertEquals(2, bids.size());

        priorBidUpdateAuction = auctionsRepository.getAuction(priorBidUpdateAuction.getId());

        testBidValues("Higher bid received", bidAmount-10, priorBidUpdateAuction, fromAccount, transaction1, bids.get(0));
        testBidValues("", bidAmount, priorBidUpdateAuction, "winner", transaction2, bids.get(1));

        // check auction has been updated with second bid
        assertEquals(transaction2.getString("consensus_timestamp"), priorBidUpdateAuction.getWinningtimestamp());
        assertEquals("winner", priorBidUpdateAuction.getWinningaccount());
        assertEquals(bidAmount, priorBidUpdateAuction.getWinningbid());
        assertEquals(transaction2.getString("transaction_id"), priorBidUpdateAuction.getWinningtxid());
        String txHash = Utils.base64toStringHex(transaction2.getString("transaction_hash"));
        assertEquals(txHash, priorBidUpdateAuction.getWinningtxhash());
        bidsWatcher.stop();
    }

    private void testUpdatedAuctionNotChanged(Auction auction) throws Exception {
        Auction updatedAuction = auctionsRepository.getAuction(auction.getId());

        assertEquals("", updatedAuction.getWinningtimestamp());
        assertEquals("", updatedAuction.getWinningaccount());
        assertEquals(0, updatedAuction.getWinningbid());
        assertEquals("", updatedAuction.getWinningtxid());
        assertEquals("", updatedAuction.getWinningtxhash());
    }

    private static void testBidValues(String testStatus, long testBidAmount, Auction testNewAuction, String testFromAccount, JsonObject testTransaction, Bid testBid) {
        assertEquals(testStatus, testBid.getStatus());
        assertEquals(testBidAmount, testBid.getBidamount());
        assertEquals(testNewAuction.getId(), testBid.getAuctionid());
        assertEquals(testFromAccount, testBid.getBidderaccountid());
        assertEquals(testTransaction.getString("consensus_timestamp"), testBid.getTimestamp());
        assertEquals(testTransaction.getString("transaction_id"), testBid.getTransactionid());
        byte[] txHashBytes = Base64.getDecoder().decode(testTransaction.getString("transaction_hash"));
        assertEquals(Hex.encodeHexString(txHashBytes), testBid.getTransactionhash());
    }
}
