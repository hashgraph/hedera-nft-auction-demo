package com.hedera.demo.auction.test.integration.restapi;

import com.hedera.demo.auction.app.api.AdminApiVerticle;
import com.hedera.demo.auction.test.integration.AbstractIntegrationTest;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testcontainers.containers.PostgreSQLContainer;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(VertxExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class PostTransferAPITest extends AbstractIntegrationTest {

    private PostgreSQLContainer postgres;
    Vertx vertx;
    private JsonObject transfer;
    private final static String url = "/v1/admin/transfer";

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
    public void createTransferWithoutBody(VertxTestContext testContext) {
      failingAdminAPITest(testContext, url, new JsonObject());
    }

    private void createTransferWithMissingData(VertxTestContext testContext, String attributeToRemove) {
      transfer.remove(attributeToRemove);
      failingAdminAPITest(testContext, url, transfer);
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
      failingAdminAPITest(testContext, url, transfer);
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
      failingAdminAPITest(testContext, url, transfer);
    }

  @Test
  public void createTransferWithInvalidAccountId(VertxTestContext testContext) {
    transfer.put("auctionaccountid", "abcdef");
    failingAdminAPITest(testContext, url, transfer);
  }

    private void basicTransfer() {
      transfer = new JsonObject();
      transfer.put("tokenid", "0.0.1234");
      transfer.put("auctionaccountid", "0.0.1234");
    }
}
