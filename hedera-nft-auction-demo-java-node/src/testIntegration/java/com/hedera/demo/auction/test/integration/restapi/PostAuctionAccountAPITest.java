package com.hedera.demo.auction.test.integration.restapi;

import com.hedera.demo.auction.app.api.AdminApiVerticle;
import com.hedera.demo.auction.app.api.RequestCreateAuctionAccount;
import com.hedera.demo.auction.app.api.RequestCreateAuctionAccountKey;
import com.hedera.demo.auction.app.api.RequestCreateAuctionAccountKeys;
import com.hedera.demo.auction.test.integration.AbstractIntegrationTest;
import com.hedera.hashgraph.sdk.PrivateKey;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testcontainers.containers.PostgreSQLContainer;

@ExtendWith(VertxExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class PostAuctionAccountAPITest extends AbstractIntegrationTest {

    private PostgreSQLContainer postgres;
    Vertx vertx;
    private final static String url = "/v1/admin/auctionaccount";
    private RequestCreateAuctionAccount requestCreateAuctionAccount;

    @BeforeAll
    public void beforeAll(VertxTestContext testContext) throws Throwable {
        this.postgres = new PostgreSQLContainer("postgres:12.6");
        this.vertx = Vertx.vertx();
        deployServerAndClient(postgres, this.vertx, testContext, new AdminApiVerticle());
    }

    @BeforeEach
    public void beforeEach() {
      basicRequestCreateAuctionAccount();
    }

    @AfterAll
    public void afterAll(VertxTestContext testContext) {
        this.vertx.close(testContext.completing());
        this.postgres.close();
    }

    @Test
    public void createAuctionAccountWithoutKey(VertxTestContext testContext) {
      failingAdminAPITest(testContext, url, new JsonObject(), "");
    }

  @Test
    public void createAuctionAccountWithoutBody(VertxTestContext testContext) {
      failingAdminAPITest(testContext, url, new JsonObject());
    }

    @Test
    public void createAuctionAccountWithNegativeThreshold(VertxTestContext testContext) {
        requestCreateAuctionAccount.keylist.threshold = -1;
        failingAdminAPITest(testContext, url, JsonObject.mapFrom(requestCreateAuctionAccount));
    }

  @Test
  public void createAuctionAccountWithNegativeBalance(VertxTestContext testContext) {
    requestCreateAuctionAccount.initialBalance = -100;
    failingAdminAPITest(testContext, url, JsonObject.mapFrom(requestCreateAuctionAccount));
  }

  @Test
  public void createAuctionAccountWithThresholdAboveKeys(VertxTestContext testContext) {
    requestCreateAuctionAccount.keylist.threshold = 10;
    failingAdminAPITest(testContext, url, JsonObject.mapFrom(requestCreateAuctionAccount));
  }

  @Test
  public void createAuctionAccountWithInvalidKey(VertxTestContext testContext) {
    requestCreateAuctionAccount.keylist.threshold = 1;

    RequestCreateAuctionAccountKey requestCreateAuctionAccountKey = new RequestCreateAuctionAccountKey();
    requestCreateAuctionAccountKey.key = "1234";

    requestCreateAuctionAccount.keylist.keys.add(requestCreateAuctionAccountKey);

    failingAdminAPITest(testContext, url, JsonObject.mapFrom(requestCreateAuctionAccount));
  }

  @Test
  public void createAuctionAccountWithDuplicateKey(VertxTestContext testContext) {
    requestCreateAuctionAccount.keylist.threshold = 1;

    RequestCreateAuctionAccountKey requestCreateAuctionAccountKey = requestCreateAuctionAccount.keylist.keys.get(0);
    requestCreateAuctionAccount.keylist.keys.add(requestCreateAuctionAccountKey);

    failingAdminAPITest(testContext, url, JsonObject.mapFrom(requestCreateAuctionAccount));
  }

  @Test
  public void createAuctionAccountWithLongKey(VertxTestContext testContext) {
    requestCreateAuctionAccount.keylist.threshold = 1;

    RequestCreateAuctionAccountKey requestCreateAuctionAccountKey = new RequestCreateAuctionAccountKey();
    requestCreateAuctionAccountKey.key = LONG_KEY;
    requestCreateAuctionAccount.keylist.keys.add(requestCreateAuctionAccountKey);

    failingAdminAPITest(testContext, url, JsonObject.mapFrom(requestCreateAuctionAccount));
  }

  private void basicRequestCreateAuctionAccount() {
    requestCreateAuctionAccount = new RequestCreateAuctionAccount();
    requestCreateAuctionAccount.initialBalance = 100;
    RequestCreateAuctionAccountKeys requestCreateAuctionAccountKeys = new RequestCreateAuctionAccountKeys();
    RequestCreateAuctionAccountKey requestCreateAuctionAccountKey = new RequestCreateAuctionAccountKey();

    requestCreateAuctionAccountKey.key = PrivateKey.generate().getPublicKey().toString();
    requestCreateAuctionAccountKeys.keys.add(requestCreateAuctionAccountKey);

    requestCreateAuctionAccount.keylist = requestCreateAuctionAccountKeys;
    requestCreateAuctionAccount.keylist.threshold = 1;
  }
}
