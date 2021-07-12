package com.hedera.demo.auction.test.integration.restapi;

import com.hedera.demo.auction.app.api.AdminApiVerticle;
import com.hedera.demo.auction.app.api.RequestCreateAuctionAccount;
import com.hedera.demo.auction.app.api.RequestCreateAuctionAccountKey;
import com.hedera.demo.auction.app.api.RequestCreateAuctionAccountKeys;
import com.hedera.demo.auction.test.integration.AbstractIntegrationTest;
import com.hedera.hashgraph.sdk.PrivateKey;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.codec.BodyCodec;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testcontainers.containers.PostgreSQLContainer;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(VertxExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class PostAuctionAccountAPITest extends AbstractIntegrationTest {

    private PostgreSQLContainer postgres;
    Vertx vertx;


  @BeforeAll
    public void beforeAll(VertxTestContext testContext) throws Throwable {
        this.postgres = new PostgreSQLContainer("postgres:12.6");
        this.postgres.start();
        migrate(this.postgres);

        this.vertx = Vertx.vertx();

        DeploymentOptions options = getVerticleDeploymentOptions(this.postgres.getJdbcUrl(), this.postgres.getUsername(), this.postgres.getPassword());
        this.vertx.deployVerticle(new AdminApiVerticle(), options, testContext.completing());

        this.webClient = WebClient.create(this.vertx);

        assertTrue(testContext.awaitCompletion(5, TimeUnit.SECONDS));
        if (testContext.failed()) {
            throw testContext.causeOfFailure();
        }

    }
    @AfterAll
    public void afterAll(VertxTestContext testContext) {
        this.vertx.close(testContext.completing());
        this.postgres.close();
    }

    @Test
    public void createAuctionAccountWithoutBody(VertxTestContext testContext) {
        webClient.post(8082, "localhost", "/v1/admin/auctionaccount")
                .as(BodyCodec.jsonObject())
                .sendBuffer(new JsonObject().toBuffer(), testContext.succeeding(response -> testContext.verify(() -> {

                    assertNull(response.body());
                    assertEquals(500, response.statusCode());
                    testContext.completeNow();
                })));
    }

    @Test
    public void createAuctionAccountWithEmptyJson(VertxTestContext testContext) {
        RequestCreateAuctionAccount requestCreateAuctionAccount = new RequestCreateAuctionAccount();

        webClient.post(8082, "localhost", "/v1/admin/auctionaccount")
                .as(BodyCodec.jsonObject())
                .sendBuffer(JsonObject.mapFrom(requestCreateAuctionAccount).toBuffer(), testContext.succeeding(response -> testContext.verify(() -> {

                    assertNull(response.body());
                    assertEquals(500, response.statusCode());
                    testContext.completeNow();
                })));
    }

    @Test
    public void createAuctionAccountWithNegativeThreshold(VertxTestContext testContext) {
        RequestCreateAuctionAccount requestCreateAuctionAccount = basicRequestCreateAuctionAccount();
        requestCreateAuctionAccount.keylist.threshold = -1;

        webClient.post(8082, "localhost", "/v1/admin/auctionaccount")
                .as(BodyCodec.jsonObject())
                .sendBuffer(JsonObject.mapFrom(requestCreateAuctionAccount).toBuffer(), testContext.succeeding(response -> testContext.verify(() -> {

                    assertNull(response.body());
                    assertEquals(500, response.statusCode());
                    testContext.completeNow();
                })));
    }

  @Test
  public void createAuctionAccountWithNegativeBalance(VertxTestContext testContext) {
    RequestCreateAuctionAccount requestCreateAuctionAccount = basicRequestCreateAuctionAccount();
    requestCreateAuctionAccount.initialBalance = -100;

    webClient.post(8082, "localhost", "/v1/admin/auctionaccount")
            .as(BodyCodec.jsonObject())
            .sendBuffer(JsonObject.mapFrom(requestCreateAuctionAccount).toBuffer(), testContext.succeeding(response -> testContext.verify(() -> {

              assertNull(response.body());
              assertEquals(500, response.statusCode());
              testContext.completeNow();
            })));
  }

  @Test
  public void createAuctionAccountWithThresholdAboveKeys(VertxTestContext testContext) {
    RequestCreateAuctionAccount requestCreateAuctionAccount = basicRequestCreateAuctionAccount();
    requestCreateAuctionAccount.keylist.threshold = 10;

    webClient.post(8082, "localhost", "/v1/admin/auctionaccount")
            .as(BodyCodec.jsonObject())
            .sendBuffer(JsonObject.mapFrom(requestCreateAuctionAccount).toBuffer(), testContext.succeeding(response -> testContext.verify(() -> {

              assertNull(response.body());
              assertEquals(500, response.statusCode());
              testContext.completeNow();
            })));
  }

  @Test
  public void createAuctionAccountWithInvalidKey(VertxTestContext testContext) {
    RequestCreateAuctionAccount requestCreateAuctionAccount = basicRequestCreateAuctionAccount();
    requestCreateAuctionAccount.keylist.threshold = 1;

    RequestCreateAuctionAccountKey requestCreateAuctionAccountKey = new RequestCreateAuctionAccountKey();
    requestCreateAuctionAccountKey.key = "1234";

    requestCreateAuctionAccount.keylist.keys.add(requestCreateAuctionAccountKey);

    webClient.post(8082, "localhost", "/v1/admin/auctionaccount")
            .as(BodyCodec.jsonObject())
            .sendBuffer(JsonObject.mapFrom(requestCreateAuctionAccount).toBuffer(), testContext.succeeding(response -> testContext.verify(() -> {

              assertNull(response.body());
              assertEquals(500, response.statusCode());
              testContext.completeNow();
            })));
  }

  @Test
  public void createAuctionAccountWithDuplicateKey(VertxTestContext testContext) {
    RequestCreateAuctionAccount requestCreateAuctionAccount = basicRequestCreateAuctionAccount();
    requestCreateAuctionAccount.keylist.threshold = 1;

    RequestCreateAuctionAccountKey requestCreateAuctionAccountKey = requestCreateAuctionAccount.keylist.keys.get(0);
    requestCreateAuctionAccount.keylist.keys.add(requestCreateAuctionAccountKey);

    webClient.post(8082, "localhost", "/v1/admin/auctionaccount")
            .as(BodyCodec.jsonObject())
            .sendBuffer(JsonObject.mapFrom(requestCreateAuctionAccount).toBuffer(), testContext.succeeding(response -> testContext.verify(() -> {

              assertNull(response.body());
              assertEquals(500, response.statusCode());
              testContext.completeNow();
            })));
  }

  @Test
  public void createAuctionAccountWithLongKey(VertxTestContext testContext) {
    RequestCreateAuctionAccount requestCreateAuctionAccount = basicRequestCreateAuctionAccount();
    requestCreateAuctionAccount.keylist.threshold = 1;

    RequestCreateAuctionAccountKey requestCreateAuctionAccountKey = new RequestCreateAuctionAccountKey();
    requestCreateAuctionAccountKey.key = LONG_KEY;
    requestCreateAuctionAccount.keylist.keys.add(requestCreateAuctionAccountKey);

    webClient.post(8082, "localhost", "/v1/admin/auctionaccount")
            .as(BodyCodec.jsonObject())
            .sendBuffer(JsonObject.mapFrom(requestCreateAuctionAccount).toBuffer(), testContext.succeeding(response -> testContext.verify(() -> {

              assertNull(response.body());
              assertEquals(500, response.statusCode());
              testContext.completeNow();
            })));
  }

  private static RequestCreateAuctionAccount basicRequestCreateAuctionAccount() {
    RequestCreateAuctionAccount requestCreateAuctionAccount = new RequestCreateAuctionAccount();
    requestCreateAuctionAccount.initialBalance = 100;
    RequestCreateAuctionAccountKeys requestCreateAuctionAccountKeys = new RequestCreateAuctionAccountKeys();
    RequestCreateAuctionAccountKey requestCreateAuctionAccountKey = new RequestCreateAuctionAccountKey();

    requestCreateAuctionAccountKey.key = PrivateKey.generate().getPublicKey().toString();
    requestCreateAuctionAccountKeys.keys.add(requestCreateAuctionAccountKey);

    requestCreateAuctionAccount.keylist = requestCreateAuctionAccountKeys;
    requestCreateAuctionAccount.keylist.threshold = 1;

    return requestCreateAuctionAccount;
  }
}
