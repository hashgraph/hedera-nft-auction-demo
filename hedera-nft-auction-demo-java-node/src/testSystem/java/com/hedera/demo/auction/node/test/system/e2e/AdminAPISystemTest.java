package com.hedera.demo.auction.node.test.system.e2e;

import com.hedera.demo.auction.node.app.CreateAuctionAccount;
import com.hedera.demo.auction.node.app.CreateToken;
import com.hedera.demo.auction.node.app.CreateTopic;
import com.hedera.demo.auction.node.app.SqlConnectionManager;
import com.hedera.demo.auction.node.app.repository.AuctionsRepository;
import com.hedera.demo.auction.node.app.repository.BidsRepository;
import com.hedera.hashgraph.sdk.*;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.codec.BodyCodec;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith({VertxExtension.class})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class AdminAPISystemTest extends AbstractE2ETest {

    Vertx vertx;
    GenericContainer container;

    AdminAPISystemTest() throws Exception {
        super();
    }

    @BeforeAll
    public void beforeAll() {
        postgres = (PostgreSQLContainer) new PostgreSQLContainer("postgres:12.6").withNetworkAliases("pgdb").withNetwork(testContainersNetwork);
        postgres.start();
        migrate(postgres);
        SqlConnectionManager connectionManager = new SqlConnectionManager(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
        auctionsRepository = new AuctionsRepository(connectionManager);
        bidsRepository = new BidsRepository(connectionManager);

        this.vertx = Vertx.vertx();
        this.webClient = WebClient.create(this.vertx);
    }

    @AfterAll
    public void afterAll() {
        this.postgres.close();
    }

    @BeforeEach
    public void beforeEach() throws Exception {
        bidsRepository.deleteAllBids();
        auctionsRepository.deleteAllAuctions();

        container = appContainer(postgres,"", "hedera");
    }

    @AfterEach
    public void afterEach() {
        container.stop();
    }

    @Test
    public void testAdminRestAPITopic(VertxTestContext testContext) {
        webClient.post(adminPort, container.getHost(), "/v1/admin/topic")
                .as(BodyCodec.jsonObject())
                .send(testContext.succeeding(response -> testContext.verify(() -> {

                    assertNotNull(response.body());
                    JsonObject body = JsonObject.mapFrom(response.body());
                    assertNotNull(body);

                    assertNotNull(body.getString("topicId"));
                    assertNotEquals("", body.getString("topicId"));

                    topicId = TopicId.fromString(body.getString("topicId"));

                    getTopicInfo();

                    assertEquals(topicId, topicInfo.topicId);

                    testContext.completeNow();
                })));
    }

    @Test
    public void testAdminRestAPIToken(VertxTestContext testContext) {
        webClient.post(adminPort, container.getHost(), "/v1/admin/token")
                .as(BodyCodec.jsonObject())
                .sendBuffer(createTokenBody(), testContext.succeeding(response -> testContext.verify(() -> {

                    assertNotNull(response.body());
                    JsonObject body = JsonObject.mapFrom(response.body());
                    assertNotNull(body);

                    assertNotNull(body.getString("tokenId"));
                    assertNotEquals("", body.getString("tokenId"));

                    tokenId = TokenId.fromString(body.getString("tokenId"));

                    getTokenInfo();

                    assertEquals(tokenId, tokenInfo.tokenId);
                    assertEquals(tokenName, tokenInfo.name);
                    assertEquals(symbol, tokenInfo.symbol);
                    assertEquals(decimals, tokenInfo.decimals);
                    assertEquals(initialSupply, tokenInfo.totalSupply);

                    testContext.completeNow();
                })));
    }

    @Test
    public void testAdminRestAPIAuctionAccount(VertxTestContext testContext) {
        webClient.post(adminPort, container.getHost(), "/v1/admin/auctionaccount")
                .as(BodyCodec.jsonObject())
                .sendBuffer(createAuctionAccountBody(), testContext.succeeding(response -> testContext.verify(() -> {

                    assertNotNull(response.body());
                    JsonObject body = JsonObject.mapFrom(response.body());
                    assertNotNull(body);

                    assertNotNull(body.getString("accountId"));
                    assertNotEquals("", body.getString("accountId"));

                    auctionAccountId = AccountId.fromString(body.getString("accountId"));

                    getAccountInfo();

                    assertEquals(auctionAccountId, accountInfo.accountId);
                    KeyList keylist = (KeyList) accountInfo.key;
                    assertNull(keylist.threshold);
                    assertEquals(1, keylist.size());
                    Object[] keyListArray = keylist.toArray();

                    Key accountKey = (Key)keyListArray[0];

                    assertEquals(hederaClient.operatorPublicKey().toString(), accountKey.toString());
                    assertEquals(initialBalance, accountInfo.balance.getValue().longValue());

                    testContext.completeNow();
                })));
    }

    @Test
    public void testAdminRestAPITransferToken(VertxTestContext testContext) throws Exception {
        CreateAuctionAccount createAuctionAccount = new CreateAuctionAccount();
        auctionAccountId = createAuctionAccount.create(initialBalance, "");

        TransactionResponse txResponse = new TokenAssociateTransaction()
                .setAccountId(auctionAccountId)
                .setTokenIds(List.of(tokenId))
                .execute(hederaClient.client());

        txResponse.getReceipt(hederaClient.client());

        webClient.post(adminPort, container.getHost(), "/v1/admin/transfer")
                .as(BodyCodec.jsonObject())
                .sendBuffer(createTokenTransferBody(), testContext.succeeding(response -> testContext.verify(() -> {

                    assertNotNull(response.body());
                    JsonObject body = JsonObject.mapFrom(response.body());
                    assertNotNull(body);

                    assertNotNull(body.getString("status"));
                    assertNotEquals("", body.getString("status"));

                    assertEquals("transferred", body.getString("status"));

                    getAccountBalance();

                    Map<TokenId, Long> tokenBalances = accountBalance.token;
                    assertTrue(tokenBalances.containsKey(tokenId));
                    assertNotEquals(0, tokenBalances.get(tokenId));

                    testContext.completeNow();
                })));
    }

    @Test
    public void testAdminRestAPIAuction(VertxTestContext testContext) throws Exception {
        CreateTopic createTopic = new CreateTopic();
        topicId = createTopic.create();

        CreateToken createToken = new CreateToken();
        tokenId = createToken.create(tokenName, symbol, initialSupply, decimals);

        CreateAuctionAccount createAuctionAccount = new CreateAuctionAccount();
        auctionAccountId = createAuctionAccount.create(initialBalance, "");

        webClient.post(adminPort, container.getHost(), "/v1/admin/auction")
                .as(BodyCodec.jsonObject())
                .sendBuffer(createAuctionBody(), testContext.succeeding(response -> testContext.verify(() -> {

                    assertNotNull(response.body());
                    JsonObject body = JsonObject.mapFrom(response.body());
                    assertNotNull(body);

                    assertNotNull(body.getString("status"));
                    assertNotEquals("", body.getString("status"));

                    assertEquals("created", body.getString("status"));

                    getTopicInfo();

                    assertEquals(1, topicInfo.sequenceNumber);

                    testContext.completeNow();

                })));
    }
}
