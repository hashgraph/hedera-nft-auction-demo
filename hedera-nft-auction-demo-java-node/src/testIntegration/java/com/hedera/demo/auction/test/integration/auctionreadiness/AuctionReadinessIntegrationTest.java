package com.hedera.demo.auction.test.integration.auctionreadiness;

import com.hedera.demo.auction.AuctionReadinessWatcher;
import com.hedera.demo.auction.app.HederaClient;
import com.hedera.demo.auction.app.SqlConnectionManager;
import com.hedera.demo.auction.app.domain.Auction;
import com.hedera.demo.auction.app.mirrormapping.MirrorTransactions;
import com.hedera.demo.auction.app.repository.AuctionsRepository;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class AuctionReadinessIntegrationTest extends AbstractIntegrationTest {

    private PostgreSQLContainer postgres;
    private AuctionsRepository auctionsRepository;
    private Auction auction;
    private final static String accountId = "0.0.1";
    private final static String tokenId = "0.0.10";
    private final static String tokenOwnerAccountId = "0.0.20";
    private final static long goodAmount = 1L;

    private final static String badToken = "0.0.12";
    private final static String badAccount = "0.0.2";
    private final static String badTokenOwnerAccount = "0.0.21";
    private final static long badAmount = 0L;

    private final HederaClient hederaClient = new HederaClient();
    private AuctionReadinessWatcher auctionReadinessWatcher;

    public AuctionReadinessIntegrationTest() throws Exception {
    }

    @BeforeAll
    public void beforeAll() throws Exception {
        postgres = new PostgreSQLContainer("postgres:12.6");
        postgres.start();
        migrate(postgres);
        SqlConnectionManager connectionManager = new SqlConnectionManager(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
        auctionsRepository = new AuctionsRepository(connectionManager);
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

        auctionReadinessWatcher = new AuctionReadinessWatcher(hederaClient, auctionsRepository, auction, 5000, /*runOnce= */ false);
    }

    @AfterEach
    public void afterEach() throws SQLException {
        auctionReadinessWatcher.stop();
        auctionsRepository.deleteAllAuctions();;
    }

    @Test
    public void testAuctionReadinessFromJsonData() throws Exception {

        assertFalse(verifyResponse(auctionReadinessWatcher, badTokenOwnerAccount, badAccount, badToken, badAmount));
        assertFalse(verifyResponse(auctionReadinessWatcher, tokenOwnerAccountId, badAccount, badToken, badAmount));
        assertFalse(verifyResponse(auctionReadinessWatcher, badTokenOwnerAccount, accountId, badToken, badAmount));
        assertFalse(verifyResponse(auctionReadinessWatcher, tokenOwnerAccountId, accountId, badToken, badAmount));
        assertFalse(verifyResponse(auctionReadinessWatcher, badTokenOwnerAccount, badAccount, tokenId, badAmount));
        assertFalse(verifyResponse(auctionReadinessWatcher, tokenOwnerAccountId, badAccount, tokenId, badAmount));
        assertFalse(verifyResponse(auctionReadinessWatcher, tokenOwnerAccountId, accountId, tokenId, badAmount));

        assertFalse(verifyResponse(auctionReadinessWatcher, badTokenOwnerAccount, badAccount, badToken, goodAmount));
        assertFalse(verifyResponse(auctionReadinessWatcher, tokenOwnerAccountId, badAccount, badToken, goodAmount));
        assertFalse(verifyResponse(auctionReadinessWatcher, badTokenOwnerAccount, accountId, badToken, goodAmount));
        assertFalse(verifyResponse(auctionReadinessWatcher, tokenOwnerAccountId, accountId, badToken, goodAmount));
        assertFalse(verifyResponse(auctionReadinessWatcher, badTokenOwnerAccount, badAccount, tokenId, goodAmount));
        assertFalse(verifyResponse(auctionReadinessWatcher, tokenOwnerAccountId, badAccount, tokenId, goodAmount));

        Auction notStartedAuction = auctionsRepository.getAuction(auction.getId());
        assertEquals("", notStartedAuction.getStarttimestamp());
        assertEquals(Auction.PENDING, notStartedAuction.getStatus());

        assertTrue(verifyResponse(auctionReadinessWatcher, tokenOwnerAccountId, accountId, tokenId, goodAmount));
        Auction startedAuction = auctionsRepository.getAuction(auction.getId());
        assertNotEquals("", startedAuction.getStarttimestamp());
        assertEquals(Auction.ACTIVE, startedAuction.getStatus());

        auctionsRepository.deleteAllAuctions();
    }

    @Test
    public void testAuctionReadinessFromJsonDataNoTransfers() throws Exception {

        // create an empty response
        JsonObject jsonResponse = HederaJson.mirrorTransactions(new JsonObject());
        MirrorTransactions mirrorTransactions = jsonResponse.mapTo(MirrorTransactions.class);
        boolean response = auctionReadinessWatcher.handleResponse(mirrorTransactions);
        assertFalse (response);
        auctionsRepository.deleteAllAuctions();
    }

    @Test
    public void testAuctionReadinessFromJsonDataNotReadyLink() throws Exception {

        AuctionReadinessWatcher readinessTester = new AuctionReadinessWatcher(hederaClient, auctionsRepository, auction, 5000, /*runOnce= */ false);

        JsonObject jsonResponse = HederaJson.mirrorTransactions(HederaJson.tokenTransferTransaction(badTokenOwnerAccount, badAccount, tokenId, goodAmount));

        jsonResponse.put("links",new JsonObject().put("next","nextlink"));
        MirrorTransactions mirrorTransactions = jsonResponse.mapTo(MirrorTransactions.class);
        boolean response = readinessTester.handleResponse(mirrorTransactions);
        assertFalse (response);
        auctionsRepository.deleteAllAuctions();
        readinessTester.stop();
    }

    @Test
    public void testAuctionReadinessFromJsonDataReadyLink() throws Exception {

        JsonObject jsonResponse = HederaJson.mirrorTransactions(HederaJson.tokenTransferTransaction(tokenOwnerAccountId, accountId, tokenId, goodAmount));

        jsonResponse.put("links",new JsonObject().put("next","nextlink"));
        MirrorTransactions mirrorTransactions = jsonResponse.mapTo(MirrorTransactions.class);
        boolean response = auctionReadinessWatcher.handleResponse(mirrorTransactions);
        assertTrue (response);
        auctionsRepository.deleteAllAuctions();
    }

    private static boolean verifyResponse(AuctionReadinessWatcher readinessTester, String fromAccount, String toAccount, String token, long amount) {

        JsonObject jsonResponse = HederaJson.mirrorTransactions(HederaJson.tokenTransferTransaction(fromAccount, toAccount, token, amount));

        MirrorTransactions mirrorTransactions = jsonResponse.mapTo(MirrorTransactions.class);
        boolean response = readinessTester.handleResponse(mirrorTransactions);
        return response;
    }
}
