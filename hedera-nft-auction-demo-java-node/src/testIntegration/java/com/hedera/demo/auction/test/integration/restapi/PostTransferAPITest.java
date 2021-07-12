package com.hedera.demo.auction.test.integration.restapi;

import com.hedera.demo.auction.app.api.AdminApiVerticle;
import com.hedera.demo.auction.test.integration.AbstractIntegrationTest;
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
public class PostTransferAPITest extends AbstractIntegrationTest {

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
    public void createTransferWithoutBody(VertxTestContext testContext) {
        webClient.post(8082, "localhost", "/v1/admin/transfer")
                .as(BodyCodec.jsonObject())
                .sendBuffer(new JsonObject().toBuffer(), testContext.succeeding(response -> testContext.verify(() -> {

                    assertNull(response.body());
                    assertEquals(500, response.statusCode());
                    testContext.completeNow();
                })));
    }

    private void createTransferWithMissingData(VertxTestContext testContext, String attributeToRemove) {
      JsonObject auction = basicTransfer();
      auction.remove(attributeToRemove);

      webClient.post(8082, "localhost", "/v1/admin/transfer")
              .as(BodyCodec.jsonObject())
              .sendBuffer(JsonObject.mapFrom(auction).toBuffer(), testContext.succeeding(response -> testContext.verify(() -> {

                assertNull(response.body());
                assertEquals(500, response.statusCode());
                testContext.completeNow();
              })));
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
      JsonObject auction = basicTransfer();
      auction.put(attributeToUpdate, VERY_LONG_STRING);

      webClient.post(8082, "localhost", "/v1/admin/transfer")
              .as(BodyCodec.jsonObject())
              .sendBuffer(JsonObject.mapFrom(auction).toBuffer(), testContext.succeeding(response -> testContext.verify(() -> {

                assertNull(response.body());
                assertEquals(500, response.statusCode());
                testContext.completeNow();
              })));
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
      JsonObject auction = basicTransfer();
      auction.put("tokenid", "abcdef");

      webClient.post(8082, "localhost", "/v1/admin/transfer")
              .as(BodyCodec.jsonObject())
              .sendBuffer(JsonObject.mapFrom(auction).toBuffer(), testContext.succeeding(response -> testContext.verify(() -> {

                assertNull(response.body());
                assertEquals(500, response.statusCode());
                testContext.completeNow();
              })));

    }

  @Test
  public void createTransferWithInvalidAccountId(VertxTestContext testContext) {
    JsonObject auction = basicTransfer();
    auction.put("auctionaccountid", "abcdef");

    webClient.post(8082, "localhost", "/v1/admin/transfer")
            .as(BodyCodec.jsonObject())
            .sendBuffer(JsonObject.mapFrom(auction).toBuffer(), testContext.succeeding(response -> testContext.verify(() -> {

              assertNull(response.body());
              assertEquals(500, response.statusCode());
              testContext.completeNow();
            })));

  }

    private static JsonObject basicTransfer() {
      JsonObject setup = new JsonObject();
      setup.put("tokenid", "0.0.1234");
      setup.put("auctionaccountid", "0.0.1234");

      return setup;
    }
}
