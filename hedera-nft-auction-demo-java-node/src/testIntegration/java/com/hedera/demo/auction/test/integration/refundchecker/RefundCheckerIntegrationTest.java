package com.hedera.demo.auction.test.integration.refundchecker;

import com.hedera.demo.auction.app.HederaClient;
import com.hedera.demo.auction.app.SqlConnectionManager;
import com.hedera.demo.auction.app.Utils;
import com.hedera.demo.auction.app.domain.Auction;
import com.hedera.demo.auction.app.domain.Bid;
import com.hedera.demo.auction.app.mirrormapping.MirrorTransactions;
import com.hedera.demo.auction.RefundChecker;
import com.hedera.demo.auction.app.repository.AuctionsRepository;
import com.hedera.demo.auction.app.repository.BidsRepository;
import com.hedera.demo.auction.test.integration.AbstractIntegrationTest;
import com.hedera.demo.auction.test.integration.HederaJson;
import io.vertx.core.json.JsonObject;
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

@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class RefundCheckerIntegrationTest extends AbstractIntegrationTest {

    private PostgreSQLContainer postgres;
    private AuctionsRepository auctionsRepository;
    private BidsRepository bidsRepository;
    private final HederaClient hederaClient = new HederaClient();
    Auction auction;
    JsonObject bidTransaction;
    JsonObject bidTransactions;
    String transactionId;
    String transactionHash;
    RefundChecker refundTester;
    Bid bid;

    public RefundCheckerIntegrationTest() throws Exception {
    }

    @BeforeAll
    public void beforeAll() {
        this.postgres = new PostgreSQLContainer("postgres:12.6");
        this.postgres.start();
        migrate(this.postgres);
        SqlConnectionManager connectionManager = new SqlConnectionManager(this.postgres.getJdbcUrl(), this.postgres.getUsername(), this.postgres.getPassword());
        auctionsRepository = new AuctionsRepository(connectionManager);
        bidsRepository = new BidsRepository(connectionManager);

        refundTester = new RefundChecker(hederaClient, auctionsRepository, bidsRepository, 5000, /*runOnce= */ false);
    }

    @AfterAll
    public void afterAll() {
        this.postgres.close();
    }

    @BeforeEach
    public void beforeEach() throws Exception {
        auction = testAuctionObject(1);
        auction = auctionsRepository.add(auction);

        auctionsRepository.setActive(auction, auction.getTokenowneraccount(), "a");

        bidTransaction = HederaJson.singleTransaction();
        bidTransactions = HederaJson.mirrorTransactions(bidTransaction);

        transactionId = bidTransaction.getString("transaction_id");
        transactionHash = Utils.base64toStringHex(bidTransaction.getString("transaction_hash"));

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

        bidsRepository.setRefunded(bid.getTransactionid(), bid.getTransactionid(), bid.getTransactionhash());
        MirrorTransactions mirrorTransactions = bidTransactions.mapTo(MirrorTransactions.class);
        refundTester.handleResponse(mirrorTransactions);

        List<Bid> updateBids = bidsRepository.getBidsList();
        assertTrue(updateBids.get(0).isRefunded());
        assertEquals(bid.getTransactionid(), updateBids.get(0).getRefundtxid());
        assertEquals(bid.getTransactionhash(), updateBids.get(0).getRefundtxhash());
    }

    @Test
    public void testRefundFailedTx() throws Exception {

        String memo = Bid.REFUND_MEMO_PREFIX.concat(bid.getTransactionid());
        String memoBase64 = Utils.stringToBase64(memo);
        bidTransaction.put("memo_base64", memoBase64);
        bidTransaction.put("result","failed");
        bidTransactions = HederaJson.mirrorTransactions(bidTransaction);
        bidsRepository.setRefundIssued(bid.getTimestamp(), "", "");

        MirrorTransactions mirrorTransactions = bidTransactions.mapTo(MirrorTransactions.class);
        refundTester.handleResponse(mirrorTransactions);

        List<Bid> updateBids = bidsRepository.getBidsList();
        assertTrue(updateBids.get(0).isRefundPending());
        assertEquals("", updateBids.get(0).getRefundtxhash());
        assertEquals("", updateBids.get(0).getRefundtxid());
    }
}
