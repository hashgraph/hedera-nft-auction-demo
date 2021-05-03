package com.hedera.demo.auction.node.test.integration.refundchecker;

import com.hedera.demo.auction.node.app.HederaClient;
import com.hedera.demo.auction.node.app.SqlConnectionManager;
import com.hedera.demo.auction.node.app.Utils;
import com.hedera.demo.auction.node.app.domain.Auction;
import com.hedera.demo.auction.node.app.domain.Bid;
import com.hedera.demo.auction.node.app.refundChecker.AbstractRefundChecker;
import com.hedera.demo.auction.node.app.repository.AuctionsRepository;
import com.hedera.demo.auction.node.app.repository.BidsRepository;
import com.hedera.demo.auction.node.test.integration.AbstractIntegrationTest;
import com.hedera.demo.auction.node.test.integration.HederaJson;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class RefundCheckerIntegrationTest extends AbstractIntegrationTest {

    private PostgreSQLContainer postgres;
    private AuctionsRepository auctionsRepository;
    private BidsRepository bidsRepository;
    private final HederaClient hederaClient = HederaClient.emptyTestClient();
    Auction auction;
    JsonObject bidTransaction;
    JsonObject bidTransactions;
    String transactionId;
    String transactionHash;
    RefundTester refundTester;
    Bid bid;

    public RefundCheckerIntegrationTest() throws Exception {
    }

    static class RefundTester extends AbstractRefundChecker {

        protected RefundTester(HederaClient hederaClient, WebClient webClient, BidsRepository bidsRepository, int mirrorQueryFrequency) {
            super(hederaClient, webClient, bidsRepository, mirrorQueryFrequency);
        }
    }

    @BeforeAll
    public void beforeAll() {
        this.postgres = new PostgreSQLContainer("postgres:12.6");
        this.postgres.start();
        migrate(this.postgres);
        SqlConnectionManager connectionManager = new SqlConnectionManager(this.postgres.getJdbcUrl(), this.postgres.getUsername(), this.postgres.getPassword());
        auctionsRepository = new AuctionsRepository(connectionManager);
        bidsRepository = new BidsRepository(connectionManager);

        refundTester = new RefundTester(hederaClient, webClient, bidsRepository, 5000);
    }

    @AfterAll
    public void afterAll() {
        this.postgres.close();
    }

    @BeforeEach
    public void beforeEach() throws Exception {
        auction = testAuctionObject(1);
        auction = auctionsRepository.add(auction);

        auctionsRepository.setActive(auction, "a");

        bidTransaction = HederaJson.singleTransaction();
        bidTransactions = HederaJson.mirrorTransactions(bidTransaction);

        transactionId = bidTransaction.getString("transaction_id");
        transactionHash = Utils.base64toString(bidTransaction.getString("transaction_hash"));

        bid = testBidObject(1, auction.getId());
        bidsRepository.add(bid);
    }

    @AfterEach
    public void afterEach() throws SQLException {
        bidsRepository.deleteAllBids();
        auctionsRepository.deleteAllAuctions();
    }

    @Test
    public void testRefundSuccess() throws Exception {

        bidsRepository.setRefundInProgress(bid.getTimestamp(), transactionId, transactionHash);

        refundTester.handleResponse(bidTransactions, bid.getTimestamp(), transactionId);

        List<Bid> updateBids = bidsRepository.getBidsList();
        assertTrue(updateBids.get(0).getRefunded());
        assertEquals(transactionHash, updateBids.get(0).getRefundtxhash());
        assertEquals(transactionId, updateBids.get(0).getRefundtxid());
    }

    @Test
    public void testRefundSuccessReadonly() throws Exception {

        bidsRepository.setRefundInProgress(bid.getTimestamp(), transactionId, "");

        refundTester.handleResponse(bidTransactions, bid.getTimestamp(), transactionId);

        List<Bid> updateBids = bidsRepository.getBidsList();
        assertTrue(updateBids.get(0).getRefunded());
        assertEquals(transactionHash, updateBids.get(0).getRefundtxhash());
        assertEquals(transactionId, updateBids.get(0).getRefundtxid());
    }

    @Test
    public void testRefundFailedTx() throws Exception {

        bidTransaction.put("result","failed");
        bidsRepository.setRefundInProgress(bid.getTimestamp(), transactionId, transactionHash);

        refundTester.handleResponse(bidTransactions, bid.getTimestamp(), transactionId);

        List<Bid> updateBids = bidsRepository.getBidsList();
        assertFalse(updateBids.get(0).getRefunded());
        assertEquals(transactionHash, updateBids.get(0).getRefundtxhash());
        assertEquals(transactionId, updateBids.get(0).getRefundtxid());
    }

    @Test
    public void testRefundFailedTxReadonly() throws Exception {

        bidTransaction.put("result","failed");
        bidsRepository.setRefundInProgress(bid.getTimestamp(), transactionId, "");

        refundTester.handleResponse(bidTransactions, bid.getTimestamp(), transactionId);

        List<Bid> updateBids = bidsRepository.getBidsList();
        assertFalse(updateBids.get(0).getRefunded());
        assertEquals("", updateBids.get(0).getRefundtxhash());
        assertEquals(transactionId, updateBids.get(0).getRefundtxid());
    }
}
