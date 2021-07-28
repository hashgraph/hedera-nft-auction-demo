package com.hedera.demo.auction.test.integration.restapi;

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
import static org.junit.jupiter.api.Assertions.assertNull;

@ExtendWith(VertxExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class GetBidsIntegrationTest extends AbstractIntegrationTest {

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
    public void getBidsTest(VertxTestContext testContext) throws SQLException {
        Auction auction = testAuctionObject(1);
        Auction newAuction1 = auctionsRepository.createComplete(auction);

        Bid bid1 = testBidObject(1, newAuction1.getId());
        bidsRepository.add(bid1);
        bidsRepository.setRefundIssued(bid1.getTimestamp(), "", "");
        bidsRepository.setRefunded(bid1.getTransactionid(), bid1.getRefundtxid(), bid1.getRefundtxhash());
        bid1.setRefundstatus(Bid.REFUND_REFUNDED);

        Bid bid2 = testBidObject(2, newAuction1.getId());
        bidsRepository.add(bid2);
        bidsRepository.setRefundIssued(bid2.getTimestamp(), "", "");
        bidsRepository.setRefunded(bid2.getTransactionid(), bid2.getRefundtxid(), bid2.getRefundtxhash());
        bid2.setRefundstatus(Bid.REFUND_REFUNDED);

        Bid bid0 = testBidObject(0, newAuction1.getId());
        bid0.setRefundstatus(Bid.REFUND_PENDING);
        bidsRepository.add(bid0);

        webClient.get(9005, "localhost", "/v1/bids/".concat(String.valueOf(bid1.getAuctionid())))
                .as(BodyCodec.buffer())
                .send(testContext.succeeding(response -> testContext.verify(() -> {
                    assertNotNull(response);
                    JsonArray body = new JsonArray(response.body());
                    assertNotNull(body);
                    assertEquals(3, body.size());
                    verifyBid(bid1, body.getJsonObject(1));
                    verifyBid(bid2, body.getJsonObject(0));
                    // test for bid that has not been refunded yet
                    assertEquals(bid0.getRefundstatus(), body.getJsonObject(2).getString("refundstatus"));

                    bidsRepository.deleteAllBids();
                    auctionsRepository.deleteAllAuctions();
                    testContext.completeNow();
                })));
    }

  @Test
  public void getBidsInvalidAuctionId(VertxTestContext testContext) {
    webClient.get(9005, "localhost", "/v1/bids/abc")
            .as(BodyCodec.buffer())
            .send(testContext.succeeding(response -> testContext.verify(() -> {
              assertNull(response.body());
              assertEquals(500, response.statusCode());
              testContext.completeNow();
            })));
  }

  @Test
  public void getBidsAuctionIdZero(VertxTestContext testContext) {
    webClient.get(9005, "localhost", "/v1/bids/0")
            .as(BodyCodec.buffer())
            .send(testContext.succeeding(response -> testContext.verify(() -> {
              assertNull(response.body());
              assertEquals(500, response.statusCode());
              testContext.completeNow();
            })));
  }
}
