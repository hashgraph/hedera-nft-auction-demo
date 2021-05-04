package com.hedera.demo.auction.node.test.integration.winnertokentransfer;

import com.google.errorprone.annotations.Var;
import com.hedera.demo.auction.node.app.HederaClient;
import com.hedera.demo.auction.node.app.SqlConnectionManager;
import com.hedera.demo.auction.node.app.domain.Auction;
import com.hedera.demo.auction.node.app.repository.AuctionsRepository;
import com.hedera.demo.auction.node.app.auctionendtransfer.AuctionEndTransfer;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TransferToWinnerIntegrationTest extends AbstractIntegrationTest {

    private PostgreSQLContainer postgres;
    private AuctionsRepository auctionsRepository;
    private final HederaClient hederaClient = HederaClient.emptyTestClient();
    private final static String tokenId = "0.0.10";
    private final static String auctionAccountId = "0.0.30";
    private final static String winningAccountId = "0.0.20";
    private final static String tokenOwnerAccountId = "0.0.30";
    private AuctionEndTransfer auctionEndTransfer;
    private Auction auction;

    public TransferToWinnerIntegrationTest() throws Exception {
    }

    @BeforeAll
    public void beforeAll() {
        this.postgres = new PostgreSQLContainer("postgres:12.6");
        this.postgres.start();
        migrate(this.postgres);
        SqlConnectionManager connectionManager = new SqlConnectionManager(this.postgres.getJdbcUrl(), this.postgres.getUsername(), this.postgres.getPassword());
        auctionsRepository = new AuctionsRepository(connectionManager);
        auctionEndTransfer = new AuctionEndTransfer(hederaClient, webClient, auctionsRepository, "", 5000);
    }

    @AfterAll
    public void afterAll() {
        this.postgres.close();
    }

    @BeforeEach
    public void beforeEach() throws SQLException {
        auction = testAuctionObject(0);
        auction.setTokenid(tokenId);
        auction.setAuctionaccountid(auctionAccountId);
        auction = auctionsRepository.add(auction);
        auction.setWinningaccount(winningAccountId);
        auction.setTokenowneraccount(tokenOwnerAccountId);
        auctionsRepository.save(auction);
    }

    @AfterEach
    public void afterEach() throws SQLException {
        auctionsRepository.deleteAllAuctions();
    }

    @Test
    public void testTokenTransfer() throws Exception {


        @Var Auction updatedAuction = auctionsRepository.getAuction(auction.getId());
        assertEquals("", updatedAuction.getTransfertxid());
        assertEquals("", updatedAuction.getTransfertxhash());

        auctionEndTransfer.transferToken(auction);

        updatedAuction = auctionsRepository.getAuction(auction.getId());
        assertNotEquals("", updatedAuction.getTransfertxid());
        assertEquals("", updatedAuction.getTransfertxhash());
    }
}
