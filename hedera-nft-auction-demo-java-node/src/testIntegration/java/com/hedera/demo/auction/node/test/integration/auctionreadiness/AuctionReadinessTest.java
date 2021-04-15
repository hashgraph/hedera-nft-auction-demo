package com.hedera.demo.auction.node.test.integration.auctionreadiness;

import com.hedera.demo.auction.node.app.HederaClient;
import com.hedera.demo.auction.node.app.SqlConnectionManager;
import com.hedera.demo.auction.node.app.domain.Auction;
import com.hedera.demo.auction.node.app.readinesswatcher.AbstractAuctionReadinessWatcher;
import com.hedera.demo.auction.node.app.repository.AuctionsRepository;
import com.hedera.demo.auction.node.app.repository.BidsRepository;
import com.hedera.demo.auction.node.test.integration.AbstractIntegrationTest;
import com.hedera.demo.auction.node.test.integration.HederaJson;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import kotlin.Pair;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class AuctionReadinessTest extends AbstractIntegrationTest {

    private PostgreSQLContainer postgres;
    private AuctionsRepository auctionsRepository;
    private Auction auction;
    private final String accountId = "0.0.1";
    private final String tokenId = "0.0.10";
    private HederaClient hederaClient = HederaClient.emptyTestClient();
    private ReadinessTester readinessTester;

    public AuctionReadinessTest() throws Exception {
    }

    static class ReadinessTester extends AbstractAuctionReadinessWatcher {

        protected ReadinessTester(HederaClient hederaClient, WebClient webClient, AuctionsRepository auctionsRepository, BidsRepository bidsRepository, Auction auction, String refundKey, int mirrorQueryFrequency) {
            super(hederaClient, webClient, auctionsRepository, bidsRepository, auction, refundKey, mirrorQueryFrequency);
        }
    }

    @BeforeAll
    public void beforeAll() throws Exception {
        PostgreSQLContainer<?> postgres = new PostgreSQLContainer("postgres:12.6");
        postgres.start();
        migrate(postgres);
        SqlConnectionManager connectionManager = new SqlConnectionManager(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
        auctionsRepository = new AuctionsRepository(connectionManager);
        this.postgres = postgres;
        this.hederaClient = HederaClient.emptyTestClient();
    }

    @AfterAll
    public void afterAll() {
        this.postgres.close();
    }

    @BeforeEach
    public void beforeEach() throws SQLException {
        auction = new Auction();
        auction.setAuctionaccountid(accountId);
        auction.setTokenid(tokenId);
        auction = auctionsRepository.add(auction);

        readinessTester = new ReadinessTester(hederaClient, null, auctionsRepository, null,auction, "", 5000);
        readinessTester.setTesting();
    }
    
    @AfterEach
    public void afterEach() throws SQLException {
        auctionsRepository.deleteAllAuctions();;
    }
    @Test
    public void testAuctionReadiness() {

        assertFalse(readinessTester.checkAssociation("diffaccount", "difftoken", 0));
        assertFalse(readinessTester.checkAssociation(accountId, "difftoken", 0));
        assertFalse(readinessTester.checkAssociation("diffaccount", tokenId, 0));

        assertFalse(readinessTester.checkAssociation("diffaccount", "difftoken", 1));
        assertFalse(readinessTester.checkAssociation(accountId, "difftoken", 1));
        assertFalse(readinessTester.checkAssociation("diffaccount", tokenId, 1));

        assertFalse(readinessTester.checkAssociation(accountId, tokenId, 0));
        assertTrue(readinessTester.checkAssociation(accountId, tokenId, 1));
    }
    @Test
    public void testAuctionReadinessFromJsonData() throws Exception {

        assertFalse(verifyResponse(readinessTester, "0.0.2", "0.0.12", 0L));
        assertFalse(verifyResponse(readinessTester, accountId, "0.0.12", 0L));
        assertFalse(verifyResponse(readinessTester, "0.0.2", tokenId, 0L));

        assertFalse(verifyResponse(readinessTester, "0.0.2", "0.0.12", 1L));
        assertFalse(verifyResponse(readinessTester, accountId, "0.0.12", 1L));
        assertFalse(verifyResponse(readinessTester, "0.0.2", tokenId, 1L));

        assertFalse(verifyResponse(readinessTester, accountId, tokenId, 0L));

        Auction notStartedAuction = auctionsRepository.getAuction(auction.getId());
        assertEquals("", notStartedAuction.getStarttimestamp());
        assertEquals(Auction.pending(), notStartedAuction.getStatus());

        assertTrue(verifyResponse(readinessTester, accountId, tokenId, 1L));
        Auction startedAuction = auctionsRepository.getAuction(auction.getId());
        assertNotEquals("", startedAuction.getStarttimestamp());
        assertEquals(Auction.active(), startedAuction.getStatus());

        auctionsRepository.deleteAllAuctions();
    }

    @Test
    public void testAuctionReadinessFromJsonDataNoTransfers() throws Exception {

        // create an empty response
        JsonObject jsonResponse = HederaJson.mirrorTransactions(new JsonObject());

        Pair<Boolean, String> response = readinessTester.handleResponse(jsonResponse);
        assertFalse (response.getFirst());
        auctionsRepository.deleteAllAuctions();
    }

    @Test
    public void testAuctionReadinessFromJsonDataNotReadyLink() throws Exception {

        ReadinessTester readinessTester = new ReadinessTester(hederaClient, null, auctionsRepository, null,auction, "", 5000);
        readinessTester.setTesting();

        JsonObject jsonResponse = HederaJson.mirrorTransactions(HederaJson.tokenTransferTransaction("0.0.2", "0.0.12", 1L));

        jsonResponse.put("links",new JsonObject().put("next","nextlink"));
        Pair<Boolean, String> response = readinessTester.handleResponse(jsonResponse);
        assertFalse (response.getFirst());
        assertEquals("nextlink", response.getSecond());
        auctionsRepository.deleteAllAuctions();
    }

    @Test
    public void testAuctionReadinessFromJsonDataReadyLink() throws Exception {

        JsonObject jsonResponse = HederaJson.mirrorTransactions(HederaJson.tokenTransferTransaction(accountId, tokenId, 1L));

        jsonResponse.put("links",new JsonObject().put("next","nextlink"));
        Pair<Boolean, String> response = readinessTester.handleResponse(jsonResponse);
        assertTrue (response.getFirst());
        assertEquals("", response.getSecond());
        auctionsRepository.deleteAllAuctions();
    }

    private static boolean verifyResponse(ReadinessTester readinessTester, String account, String token, long amount) {

        JsonObject jsonResponse = HederaJson.mirrorTransactions(HederaJson.tokenTransferTransaction(account, token, amount));

        Pair<Boolean, String> response = readinessTester.handleResponse(jsonResponse);
        return response.getFirst();
    }
}
