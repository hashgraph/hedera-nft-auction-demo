package com.hedera.demo.auction.test.integration.restapi;

import com.hedera.demo.auction.app.SqlConnectionManager;
import com.hedera.demo.auction.app.api.ApiVerticle;
import com.hedera.demo.auction.app.domain.Auction;
import com.hedera.demo.auction.app.repository.AuctionsRepository;
import com.hedera.demo.auction.test.integration.AbstractIntegrationTest;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.codec.BodyCodec;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testcontainers.containers.PostgreSQLContainer;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@ExtendWith(VertxExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class GetAuctionIntegrationTest extends AbstractIntegrationTest {

    private PostgreSQLContainer postgres;
    private AuctionsRepository auctionsRepository;
    Vertx vertx;

    @BeforeAll
    public void beforeAll(VertxTestContext testContext) throws Throwable {
        this.postgres = new PostgreSQLContainer("postgres:12.6");
        this.vertx = Vertx.vertx();

        deployServerAndClient(postgres, this.vertx, testContext, new ApiVerticle());

        SqlConnectionManager connectionManager = new SqlConnectionManager(this.postgres.getJdbcUrl(), this.postgres.getUsername(), this.postgres.getPassword());
        this.auctionsRepository = new AuctionsRepository(connectionManager);
    }
    @AfterAll
    public void afterAll(VertxTestContext testContext) {
        this.vertx.close(testContext.completing());
        this.postgres.close();
    }

    @Test
    public void getAuctionTest(VertxTestContext testContext) throws SQLException {
        Auction auction = testAuctionObject(1);
        Auction newAuction = auctionsRepository.createComplete(auction);
        webClient.get(9005, "localhost", "/v1/auctions/".concat(String.valueOf(newAuction.getId())))
                .as(BodyCodec.jsonObject())
                .send(testContext.succeeding(response -> testContext.verify(() -> {

                    assertNotNull(response.body());
                    JsonObject body = JsonObject.mapFrom(response.body());
                    assertNotNull(body);

                    verifyAuction(newAuction, body);

                    auctionsRepository.deleteAllAuctions();

                    testContext.completeNow();
                })));
    }

  @Test
  public void getAuctionInvalidId(VertxTestContext testContext) throws SQLException {
    webClient.get(9005, "localhost", "/v1/auctions/abc")
            .as(BodyCodec.jsonObject())
            .send(testContext.succeeding(response -> testContext.verify(() -> {

              assertNull(response.body());
              assertEquals(500, response.statusCode());
              testContext.completeNow();
            })));
  }
}
