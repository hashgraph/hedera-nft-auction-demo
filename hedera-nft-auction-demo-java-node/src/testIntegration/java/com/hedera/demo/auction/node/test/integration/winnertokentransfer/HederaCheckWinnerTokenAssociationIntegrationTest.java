package com.hedera.demo.auction.node.test.integration.winnertokentransfer;

import com.hedera.demo.auction.node.app.HederaClient;
import com.hedera.demo.auction.node.app.SqlConnectionManager;
import com.hedera.demo.auction.node.app.domain.Auction;
import com.hedera.demo.auction.node.app.repository.AuctionsRepository;
import com.hedera.demo.auction.node.app.auctionendtokentransfer.HederaAuctionEndTokenTransfer;
import com.hedera.demo.auction.node.test.integration.AbstractIntegrationTest;
import io.vertx.core.json.JsonArray;
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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class HederaCheckWinnerTokenAssociationIntegrationTest extends AbstractIntegrationTest {

    private PostgreSQLContainer postgres;
    private AuctionsRepository auctionsRepository;
    private final HederaClient hederaClient = HederaClient.emptyTestClient();
    private final static String tokenId = "0.0.10";
    private final static String winningAccountId = "0.0.20";
    private HederaAuctionEndTokenTransfer hederaWinnerTokenTransfer;
    private Auction auction;
    private JsonObject transfer;
    private JsonArray balances;
    private JsonObject balance;

    public HederaCheckWinnerTokenAssociationIntegrationTest() throws Exception {
    }

    @BeforeAll
    public void beforeAll() {
        this.postgres = new PostgreSQLContainer("postgres:12.6");
        this.postgres.start();
        migrate(this.postgres);
        SqlConnectionManager connectionManager = new SqlConnectionManager(this.postgres.getJdbcUrl(), this.postgres.getUsername(), this.postgres.getPassword());
        auctionsRepository = new AuctionsRepository(connectionManager);

        hederaWinnerTokenTransfer = new HederaAuctionEndTokenTransfer(hederaClient, null, auctionsRepository, tokenId, winningAccountId);
    }

    @AfterAll
    public void afterAll() {
        this.postgres.close();
    }

    @BeforeEach
    public void beforeEach() throws SQLException {
        auction = testAuctionObject(0);
        auction.setTokenid(tokenId);
        auction = auctionsRepository.add(auction);

        transfer = new JsonObject();
        balances = new JsonArray();
        balance = new JsonObject();
    }

    @AfterEach
    public void afterEach() throws SQLException {
        auctionsRepository.deleteAllAuctions();
    }

    @Test
    public void testEmptyBody() throws Exception {

        hederaWinnerTokenTransfer.checkForAssociation(transfer);

        Auction updatedAuction = auctionsRepository.getAuction(auction.getId());
        assertTrue(updatedAuction.isPending());
    }

    @Test
    public void testWinningAccount() throws Exception {

        transfer.put("transactions", new JsonArray().add(balances));

        hederaWinnerTokenTransfer.checkForAssociation(transfer);

        Auction updatedAuction = auctionsRepository.getAuction(auction.getId());
        assertTrue(updatedAuction.isTransferring());
    }

    @Test
    public void testWinningAccountBalanceZero() throws Exception {

        transfer.put("transactions", new JsonArray());

        hederaWinnerTokenTransfer.checkForAssociation(transfer);

        Auction updatedAuction = auctionsRepository.getAuction(auction.getId());
        assertFalse(updatedAuction.isTransferring());
    }
}
