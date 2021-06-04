package com.hedera.demo.auction.test.system;

import com.hedera.demo.auction.app.CreateAuctionAccount;
import com.hedera.demo.auction.app.CreateToken;
import com.hedera.demo.auction.app.CreateTopic;
import com.hedera.hashgraph.sdk.*;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.codec.BodyCodec;
import io.vertx.junit5.VertxTestContext;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class AbstractAPITester extends AbstractE2ETest {

    protected AbstractAPITester() throws Exception {
        adminPort = Integer.parseInt(dotenv.get("ADMIN_API_PORT"));
    }

    public void adminRestAPITopic(VertxTestContext testContext, String host) {
        webClient.post(adminPort, host, "/v1/admin/topic")
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

    public void adminRestAPIToken(VertxTestContext testContext, String host) {
        webClient.post(adminPort, host, "/v1/admin/token")
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

    public void adminRestAPIAuctionAccount(VertxTestContext testContext, String host) {
        webClient.post(adminPort, host, "/v1/admin/auctionaccount")
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
                    assertEquals(initialBalance, accountInfo.balance.getValue().longValue());

                    testContext.completeNow();
                })));
    }

    public void adminRestAPITransferToken(VertxTestContext testContext, String host) throws Exception {
        CreateAuctionAccount createAuctionAccount = new CreateAuctionAccount();
        auctionAccountId = createAuctionAccount.create(initialBalance, "");

        TransactionResponse txResponse = new TokenAssociateTransaction()
                .setAccountId(auctionAccountId)
                .setTokenIds(List.of(tokenId))
                .execute(hederaClient.client());

        txResponse.getReceipt(hederaClient.client());

        webClient.post(adminPort, host, "/v1/admin/transfer")
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

    public void adminRestAPIAuction(VertxTestContext testContext, String host) throws Exception {
        CreateTopic createTopic = new CreateTopic();
        topicId = createTopic.create();

        CreateToken createToken = new CreateToken();
        tokenId = createToken.create(tokenName, symbol, initialSupply, decimals, tokenMemo);

        CreateAuctionAccount createAuctionAccount = new CreateAuctionAccount();
        auctionAccountId = createAuctionAccount.create(initialBalance, "");

        webClient.post(adminPort, host, "/v1/admin/auction")
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
