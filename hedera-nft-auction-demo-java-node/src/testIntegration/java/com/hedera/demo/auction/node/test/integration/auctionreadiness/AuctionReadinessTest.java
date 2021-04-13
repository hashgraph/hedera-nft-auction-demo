package com.hedera.demo.auction.node.test.integration.auctionreadiness;

import com.google.errorprone.annotations.Var;
import com.hedera.demo.auction.node.app.HederaClient;
import com.hedera.demo.auction.node.app.SqlConnectionManager;
import com.hedera.demo.auction.node.app.domain.Auction;
import com.hedera.demo.auction.node.app.readinesswatcher.AbstractAuctionReadinessWatcher;
import com.hedera.demo.auction.node.app.repository.AuctionsRepository;
import com.hedera.demo.auction.node.app.repository.BidsRepository;
import com.hedera.demo.auction.node.test.integration.AbstractIntegrationTest;
import com.hedera.demo.auction.node.test.integration.HederaJson;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import kotlin.Pair;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class AuctionReadinessTest extends AbstractIntegrationTest {

    private PostgreSQLContainer postgres;
    private AuctionsRepository auctionsRepository;
    private HederaClient hederaClient;

    static class ReadinessTester extends AbstractAuctionReadinessWatcher {

        protected ReadinessTester(HederaClient hederaClient, WebClient webClient, AuctionsRepository auctionsRepository, BidsRepository bidsRepository, Auction auction, String refundKey, int mirrorQueryFrequency) {
            super(hederaClient, webClient, auctionsRepository, bidsRepository, auction, refundKey, mirrorQueryFrequency);
        }
    }

    @BeforeAll
    public void setupDatabase() throws Exception {
        PostgreSQLContainer<?> postgres = new PostgreSQLContainer("postgres:12.6");
        postgres.start();
        migrate(postgres);
        SqlConnectionManager connectionManager = new SqlConnectionManager(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
        auctionsRepository = new AuctionsRepository(connectionManager);
        this.postgres = postgres;
        this.hederaClient = HederaClient.emptyTestClient();
    }

    @AfterAll
    public void stopDatabase() {
        this.postgres.close();
    }

    @Test
    public void testAuctionReadiness() {
        Auction auction = new Auction();

        auction.setAuctionaccountid("account");
        auction.setTokenid("token");

        ReadinessTester readinessTester = new ReadinessTester(hederaClient, null, auctionsRepository, null,auction, "", 5000);
        readinessTester.setTesting();

        assertFalse(readinessTester.checkAssociation("diffaccount", "difftoken", 0));
        assertFalse(readinessTester.checkAssociation("account", "difftoken", 0));
        assertFalse(readinessTester.checkAssociation("diffaccount", "token", 0));

        assertFalse(readinessTester.checkAssociation("diffaccount", "difftoken", 1));
        assertFalse(readinessTester.checkAssociation("account", "difftoken", 1));
        assertFalse(readinessTester.checkAssociation("diffaccount", "token", 1));

        assertFalse(readinessTester.checkAssociation("account", "token", 0));
        assertTrue(readinessTester.checkAssociation("account", "token", 1));
    }
    @Test
    public void testAuctionReadinessFromJsonData() throws Exception {

        @Var Auction auction = new Auction();

        String accountId = "0.0.1";
        String tokenId = "0.0.10";

        auction.setAuctionaccountid(accountId);
        auction.setTokenid(tokenId);

        auction = auctionsRepository.add(auction);

        HederaClient hederaClient = HederaClient.emptyTestClient();
        ReadinessTester readinessTester = new ReadinessTester(hederaClient, null, auctionsRepository, null,auction, "", 5000);
        readinessTester.setTesting();

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

        @Var Auction auction = new Auction();

        String accountId = "0.0.1";
        String tokenId = "0.0.10";

        auction.setAuctionaccountid(accountId);
        auction.setTokenid(tokenId);

        auction = auctionsRepository.add(auction);

        HederaClient hederaClient = HederaClient.emptyTestClient();
        ReadinessTester readinessTester = new ReadinessTester(hederaClient, null, auctionsRepository, null,auction, "", 5000);
        readinessTester.setTesting();

        JsonObject jsonResponse = new JsonObject();
        JsonArray transactions = new JsonArray();

        jsonResponse.put("transactions", transactions);

        Pair<Boolean, String> response = readinessTester.handleResponse(jsonResponse);
        assertFalse (response.getFirst());
        auctionsRepository.deleteAllAuctions();
    }

    @Test
    public void testAuctionReadinessFromJsonDataNotReadyLink() throws Exception {

        @Var Auction auction = new Auction();

        String accountId = "0.0.1";
        String tokenId = "0.0.10";

        auction.setAuctionaccountid(accountId);
        auction.setTokenid(tokenId);

        auction = auctionsRepository.add(auction);

        HederaClient hederaClient = HederaClient.emptyTestClient();
        ReadinessTester readinessTester = new ReadinessTester(hederaClient, null, auctionsRepository, null,auction, "", 5000);
        readinessTester.setTesting();

        JsonObject jsonResponse = new JsonObject();
        JsonArray transactions = new JsonArray();

        transactions.add(HederaJson.tokenTransferTransaction("0.0.2", "0.0.12", 1L));
        jsonResponse.put("transactions", transactions);

        jsonResponse.put("links",new JsonObject().put("next","nextlink"));
        Pair<Boolean, String> response = readinessTester.handleResponse(jsonResponse);
        assertFalse (response.getFirst());
        assertEquals("nextlink", response.getSecond());
        auctionsRepository.deleteAllAuctions();
    }

    @Test
    public void testAuctionReadinessFromJsonDataReadyLink() throws Exception {

        @Var Auction auction = new Auction();

        String accountId = "0.0.1";
        String tokenId = "0.0.10";

        auction.setAuctionaccountid(accountId);
        auction.setTokenid(tokenId);

        auction = auctionsRepository.add(auction);

        HederaClient hederaClient = HederaClient.emptyTestClient();
        ReadinessTester readinessTester = new ReadinessTester(hederaClient, null, auctionsRepository, null,auction, "", 5000);
        readinessTester.setTesting();

        JsonObject jsonResponse = new JsonObject();
        JsonArray transactions = new JsonArray();

        transactions.add(HederaJson.tokenTransferTransaction(accountId, tokenId, 1L));
        jsonResponse.put("transactions", transactions);

        jsonResponse.put("links",new JsonObject().put("next","nextlink"));
        Pair<Boolean, String> response = readinessTester.handleResponse(jsonResponse);
        assertTrue (response.getFirst());
        assertEquals("", response.getSecond());
        auctionsRepository.deleteAllAuctions();
    }

    private static boolean verifyResponse(ReadinessTester readinessTester, String account, String token, long amount) {

        JsonObject jsonResponse = new JsonObject();
        JsonArray transactions = new JsonArray();

        transactions.add(HederaJson.tokenTransferTransaction(account, token, amount));
        jsonResponse.put("transactions", transactions);

        Pair<Boolean, String> response = readinessTester.handleResponse(jsonResponse);
        return response.getFirst();
    }
}
