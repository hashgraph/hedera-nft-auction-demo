package com.hedera.demo.auction.test.integration.restapi;

import com.hedera.demo.auction.app.api.AdminApiVerticle;
import com.hedera.demo.auction.test.integration.AbstractIntegrationTest;
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
public class PostTransferApiTest extends AbstractIntegrationTest {

    private PostgreSQLContainer<?> postgres;
    Vertx vertx;
    private JsonObject transfer;
    private final static String url = "/v1/admin/transfer";

    @BeforeAll
    public void beforeAll(VertxTestContext testContext) throws Throwable {
        this.postgres = new PostgreSQLContainer<>("POSTGRES_CONTAINER_VERSION");
      this.postgres.start();
      migrate(postgres);
        this.vertx = Vertx.vertx();

        deployServerAndClient(postgres, this.vertx, testContext, new AdminApiVerticle());
  }

    @BeforeEach
    public void beforeEach() {
      basicTransfer();
    }

    @AfterAll
    public void afterAll(VertxTestContext testContext) {
        this.vertx.close(testContext.completing());
        this.postgres.close();
    }

    @Test
    public void createTransferWithoutKey(VertxTestContext testContext) {
      failingAdminApiTest(testContext, url, new JsonObject(), "");
    }

    @Test
    public void createTransferWithoutBody(VertxTestContext testContext) {
      failingAdminApiTest(testContext, url, new JsonObject());
    }

  private void createTransferWithMissingData(VertxTestContext testContext, String attributeToRemove) {
      transfer.remove(attributeToRemove);
      failingAdminApiTest(testContext, url, transfer);
    }

    @Test
    public void createTransferWithMissingTokenId(VertxTestContext testContext) {
      createTransferWithMissingData(testContext, "tokenid");
    }

    @Test
    public void createTransferWithMissingAccount(VertxTestContext testContext) {
      createTransferWithMissingData(testContext, "auctionaccountid");
    }

    private void createTransferWithLongString(VertxTestContext testContext, String attributeToUpdate) {
      transfer.put(attributeToUpdate, VERY_LONG_STRING);
      failingAdminApiTest(testContext, url, transfer);
    }

    @Test
    public void createTransferWithLongTokenId(VertxTestContext testContext) {
      createTransferWithLongString(testContext, "tokenid");
    }

    @Test
    public void createTransferWithLongAccount(VertxTestContext testContext) {
      createTransferWithLongString(testContext, "auctionaccountid");
    }

    @Test
    public void createTransferWithInvalidTokenId(VertxTestContext testContext) {
      transfer.put("tokenid", "abcdef");
      failingAdminApiTest(testContext, url, transfer);
    }

  @Test
  public void createTransferWithInvalidAccountId(VertxTestContext testContext) {
    transfer.put("auctionaccountid", "abcdef");
    failingAdminApiTest(testContext, url, transfer);
  }

    private void basicTransfer() {
      transfer = new JsonObject();
      transfer.put("tokenid", "0.0.1234");
      transfer.put("auctionaccountid", "0.0.1234");
    }
}
