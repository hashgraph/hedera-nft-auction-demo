package com.hedera.demo.auction.test.system;

import com.hedera.demo.auction.app.CreateAuctionAccount;
import com.hedera.demo.auction.app.CreateToken;
import com.hedera.demo.auction.app.CreateTopic;
import com.hedera.demo.auction.app.api.RequestCreateAuctionAccount;
import com.hedera.demo.auction.app.api.RequestCreateAuctionAccountKey;
import com.hedera.demo.auction.app.api.RequestCreateToken;
import com.hedera.hashgraph.sdk.AccountId;
import com.hedera.hashgraph.sdk.TokenAssociateTransaction;
import com.hedera.hashgraph.sdk.TokenId;
import com.hedera.hashgraph.sdk.TopicId;
import com.hedera.hashgraph.sdk.TransactionResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.codec.BodyCodec;
import io.vertx.junit5.VertxTestContext;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AbstractAPITester extends AbstractE2ETest {

  @SuppressWarnings("FieldMissingNullable")
  protected final static String apiKey = Optional.ofNullable(dotenv.get("X_API_KEY")).orElse("");
    protected AbstractAPITester() throws Exception {
        adminPort = Integer.parseInt(dotenv.get("ADMIN_API_PORT"));
    }

    public void adminRestAPITopic(VertxTestContext testContext, String host) {
        webClient.post(adminPort, host, "/v1/admin/topic")
                .putHeader("x-api-key", apiKey)
                .as(BodyCodec.jsonObject())
                .send(testContext.succeeding(response -> testContext.verify(() -> {

                    assertNotNull(response.body());
                    JsonObject body = JsonObject.mapFrom(response.body());
                    assertNotNull(body);

                    assertNotNull(body.getString("topicId"));
                    assertNotEquals("", body.getString("topicId"));

                    topicId = TopicId.fromString(body.getString("topicId"));

                    getTopicInfo();

                    assertNotNull(topicInfo);
                    assertEquals(topicId, topicInfo.topicId);

                    testContext.completeNow();
                })));
    }

    public void adminRestAPIToken(VertxTestContext testContext, String host) {
        webClient.post(adminPort, host, "/v1/admin/token")
                .putHeader("x-api-key", apiKey)
                .as(BodyCodec.jsonObject())
                .sendBuffer(createTokenBody(), testContext.succeeding(response -> testContext.verify(() -> {

                    assertNotNull(response.body());
                    JsonObject body = JsonObject.mapFrom(response.body());
                    assertNotNull(body);

                    assertNotNull(body.getString("tokenId"));
                    assertNotEquals("", body.getString("tokenId"));

                    tokenId = TokenId.fromString(body.getString("tokenId"));

                    getTokenInfo();

                    assertNotNull(tokenInfo);
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
                .putHeader("x-api-key", apiKey)
                .as(BodyCodec.jsonObject())
                .sendBuffer(createAuctionAccountBody(), testContext.succeeding(response -> testContext.verify(() -> {

                    assertNotNull(response.body());
                    JsonObject body = JsonObject.mapFrom(response.body());
                    assertNotNull(body);

                    assertNotNull(body.getString("accountId"));
                    assertNotEquals("", body.getString("accountId"));

                    auctionAccountId = AccountId.fromString(body.getString("accountId"));

                    getAccountInfo();

                    assertNotNull(accountInfo);
                    assertEquals(auctionAccountId, accountInfo.accountId);
                    assertEquals(initialBalance, accountInfo.balance.getValue().longValue());

                    testContext.completeNow();
                })));
    }

    public void adminRestAPITransferToken(VertxTestContext testContext, String host) throws Exception {
        CreateAuctionAccount createAuctionAccount = new CreateAuctionAccount();
        RequestCreateAuctionAccount requestCreateAuctionAccount = new RequestCreateAuctionAccount();
        requestCreateAuctionAccount.initialBalance = initialBalance;
        requestCreateAuctionAccount.keylist.threshold = 1;
        RequestCreateAuctionAccountKey requestCreateAuctionAccountKey = new RequestCreateAuctionAccountKey();
        requestCreateAuctionAccountKey.key = hederaClient.client().getOperatorPublicKey().toString();
        requestCreateAuctionAccount.keylist.keys.add(requestCreateAuctionAccountKey);
        auctionAccountId = createAuctionAccount.create(requestCreateAuctionAccount);

        TransactionResponse txResponse = new TokenAssociateTransaction()
                .setAccountId(auctionAccountId)
                .setTokenIds(List.of(tokenId))
                .execute(hederaClient.client());

        txResponse.getReceipt(hederaClient.client());

        webClient.post(adminPort, host, "/v1/admin/transfer")
                .putHeader("x-api-key", apiKey)
                .as(BodyCodec.jsonObject())
                .sendBuffer(createTokenTransferBody(), testContext.succeeding(response -> testContext.verify(() -> {

                    assertNotNull(response.body());
                    JsonObject body = JsonObject.mapFrom(response.body());
                    assertNotNull(body);

                    assertNotNull(body.getString("status"));
                    assertNotEquals("", body.getString("status"));

                    assertEquals("transferred", body.getString("status"));

                    getAccountBalance();

                    assertNotNull(accountBalance);
                    Map<TokenId, Long> tokenBalances = accountBalance.token;
                    assertTrue(tokenBalances.containsKey(tokenId));
                    assertNotEquals(0, tokenBalances.get(tokenId));

                    testContext.completeNow();
                })));
    }

    public void adminRestAPIAuction(VertxTestContext testContext, String host) throws Exception {
        CreateTopic createTopic = new CreateTopic();
        topicId = createTopic.create();

        CreateToken createToken = new CreateToken(filesPath);

        RequestCreateToken requestCreateToken = new RequestCreateToken();
        requestCreateToken.setName(tokenName);
        requestCreateToken.setSymbol(symbol);
        requestCreateToken.initialSupply = initialSupply;
        requestCreateToken.decimals = decimals;
        requestCreateToken.setMemo(tokenMemo);

        tokenId = createToken.create(requestCreateToken);

        CreateAuctionAccount createAuctionAccount = new CreateAuctionAccount();
        RequestCreateAuctionAccount requestCreateAuctionAccount = new RequestCreateAuctionAccount();
        requestCreateAuctionAccount.initialBalance = initialBalance;
        requestCreateAuctionAccount.keylist.threshold = 1;
        RequestCreateAuctionAccountKey requestCreateAuctionAccountKey = new RequestCreateAuctionAccountKey();
        requestCreateAuctionAccountKey.key = hederaClient.client().getOperatorPublicKey().toString();
        requestCreateAuctionAccount.keylist.keys.add(requestCreateAuctionAccountKey);
        auctionAccountId = createAuctionAccount.create(requestCreateAuctionAccount);

        webClient.post(adminPort, host, "/v1/admin/auction")
                .putHeader("x-api-key", apiKey)
                .as(BodyCodec.jsonObject())
                .sendBuffer(createAuctionBody(), testContext.succeeding(response -> testContext.verify(() -> {

                    assertNotNull(response.body());
                    JsonObject body = JsonObject.mapFrom(response.body());
                    assertNotNull(body);

                    assertNotNull(body.getString("status"));
                    assertNotEquals("", body.getString("status"));

                    assertEquals("created", body.getString("status"));

                    getTopicInfo();

                    assertNotNull(topicInfo);
                    assertEquals(1, topicInfo.sequenceNumber);

                    testContext.completeNow();

                })));
    }

  public void validatorAPICall(VertxTestContext testContext, String host, JsonObject validatorJson) throws Exception {
    webClient.post(adminPort, host, "/v1/admin/validators")
            .putHeader("x-api-key", apiKey)
            .as(BodyCodec.jsonObject())
            .sendJson(validatorJson, testContext.succeeding(response -> testContext.verify(() -> {

              assertNotNull(response.body());
              JsonObject body = JsonObject.mapFrom(response.body());
              assertNotNull(body);

              assertEquals("success", body.getString("status"));

              testContext.completeNow();

            })));
  }
}
