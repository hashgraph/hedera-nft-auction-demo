package com.hedera.demo.auction.node.test.integration.auctionreadiness;

import com.hedera.demo.auction.node.app.HederaClient;
import com.hedera.demo.auction.node.app.SqlConnectionManager;
import com.hedera.demo.auction.node.app.domain.Auction;
import com.hedera.demo.auction.node.app.mirrormapping.MirrorTransactions;
import com.hedera.demo.auction.node.app.readinesswatcher.AbstractAuctionReadinessWatcher;
import com.hedera.demo.auction.node.app.repository.AuctionsRepository;
import com.hedera.demo.auction.node.app.repository.BidsRepository;
import com.hedera.demo.auction.node.test.integration.AbstractIntegrationTest;
import com.hedera.demo.auction.node.test.integration.HederaJson;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import org.junit.jupiter.api.*;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

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

    private HederaClient hederaClient = HederaClient.emptyTestClient();
    private ReadinessTester readinessTester;

    public AuctionReadinessIntegrationTest() throws Exception {
    }

    static class ReadinessTester extends AbstractAuctionReadinessWatcher {

        protected ReadinessTester(HederaClient hederaClient, WebClient webClient, AuctionsRepository auctionsRepository, BidsRepository bidsRepository, Auction auction, String refundKey, int mirrorQueryFrequency) {
            super(hederaClient, webClient, auctionsRepository, bidsRepository, auction, refundKey, mirrorQueryFrequency);
        }
    }

    @BeforeAll
    public void beforeAll() throws Exception {
        postgres = new PostgreSQLContainer("postgres:12.6");
        postgres.start();
        migrate(postgres);
        SqlConnectionManager connectionManager = new SqlConnectionManager(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
        auctionsRepository = new AuctionsRepository(connectionManager);
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
    public void testAuctionReadinessFromJsonData() throws Exception {

        assertFalse(verifyResponse(readinessTester, badTokenOwnerAccount, badAccount, badToken, badAmount));
        assertFalse(verifyResponse(readinessTester, tokenOwnerAccountId, badAccount, badToken, badAmount));
        assertFalse(verifyResponse(readinessTester, badTokenOwnerAccount, accountId, badToken, badAmount));
        assertFalse(verifyResponse(readinessTester, tokenOwnerAccountId, accountId, badToken, badAmount));
        assertFalse(verifyResponse(readinessTester, badTokenOwnerAccount, badAccount, tokenId, badAmount));
        assertFalse(verifyResponse(readinessTester, tokenOwnerAccountId, badAccount, tokenId, badAmount));
        assertFalse(verifyResponse(readinessTester, tokenOwnerAccountId, accountId, tokenId, badAmount));

        assertFalse(verifyResponse(readinessTester, badTokenOwnerAccount, badAccount, badToken, goodAmount));
        assertFalse(verifyResponse(readinessTester, tokenOwnerAccountId, badAccount, badToken, goodAmount));
        assertFalse(verifyResponse(readinessTester, badTokenOwnerAccount, accountId, badToken, goodAmount));
        assertFalse(verifyResponse(readinessTester, tokenOwnerAccountId, accountId, badToken, goodAmount));
        assertFalse(verifyResponse(readinessTester, badTokenOwnerAccount, badAccount, tokenId, goodAmount));
        assertFalse(verifyResponse(readinessTester, tokenOwnerAccountId, badAccount, tokenId, goodAmount));

        Auction notStartedAuction = auctionsRepository.getAuction(auction.getId());
        assertEquals("", notStartedAuction.getStarttimestamp());
        assertEquals(Auction.PENDING, notStartedAuction.getStatus());

        assertTrue(verifyResponse(readinessTester, tokenOwnerAccountId, accountId, tokenId, goodAmount));
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
        boolean response = readinessTester.handleResponse(mirrorTransactions);
        assertFalse (response);
        auctionsRepository.deleteAllAuctions();
    }

    @Test
    public void testAuctionReadinessFromJsonDataNotReadyLink() throws Exception {

        ReadinessTester readinessTester = new ReadinessTester(hederaClient, null, auctionsRepository, null,auction, "", 5000);
        readinessTester.setTesting();

        JsonObject jsonResponse = HederaJson.mirrorTransactions(HederaJson.tokenTransferTransaction(badTokenOwnerAccount, badAccount, tokenId, goodAmount));

        jsonResponse.put("links",new JsonObject().put("next","nextlink"));
        MirrorTransactions mirrorTransactions = jsonResponse.mapTo(MirrorTransactions.class);
        boolean response = readinessTester.handleResponse(mirrorTransactions);
        assertFalse (response);
        auctionsRepository.deleteAllAuctions();
    }

    @Test
    public void testAuctionReadinessFromJsonDataReadyLink() throws Exception {

        JsonObject jsonResponse = HederaJson.mirrorTransactions(HederaJson.tokenTransferTransaction(tokenOwnerAccountId, accountId, tokenId, goodAmount));

        jsonResponse.put("links",new JsonObject().put("next","nextlink"));
        MirrorTransactions mirrorTransactions = jsonResponse.mapTo(MirrorTransactions.class);
        boolean response = readinessTester.handleResponse(mirrorTransactions);
        assertTrue (response);
        auctionsRepository.deleteAllAuctions();
    }

    private static boolean verifyResponse(ReadinessTester readinessTester, String fromAccount, String toAccount, String token, long amount) {

        JsonObject jsonResponse = HederaJson.mirrorTransactions(HederaJson.tokenTransferTransaction(fromAccount, toAccount, token, amount));

        MirrorTransactions mirrorTransactions = jsonResponse.mapTo(MirrorTransactions.class);
        boolean response = readinessTester.handleResponse(mirrorTransactions);
        return response;
    }
}
