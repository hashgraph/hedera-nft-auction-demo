package com.hedera.demo.auction.node.test.integration.winnertokentransfer;

import com.hedera.demo.auction.node.app.HederaClient;
import com.hedera.demo.auction.node.app.SqlConnectionManager;
import com.hedera.demo.auction.node.app.Utils;
import com.hedera.demo.auction.node.app.domain.Auction;
import com.hedera.demo.auction.node.app.repository.AuctionsRepository;
import com.hedera.demo.auction.node.app.winnertokentransferwatcher.AbstractWinnerTokenTransferWatcher;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class WinnerTokenTransferWatcherIntegrationTest extends AbstractIntegrationTest {

    static class WinnerTokenTransferWatcher extends AbstractWinnerTokenTransferWatcher {
        protected WinnerTokenTransferWatcher(HederaClient hederaClient, WebClient webClient, AuctionsRepository auctionsRepository, Auction auction) {
            super(hederaClient, webClient, auctionsRepository, auction);
        }
    }

    private PostgreSQLContainer postgres;
    private AuctionsRepository auctionsRepository;
    private final HederaClient hederaClient = HederaClient.emptyTestClient();
    private final static String tokenId = "0.0.10";
    private final static String auctionAccountId = "0.0.30";
    private WinnerTokenTransferWatcher winnerTokenTransferWatcher;
    private Auction auction;
    private String transactionId;
    private String transactionHash;
    private JsonObject response;

    public WinnerTokenTransferWatcherIntegrationTest() throws Exception {
    }

    @BeforeAll
    public void beforeAll() {
        this.postgres = new PostgreSQLContainer("postgres:12.6");
        this.postgres.start();
        migrate(this.postgres);
        SqlConnectionManager connectionManager = new SqlConnectionManager(this.postgres.getJdbcUrl(), this.postgres.getUsername(), this.postgres.getPassword());
        auctionsRepository = new AuctionsRepository(connectionManager);
    }

    @AfterAll
    public void afterAll() {
        this.postgres.close();
    }

    @BeforeEach
    public void beforeEach() throws SQLException {
        JsonObject mirrorTransaction = HederaJson.singleTransaction();
        transactionId = mirrorTransaction.getString("transaction_id");
        transactionHash = Utils.base64toString(mirrorTransaction.getString("transaction_hash"));
        response = HederaJson.mirrorTransactions(mirrorTransaction);

        auction = testAuctionObject(0);
        auction.setTokenid(tokenId);
        auction.setAuctionaccountid(auctionAccountId);
        auction = auctionsRepository.add(auction);
        auction.setTransfertxid(transactionId);
        auctionsRepository.setTransferTransaction(auction.getId(), transactionId, "");

        winnerTokenTransferWatcher = new WinnerTokenTransferWatcher(hederaClient, webClient, auctionsRepository, auction);
    }

    @AfterEach
    public void afterEach() throws SQLException {
        auctionsRepository.deleteAllAuctions();
    }

    @Test
    public void testSuccessfulTransaction() throws Exception {

        winnerTokenTransferWatcher.handleResponse(response, auction);

        Auction updatedAuction = auctionsRepository.getAuction(auction.getId());
        assertEquals(transactionId, updatedAuction.getTransfertxid());
        assertEquals(transactionHash, updatedAuction.getTransfertxhash());
        assertTrue(updatedAuction.isEnded());
    }

    @Test
    public void testFailedTransaction() throws Exception {

        response.getJsonArray("transactions").getJsonObject(0).put("result", "error");

        winnerTokenTransferWatcher.handleResponse(response, auction);

        Auction updatedAuction = auctionsRepository.getAuction(auction.getId());
        assertEquals(transactionId, updatedAuction.getTransfertxid());
        assertEquals("", updatedAuction.getTransfertxhash());
        assertTrue(updatedAuction.isPending());
    }

    @Test
    public void testEmptyResponse() throws Exception {

        response = new JsonObject();

        winnerTokenTransferWatcher.handleResponse(response, auction);

        Auction updatedAuction = auctionsRepository.getAuction(auction.getId());
        assertEquals(transactionId, updatedAuction.getTransfertxid());
        assertEquals("", updatedAuction.getTransfertxhash());
        assertTrue(updatedAuction.isPending());
    }
}
