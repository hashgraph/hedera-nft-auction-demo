package com.hedera.demo.auction.node.test.system;

import com.hedera.demo.auction.node.app.CreateAuctionAccount;
import com.hedera.demo.auction.node.app.CreateToken;
import com.hedera.demo.auction.node.app.CreateTopic;
import com.hedera.hashgraph.sdk.AccountId;
import com.hedera.hashgraph.sdk.Key;
import com.hedera.hashgraph.sdk.KeyList;
import com.hedera.hashgraph.sdk.TokenAssociateTransaction;
import com.hedera.hashgraph.sdk.TokenId;
import com.hedera.hashgraph.sdk.TopicId;
import com.hedera.hashgraph.sdk.TransactionResponse;
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
        tokenId = createToken.create(tokenName, symbol, initialSupply, decimals);

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
