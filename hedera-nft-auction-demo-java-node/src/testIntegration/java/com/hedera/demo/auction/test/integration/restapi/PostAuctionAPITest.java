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
public class PostAuctionAPITest extends AbstractIntegrationTest {

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
    public void createAuctionAuctionWithoutBody(VertxTestContext testContext) {
        webClient.post(8082, "localhost", "/v1/admin/auction")
                .as(BodyCodec.jsonObject())
                .sendBuffer(new JsonObject().toBuffer(), testContext.succeeding(response -> testContext.verify(() -> {

                    assertNull(response.body());
                    assertEquals(500, response.statusCode());
                    testContext.completeNow();
                })));
    }

    private void createAuctionWithMissingData(VertxTestContext testContext, String attributeToRemove) {
      JsonObject auction = basicAuction();
      auction.remove(attributeToRemove);

      webClient.post(8082, "localhost", "/v1/admin/auction")
              .as(BodyCodec.jsonObject())
              .sendBuffer(JsonObject.mapFrom(auction).toBuffer(), testContext.succeeding(response -> testContext.verify(() -> {

                assertNull(response.body());
                assertEquals(500, response.statusCode());
                testContext.completeNow();
              })));
    }

    @Test
      public void createAuctionWithMissingTokenId(VertxTestContext testContext) {
        createAuctionWithMissingData(testContext, "tokenid");
      }

    @Test
    public void createAuctionWithMissingAccountId(VertxTestContext testContext) {
      createAuctionWithMissingData(testContext, "auctionaccountid");
    }

    @Test
    public void createAuctionWithMissingReserve(VertxTestContext testContext) {
      createAuctionWithMissingData(testContext, "reserve");
    }

    @Test
    public void createAuctionWithMissingMinimumBid(VertxTestContext testContext) {
      createAuctionWithMissingData(testContext, "minimumbid");
    }

    @Test
    public void createAuctionWithMissingWinnerCanBid(VertxTestContext testContext) {
      createAuctionWithMissingData(testContext, "winnercanbid");
    }

    @Test
    public void createAuctionWithMissingTitle(VertxTestContext testContext) {
      createAuctionWithMissingData(testContext, "title");
    }

    @Test
    public void createAuctionWithMissingDescription(VertxTestContext testContext) {
      createAuctionWithMissingData(testContext, "description");
    }

    private void createAuctionWithLongString(VertxTestContext testContext, String attributeToUpdate) {
      JsonObject auction = basicAuction();
      auction.put(attributeToUpdate, VERY_LONG_STRING);

      webClient.post(8082, "localhost", "/v1/admin/auction")
              .as(BodyCodec.jsonObject())
              .sendBuffer(JsonObject.mapFrom(auction).toBuffer(), testContext.succeeding(response -> testContext.verify(() -> {

                assertNull(response.body());
                assertEquals(500, response.statusCode());
                testContext.completeNow();
              })));
    }

    @Test
    public void createAuctionWithLongTokenId(VertxTestContext testContext) {
      createAuctionWithLongString(testContext, "tokenid");
    }

    @Test
    public void createAuctionWithLongAccountId(VertxTestContext testContext) {
      createAuctionWithLongString(testContext, "auctionaccountid");
    }

    @Test
    public void createAuctionWithLongTitle(VertxTestContext testContext) {
      createAuctionWithLongString(testContext, "title");
    }

    @Test
    public void createAuctionWithLongDescription(VertxTestContext testContext) {
      createAuctionWithLongString(testContext, "description");
    }

  @Test
  public void createAuctionWithLongEndTimestamp(VertxTestContext testContext) {
    createAuctionWithLongString(testContext, "endtimestamp");
  }

    @Test
    public void createAuctionWithInvalidReserve(VertxTestContext testContext) {
      JsonObject auction = basicAuction();
      auction.put("reserve", "abcdef");

      webClient.post(8082, "localhost", "/v1/admin/auction")
              .as(BodyCodec.jsonObject())
              .sendBuffer(JsonObject.mapFrom(auction).toBuffer(), testContext.succeeding(response -> testContext.verify(() -> {

                assertNull(response.body());
                assertEquals(500, response.statusCode());
                testContext.completeNow();
              })));
    }

