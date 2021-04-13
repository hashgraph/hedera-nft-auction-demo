package com.hedera.demo.auction.node.test.integration.auctionreadiness;

import com.hedera.demo.auction.node.app.HederaClient;
import com.hedera.demo.auction.node.app.SqlConnectionManager;
import com.hedera.demo.auction.node.app.domain.Auction;
import com.hedera.demo.auction.node.app.readinesswatcher.HederaAuctionReadinessWatcher;
import com.hedera.demo.auction.node.app.repository.AuctionsRepository;
import com.hedera.demo.auction.node.app.repository.BidsRepository;
import com.hedera.demo.auction.node.test.integration.AbstractIntegrationTest;
import com.hedera.demo.auction.node.test.integration.HederaJson;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockserver.client.MockServerClient;
import org.mockserver.integration.ClientAndServer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class HederaAuctionReadinessTest extends AbstractIntegrationTest {

    private PostgreSQLContainer postgres;
    private AuctionsRepository auctionsRepository;
    private BidsRepository bidsRepository;
    private HederaClient hederaClient;
    private ClientAndServer mockServer;
    private static final String accountId = "0.0.1";
    private static final String tokenId = "0.0.10";
    private Auction auction = new Auction();

    @BeforeAll
    public void beforeAll() throws Exception {
        this.postgres = new PostgreSQLContainer("postgres:12.6");
        this.postgres.start();
        migrate(this.postgres);
        SqlConnectionManager connectionManager = new SqlConnectionManager(this.postgres.getJdbcUrl(), this.postgres.getUsername(), this.postgres.getPassword());
        this.auctionsRepository = new AuctionsRepository(connectionManager);
        this.bidsRepository = new BidsRepository(connectionManager);
        this.hederaClient = HederaClient.emptyTestClient();

        this.mockServer = startClientAndServer();

        auction.setAuctionaccountid(accountId);
        auction.setTokenid(tokenId);
    }

    @AfterAll
    public void afterAll() {
        this.postgres.close();
        this.mockServer.stop();
    }

    @Test
    public void testAuctionReadinessEmptyResponse() throws Exception {
        hederaClient.setTestingMirrorURL("localhost");
        this.auction = auctionsRepository.add(this.auction);

        HederaAuctionReadinessWatcher hederaAuctionReadinessWatcher = new HederaAuctionReadinessWatcher(hederaClient, webClient, auctionsRepository, bidsRepository, auction, "", 5000);
        hederaAuctionReadinessWatcher.setTesting();
        hederaAuctionReadinessWatcher.setTestingMirrorPort(this.mockServer.getLocalPort());

        JsonObject transactions = new JsonObject();

        new MockServerClient("127.0.0.1", this.mockServer.getLocalPort())
                .when(request()
                        .withPath("/api/v1/transactions")
                        .withQueryStringParameter("account.id", auction.getAuctionaccountid())
                        .withQueryStringParameter("transactiontype", "CRYPTOTRANSFER")
                        .withQueryStringParameter("order", "asc")
                )
                .respond(response()
                        .withBody(transactions.toString()));

        hederaAuctionReadinessWatcher.watch();

        //TODO: This doesn't really check anything !
        Auction updatedAuction = auctionsRepository.getAuction(auction.getId());
        assertEquals(Auction.pending(), updatedAuction.getStatus());

        auctionsRepository.deleteAllAuctions();
    }

    @Test
    public void testAuctionReadiness() throws Exception {
        testMatch(/* updated= */false, "diffaccount", "difftoken", 0);
        testMatch(/* updated= */false, "account", "difftoken", 0);
        testMatch(/* updated= */false, "diffaccount", "token", 0);

        testMatch(/* updated= */false, "diffaccount", "difftoken", 1);
        testMatch(/* updated= */false, "account", "difftoken", 1);
        testMatch(/* updated= */false, "diffaccount", "token", 1);

        testMatch(/* updated= */false, "account", "token", 0);

//        testMatch(/* updated= */true, accountId, tokenId, 1);
    }

    private void testMatch(boolean updated, String account, String token, long amount) throws Exception {
        hederaClient.setTestingMirrorURL("localhost");
        this.auction = auctionsRepository.add(this.auction);
        // set the status to "closed" so we can check it's not reset to pending or open by the tests
//        auctionsRepository.setClosed(auction.getId());

        HederaAuctionReadinessWatcher hederaAuctionReadinessWatcher = new HederaAuctionReadinessWatcher(hederaClient, webClient, auctionsRepository, bidsRepository, auction, "", 5000);
        hederaAuctionReadinessWatcher.setTesting();
        hederaAuctionReadinessWatcher.setTestingMirrorPort(this.mockServer.getLocalPort());

        JsonObject response = new JsonObject();
        JsonArray transactions = new JsonArray();

        transactions.add(HederaJson.tokenTransferTransaction(account, token, amount));
        response.put("transactions", transactions);

        new MockServerClient("127.0.0.1", this.mockServer.getPort())
                .when(request()
                        .withPath("/api/v1/transactions")
                        .withQueryStringParameter("account.id", this.auction.getAuctionaccountid())
                        .withQueryStringParameter("transactiontype", "CRYPTOTRANSFER")
                        .withQueryStringParameter("order", "asc")
                )
                .respond(response()
                        .withBody(response.toString()));

        hederaAuctionReadinessWatcher.watch();

        //TODO: Not ideal, unsure how to catch the end of "watch" which is async
        Thread.sleep(1000);

        Auction updatedAuction = auctionsRepository.getAuction(this.auction.getId());
        if (updated) {
            assertEquals(Auction.active(), updatedAuction.getStatus());
        } else {
            assertEquals(Auction.pending(), updatedAuction.getStatus());
        }
        auctionsRepository.deleteAllAuctions();
    }
}
