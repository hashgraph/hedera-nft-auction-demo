package com.hedera.demo.auction.test.integration.restapi;

import com.google.errorprone.annotations.Var;
import com.hedera.demo.auction.app.SqlConnectionManager;
import com.hedera.demo.auction.app.api.ApiVerticle;
import com.hedera.demo.auction.app.domain.Auction;
import com.hedera.demo.auction.app.domain.Bid;
import com.hedera.demo.auction.app.repository.AuctionsRepository;
import com.hedera.demo.auction.app.repository.BidsRepository;
import com.hedera.demo.auction.test.integration.AbstractIntegrationTest;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
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

@ExtendWith(VertxExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class GetAuctionsIntegrationTest extends AbstractIntegrationTest {

    private PostgreSQLContainer postgres;
    private AuctionsRepository auctionsRepository;
    private BidsRepository bidsRepository;
    Vertx vertx;

    @BeforeAll
    public void beforeAll(VertxTestContext testContext) throws Throwable {
        this.postgres = new PostgreSQLContainer("postgres:12.6");
        this.vertx = Vertx.vertx();

        deployServerAndClient(postgres, this.vertx, testContext, new ApiVerticle());

        SqlConnectionManager connectionManager = new SqlConnectionManager(this.postgres.getJdbcUrl(), this.postgres.getUsername(), this.postgres.getPassword());
        this.auctionsRepository = new AuctionsRepository(connectionManager);
        this.bidsRepository = new BidsRepository(connectionManager);
    }
    @AfterAll
    public void afterAll(VertxTestContext testContext) {
        this.vertx.close(testContext.completing());
        this.postgres.close();
    }

    @Test
    public void getAuctionsTest(VertxTestContext testContext) throws SQLException {
        @Var Auction auction = testAuctionObject(1);
        Auction newAuction1 = auctionsRepository.createComplete(auction);
        auction = testAuctionObject(2);
        Auction newAuction2 = auctionsRepository.createComplete(auction);

        webClient.get(9005, "localhost", "/v1/auctions/")
                .as(BodyCodec.buffer())
                .send(testContext.succeeding(response -> testContext.verify(() -> {
                    assertNotNull(response);
                    JsonArray body = new JsonArray(response.body());
                    assertNotNull(body);
                    assertEquals(2, body.size());
                    verifyAuction(newAuction1, body.getJsonObject(0));
                    verifyAuction(newAuction2, body.getJsonObject(1));

                    auctionsRepository.deleteAllAuctions();
                    testContext.completeNow();
                })));
    }

    @Test
    public void getPendingAuctionsTest(VertxTestContext testContext) throws SQLException {
        getAuction(testContext,  "/v1/pendingauctions/", Auction.PENDING, 2);
    }

    @Test
    public void getActiveAuctionsTest(VertxTestContext testContext) throws SQLException {
        getAuction(testContext,  "/v1/activeauctions/", Auction.ACTIVE, 1);
    }

    @Test
    public void getClosedAuctionsTest(VertxTestContext testContext) throws SQLException {
        getAuction(testContext,  "/v1/closedauctions/", Auction.CLOSED, 1);
    }

    @Test
    public void getEndedAuctionsTest(VertxTestContext testContext) throws SQLException {
        getAuction(testContext,  "/v1/endedauctions/", Auction.ENDED, 1);
    }

    @Test
    public void getAuctionReserveNotMet(VertxTestContext testContext) throws SQLException {
        // reserve not met auction
        @Var Auction auction = testAuctionObject(1);
        auction.setWinningbid(0L);
        auction.setReserve(5L);
        auction = auctionsRepository.createComplete(auction);

        Bid bid = testBidObject(1, auction.getId());
        bid.setStatus("Bid below reserve");
        bidsRepository.add(bid);

        // reserve met auction
        Auction newAuction = testAuctionObject(2);
        newAuction.setWinningbid(2L);
        newAuction.setReserve(0L);
        auctionsRepository.createComplete(newAuction);

        Auction finalAuction = auction;
        webClient.get(9005, "localhost", "/v1/reservenotmetauctions")
                .as(BodyCodec.buffer())
                .send(testContext.succeeding(response -> testContext.verify(() -> {
                    assertNotNull(response);
                    JsonArray body = new JsonArray(response.body());
                    assertNotNull(body);
                    assertEquals(1, body.size());
                    verifyAuction(finalAuction, body.getJsonObject(0));

                    bidsRepository.deleteAllBids();
                    auctionsRepository.deleteAllAuctions();
                    testContext.completeNow();
                })));
    }

  @Test
  public void getAuctionSold(VertxTestContext testContext) throws SQLException {
    // reserve met auction
    Auction auction = testAuctionObject(1);
    auction.setStatus(Auction.ENDED);
    auction.setWinningbid(6L);
    auction.setReserve(5L);
    auctionsRepository.createComplete(auction);
    // ended not sold
    Auction newAuction = testAuctionObject(2);
    newAuction.setWinningbid(0L);
    newAuction.setReserve(0L);
    newAuction.setStatus(Auction.ENDED);
    auctionsRepository.createComplete(newAuction);
    // ended below reserve
    Auction newAuction2 = testAuctionObject(3);
    newAuction2.setWinningbid(1L);
    newAuction2.setReserve(5L);
    newAuction2.setStatus(Auction.ENDED);
    auctionsRepository.createComplete(newAuction2);

    webClient.get(9005, "localhost", "/v1/soldauctions")
            .as(BodyCodec.buffer())
            .send(testContext.succeeding(response -> testContext.verify(() -> {
              assertNotNull(response);
              JsonArray body = new JsonArray(response.body());
              assertNotNull(body);
              assertEquals(1, body.size());
              verifyAuction(auction, body.getJsonObject(0));

              auctionsRepository.deleteAllAuctions();
              testContext.completeNow();
            })));
  }

  public void getAuction(VertxTestContext testContext, String url, String status, int expectedCount) throws SQLException {
        Auction auction = testAuctionObject(1);
        auctionsRepository.createComplete(auction);
        @Var Auction newAuction = testAuctionObject(2);
        newAuction.setStatus(status);
        newAuction = auctionsRepository.createComplete(newAuction);

        Auction finalNewAuction = newAuction;
        webClient.get(9005, "localhost", url)
                .as(BodyCodec.buffer())
                .send(testContext.succeeding(response -> testContext.verify(() -> {
                    assertNotNull(response);
                    JsonArray body = new JsonArray(response.body());
                    assertNotNull(body);
                    assertEquals(expectedCount, body.size());
                    verifyAuction(finalNewAuction, body.getJsonObject(expectedCount - 1));

                    auctionsRepository.deleteAllAuctions();
                    testContext.completeNow();
                })));
    }
}