    @Test
    public void createAuctionWithNegativeReserve(VertxTestContext testContext) {
      JsonObject auction = basicAuction();
      auction.put("reserve", -1);

      webClient.post(8082, "localhost", "/v1/admin/auction")
              .as(BodyCodec.jsonObject())
              .sendBuffer(JsonObject.mapFrom(auction).toBuffer(), testContext.succeeding(response -> testContext.verify(() -> {

                assertNull(response.body());
                assertEquals(500, response.statusCode());
                testContext.completeNow();
              })));
    }

    @Test
    public void createAuctionWithInvalidMinimumBid(VertxTestContext testContext) {
      JsonObject auction = basicAuction();
      auction.put("minimumbid", "abcdef");

      webClient.post(8082, "localhost", "/v1/admin/auction")
              .as(BodyCodec.jsonObject())
              .sendBuffer(JsonObject.mapFrom(auction).toBuffer(), testContext.succeeding(response -> testContext.verify(() -> {

                assertNull(response.body());
                assertEquals(500, response.statusCode());
                testContext.completeNow();
              })));
    }

    @Test
    public void createAuctionWithNegativeMinimumBid(VertxTestContext testContext) {
      JsonObject auction = basicAuction();
      auction.put("minimumbid", -1);

      webClient.post(8082, "localhost", "/v1/admin/auction")
              .as(BodyCodec.jsonObject())
              .sendBuffer(JsonObject.mapFrom(auction).toBuffer(), testContext.succeeding(response -> testContext.verify(() -> {

                assertNull(response.body());
                assertEquals(500, response.statusCode());
                testContext.completeNow();
              })));
    }

    @Test
    public void createAuctionWithNonBoolean(VertxTestContext testContext) {
      JsonObject auction = basicAuction();
      auction.put("winnercanbid", "yes they can");

      webClient.post(8082, "localhost", "/v1/admin/auction")
              .as(BodyCodec.jsonObject())
              .sendBuffer(JsonObject.mapFrom(auction).toBuffer(), testContext.succeeding(response -> testContext.verify(() -> {

                assertNull(response.body());
                assertEquals(500, response.statusCode());
                testContext.completeNow();
              })));
    }

    @Test
    public void createAuctionWithInvalidTokenFormat(VertxTestContext testContext) {
      JsonObject auction = basicAuction();
      auction.put("tokenid", "abcde");

      webClient.post(8082, "localhost", "/v1/admin/auction")
              .as(BodyCodec.jsonObject())
              .sendBuffer(JsonObject.mapFrom(auction).toBuffer(), testContext.succeeding(response -> testContext.verify(() -> {

                assertNull(response.body());
                assertEquals(500, response.statusCode());
                testContext.completeNow();
              })));
    }

    @Test
    public void createAuctionWithInvalidAccountFormat(VertxTestContext testContext) {
      JsonObject auction = basicAuction();
      auction.put("auctionaccountid", "abcde");

      webClient.post(8082, "localhost", "/v1/admin/auction")
              .as(BodyCodec.jsonObject())
              .sendBuffer(JsonObject.mapFrom(auction).toBuffer(), testContext.succeeding(response -> testContext.verify(() -> {

                assertNull(response.body());
                assertEquals(500, response.statusCode());
                testContext.completeNow();
              })));
    }
    private static JsonObject basicAuction() {
      JsonObject auction = new JsonObject();
      auction.put("tokenid", "0.0.1234");
      auction.put("auctionaccountid", "0.0.1234");
      auction.put("reserve", 0);
      auction.put("minimumbid", 0);
      auction.put("endtimestamp", "");
      auction.put("winnercanbid", false);
      auction.put("title", "title");
      auction.put("description", "description");

      return auction;
    }
}
