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
public class PostAuctionAPITest extends AbstractIntegrationTest {

    private PostgreSQLContainer postgres;
    Vertx vertx;
    private JsonObject auction;
    private final static String url = "/v1/admin/auction";

    @BeforeAll
    public void beforeAll(VertxTestContext testContext) throws Throwable {
      this.postgres = new PostgreSQLContainer("postgres:12.6");
      this.vertx = Vertx.vertx();

      deployServerAndClient(postgres, this.vertx, testContext, new AdminApiVerticle());
    }

    @BeforeEach
    public void beforeEach() {
      basicAuction();
    }

    @AfterAll
    public void afterAll(VertxTestContext testContext) {
        this.vertx.close(testContext.completing());
        this.postgres.close();
    }

    @Test
    public void createAuctionAuctionWithoutKey(VertxTestContext testContext) {
      failingAdminAPITest(testContext, url, new JsonObject(), "");
    }

  @Test
    public void createAuctionAuctionWithoutBody(VertxTestContext testContext) {
      failingAdminAPITest(testContext, url, new JsonObject());
    }

    private void createAuctionWithMissingData(VertxTestContext testContext, String attributeToRemove) {
      auction.remove(attributeToRemove);
      failingAdminAPITest(testContext, url, auction);
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
      auction.put(attributeToUpdate, VERY_LONG_STRING);
      failingAdminAPITest(testContext, url, auction);
    }

  @Test
  public void createAuctionWithLongTokenId(VertxTestContext testContext) {
    createAuctionWithLongString(testContext, "tokenid");
  }

  @Test
  public void createAuctionWithLongTopicId(VertxTestContext testContext) {
    createAuctionWithLongString(testContext, "topicid");
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
      auction.put("reserve", "abcdef");
      failingAdminAPITest(testContext, url, auction);
    }

    @Test
    public void createAuctionWithNegativeReserve(VertxTestContext testContext) {
      auction.put("reserve", -1);
      failingAdminAPITest(testContext, url, auction);
    }

    @Test
    public void createAuctionWithInvalidMinimumBid(VertxTestContext testContext) {
      auction.put("minimumbid", "abcdef");
      failingAdminAPITest(testContext, url, auction);
    }

    @Test
    public void createAuctionWithNegativeMinimumBid(VertxTestContext testContext) {
      auction.put("minimumbid", -1);
      failingAdminAPITest(testContext, url, auction);
    }

    @Test
    public void createAuctionWithNonBoolean(VertxTestContext testContext) {
      auction.put("winnercanbid", "yes they can");
      failingAdminAPITest(testContext, url, auction);
    }

    @Test
    public void createAuctionWithInvalidTokenFormat(VertxTestContext testContext) {
      auction.put("tokenid", "abcde");
      failingAdminAPITest(testContext, url, auction);
    }

    @Test
    public void createAuctionWithInvalidAccountFormat(VertxTestContext testContext) {
      auction.put("auctionaccountid", "abcde");
      failingAdminAPITest(testContext, url, auction);
    }

  @Test
  public void createAuctionWithInvalidTopicFormat(VertxTestContext testContext) {
    auction.put("topicid", "abcde");
    failingAdminAPITest(testContext, url, auction);
  }

    private void basicAuction() {
      auction = new JsonObject();
      auction.put("tokenid", "0.0.1234");
      auction.put("auctionaccountid", "0.0.1234");
      auction.put("reserve", 0);
      auction.put("minimumbid", 0);
      auction.put("endtimestamp", "");
      auction.put("winnercanbid", false);
      auction.put("title", "title");
      auction.put("description", "description");
    }
}
