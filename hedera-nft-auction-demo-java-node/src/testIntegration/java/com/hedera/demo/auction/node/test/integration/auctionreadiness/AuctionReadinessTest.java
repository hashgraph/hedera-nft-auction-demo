package com.hedera.demo.auction.node.test.integration.auctionreadiness;

import com.google.errorprone.annotations.Var;
import com.hedera.demo.auction.node.app.HederaClient;
import com.hedera.demo.auction.node.app.SqlConnectionManager;
import com.hedera.demo.auction.node.app.domain.Auction;
import com.hedera.demo.auction.node.app.readinesswatcher.AbstractAuctionReadinessWatcher;
import com.hedera.demo.auction.node.app.repository.AuctionsRepository;
import com.hedera.demo.auction.node.app.repository.BidsRepository;
import com.hedera.demo.auction.node.test.integration.AbstractIntegrationTest;
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
    public void testAuctionReadiness() throws Exception {
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
    public void testAuctionReadinessFromMirrorData() throws  Exception {

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
    public void testAuctionReadinessFromMirrorDataNoTransfers() throws  Exception {

        @Var Auction auction = new Auction();

        String accountId = "0.0.1";
        String tokenId = "0.0.10";

        auction.setAuctionaccountid(accountId);
        auction.setTokenid(tokenId);

        auction = auctionsRepository.add(auction);

        HederaClient hederaClient = HederaClient.emptyTestClient();
        ReadinessTester readinessTester = new ReadinessTester(hederaClient, null, auctionsRepository, null,auction, "", 5000);
        readinessTester.setTesting();

        JsonObject transaction = transaction();

        Pair<Boolean, String> response = readinessTester.handleResponse(transaction);
        assertFalse (response.getFirst());
        auctionsRepository.deleteAllAuctions();
    }

    @Test
    public void testAuctionReadinessFromMirrorDataNotReadyLink() throws  Exception {

        @Var Auction auction = new Auction();

        String accountId = "0.0.1";
        String tokenId = "0.0.10";

        auction.setAuctionaccountid(accountId);
        auction.setTokenid(tokenId);

        auction = auctionsRepository.add(auction);

        HederaClient hederaClient = HederaClient.emptyTestClient();
        ReadinessTester readinessTester = new ReadinessTester(hederaClient, null, auctionsRepository, null,auction, "", 5000);
        readinessTester.setTesting();

        JsonObject transaction = transaction();

        JsonArray tokenTransfers = new JsonArray();
        tokenTransfers.add(tokenTransfer("0.0.2", "0.0.12", 1L));

        transaction.getJsonArray("transactions").getJsonObject(0).put("token_transfers", tokenTransfers);
        transaction.put("links",new JsonObject().put("next","nextlink"));
        Pair<Boolean, String> response = readinessTester.handleResponse(transaction);
        assertFalse (response.getFirst());
        assertEquals("nextlink", response.getSecond());
        auctionsRepository.deleteAllAuctions();
    }

    @Test
    public void testAuctionReadinessFromMirrorDataReadyLink() throws  Exception {

        @Var Auction auction = new Auction();

        String accountId = "0.0.1";
        String tokenId = "0.0.10";

        auction.setAuctionaccountid(accountId);
        auction.setTokenid(tokenId);

        auction = auctionsRepository.add(auction);

        HederaClient hederaClient = HederaClient.emptyTestClient();
        ReadinessTester readinessTester = new ReadinessTester(hederaClient, null, auctionsRepository, null,auction, "", 5000);
        readinessTester.setTesting();

        JsonObject transaction = transaction();

        JsonArray tokenTransfers = new JsonArray();
        tokenTransfers.add(tokenTransfer(accountId, tokenId, 1L));

        transaction.getJsonArray("transactions").getJsonObject(0).put("token_transfers", tokenTransfers);
        transaction.put("links",new JsonObject().put("next","nextlink"));
        Pair<Boolean, String> response = readinessTester.handleResponse(transaction);
        assertTrue (response.getFirst());
        assertEquals("", response.getSecond());
        auctionsRepository.deleteAllAuctions();
    }

    private static boolean verifyResponse(ReadinessTester readinessTester, String account, String token, long amount) {

        JsonObject transaction = transaction();

        JsonArray tokenTransfers = new JsonArray();
        tokenTransfers.add(tokenTransfer(account, token, amount));

        transaction.getJsonArray("transactions").getJsonObject(0).put("token_transfers", tokenTransfers);
        Pair<Boolean, String> response = readinessTester.handleResponse(transaction);
        return response.getFirst();
    }

    private static JsonObject tokenTransfer(String account, String token, long amount) {
        JsonObject transfer = new JsonObject();
        transfer.put("amount", amount);
        transfer.put("token_id", token);
        transfer.put("account", account);
        return transfer;
    }

    private static JsonObject transaction() {
        JsonObject transaction = new JsonObject();
        transaction.put("charged_tx_fee", 84650);
        transaction.put("consensus_timestamp", "1617786661.662353000");
        transaction.put("max_fee", "100000000");
        transaction.put("memo_base64", "RGV2T3BzIFN5bnRoZXRpYyBUZXN0aW5n");
        transaction.put("name", "CRYPTOTRANSFER");
        transaction.put("node", "0.0.7");
        transaction.put("result", "SUCCESS");
        transaction.put("scheduled", false);
        transaction.put("transaction_hash", "KeUK9l64b1HShMmvFeQ+CCO2hBvzF5tUL8X2Bvxvsh+rcdNxxkQHEb3/nS6zsRwX");
        transaction.put("transaction_id", "0.0.90-1617786650-796134000");
        transaction.put("valid_duration_seconds", "120");
        transaction.put("valid_start_timestamp", "1617786650.796134000");

        JsonArray transactions = new JsonArray();
        transactions.add(transaction);
        return new JsonObject().put("transactions", transactions);
    }
}
