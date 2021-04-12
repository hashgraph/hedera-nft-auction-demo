package com.hedera.demo.auction.node.test.integration.restapi;

import com.google.errorprone.annotations.Var;
import com.hedera.demo.auction.node.app.SqlConnectionManager;
import com.hedera.demo.auction.node.app.api.ApiVerticle;
import com.hedera.demo.auction.node.app.domain.Auction;
import com.hedera.demo.auction.node.app.domain.Bid;
import com.hedera.demo.auction.node.app.repository.AuctionsRepository;
import com.hedera.demo.auction.node.app.repository.BidsRepository;
import com.hedera.demo.auction.node.test.integration.AbstractIntegrationTest;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
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

import java.io.IOException;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(VertxExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ClientRestApiTest extends AbstractIntegrationTest {

    private PostgreSQLContainer postgres;
    private AuctionsRepository auctionsRepository;
    private BidsRepository bidsRepository;
    Vertx vertx;

    @BeforeAll
    public void beforeAll(VertxTestContext testContext) throws Throwable {
        PostgreSQLContainer<?> postgres = new PostgreSQLContainer("postgres:12.6");
        postgres.start();
        migrate(postgres);
        SqlConnectionManager connectionManager = new SqlConnectionManager(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
        this.postgres = postgres;

        auctionsRepository = new AuctionsRepository(connectionManager);
        bidsRepository = new BidsRepository(connectionManager);

        vertx = Vertx.vertx();

        DeploymentOptions options = getVerticleDeploymentOptions(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
//        vertx.deployVerticle(new ApiVerticle(), options, testContext.completing());
        vertx.deployVerticle(new ApiVerticle(), options, testContext.completing());

        webClient = WebClient.create(vertx);

        assertTrue(testContext.awaitCompletion(5, TimeUnit.SECONDS));
        if (testContext.failed()) {
            throw testContext.causeOfFailure();
        }
        System.out.println("Server started");

    }
    @AfterAll
    public void afterAll(VertxTestContext testContext) {
        vertx.close(testContext.completing());
        this.postgres.close();
    }

    @Test
    public void getAuctionTest() throws SQLException {
        VertxTestContext testContext = new VertxTestContext();
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
    public void getAuctionsTest() throws IOException, SQLException {
        VertxTestContext testContext = new VertxTestContext();
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
    public void getBidsTest() throws SQLException {
        VertxTestContext testContext = new VertxTestContext();
        Auction auction = testAuctionObject(1);
        Auction newAuction1 = auctionsRepository.createComplete(auction);

        Bid bid1 = testBidObject(1, newAuction1.getId());
        bidsRepository.add(bid1);
        bidsRepository.setRefundInProgress(bid1.getTimestamp(), bid1.getRefundtxid(), bid1.getRefundtxhash());
        bidsRepository.setRefunded(bid1.getTimestamp(), bid1.getTransactionhash());

        Bid bid2 = testBidObject(2, newAuction1.getId());
        bidsRepository.add(bid2);
        bidsRepository.setRefundInProgress(bid2.getTimestamp(), bid2.getRefundtxid(), bid2.getRefundtxhash());
        bidsRepository.setRefunded(bid2.getTimestamp(), bid2.getTransactionhash());

        Bid bid0 = testBidObject(0, newAuction1.getId());
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
                    assertEquals(bid0.getRefunded(), body.getJsonObject(2).getBoolean("refunded"));

                    bidsRepository.deleteAllBids();
                    auctionsRepository.deleteAllAuctions();
                    testContext.completeNow();
                })));
    }

    @Test
    public void getLastBidTest() throws IOException, SQLException {
        VertxTestContext testContext = new VertxTestContext();
        Auction auction = testAuctionObject(1);
        Auction newAuction1 = auctionsRepository.createComplete(auction);

        Bid bid1 = testBidObject(1, newAuction1.getId());
        bidsRepository.add(bid1);

        Bid bid2 = testBidObject(2, newAuction1.getId());
        bid2.setBidderaccountid(bid1.getBidderaccountid());
        bidsRepository.add(bid2);

        webClient.get(9005, "localhost", "/v1/lastbid/"
                .concat(String.valueOf(bid1.getAuctionid()))
                .concat("/")
                .concat(bid1.getBidderaccountid())
        )
                .as(BodyCodec.jsonObject())
                .send(testContext.succeeding(response -> testContext.verify(() -> {
                    assertNotNull(response);
                    JsonObject body = JsonObject.mapFrom(response.body());
                    assertNotNull(body);
                    assertEquals(bid2.getBidamount(), body.getLong("bidamount"));
                    assertEquals(bid2.getTransactionid(), body.getString("transactionid"));
                    assertEquals(bid2.getBidderaccountid(), body.getString("bidderaccountid"));
                    assertEquals(bid2.getTimestamp(), body.getString("timestamp"));
                    assertEquals(bid2.getAuctionid(), body.getInteger("auctionid"));

                    vertx.close(testContext.succeeding());
                    bidsRepository.deleteAllBids();
                    auctionsRepository.deleteAllAuctions();
                    testContext.completeNow();
                })));
    }

    private static void verifyBid(Bid bid, JsonObject body) {
        assertEquals(bid.getTimestamp(), body.getString("timestamp"));
        assertEquals(bid.getAuctionid(), body.getInteger("auctionid"));
        assertEquals(bid.getBidderaccountid(), body.getString("bidderaccountid"));
        assertEquals(bid.getBidamount(), body.getLong("bidamount"));
        assertEquals(bid.getStatus(), body.getString("status"));
        assertEquals(true, body.getBoolean("refunded"));
        assertEquals(bid.getRefundtxid(), body.getString("refundtxid"));
        assertEquals(bid.getRefundtxhash(), body.getString("refundtxhash"));
        assertEquals(bid.getTransactionid(), body.getString("transactionid"));
        assertEquals(bid.getTransactionhash(), body.getString("transactionhash"));
    }

    private static void verifyAuction(Auction auction, JsonObject body) {
        assertEquals(auction.getId(), body.getInteger("id"));
        assertEquals(auction.getLastconsensustimestamp(), body.getString("lastconsensustimestamp"));
        assertEquals(auction.getWinningbid(), body.getLong("winningbid"));
        assertEquals(auction.getWinningaccount(), body.getString("winningaccount"));
        assertEquals(auction.getWinningtimestamp(), body.getString("winningtimestamp"));
        assertEquals(auction.getTokenid(), body.getString("tokenid"));
        assertEquals(auction.getAuctionaccountid(), body.getString("auctionaccountid"));
        assertEquals(auction.getEndtimestamp(), body.getString("endtimestamp"));
        assertEquals(auction.getReserve(), body.getLong("reserve"));
        assertEquals(auction.getStatus(), body.getString("status"));
        assertEquals(auction.getWinningtxid(), body.getString("winningtxid"));
        assertEquals(auction.getWinningtxhash(), body.getString("winningtxhash"));
        assertEquals(auction.getTokenimage(), body.getString("tokenimage"));
        assertEquals(auction.getMinimumbid(), body.getLong("minimumbid"));
        assertEquals(auction.getStarttimestamp(), body.getString("starttimestamp"));
        assertEquals(auction.getTransfertxid(), body.getString("transfertxid"));
        assertEquals(auction.getTransfertxhash(), body.getString("transfertxhash"));
        assertEquals(auction.isActive(), body.getBoolean("active"));
        assertEquals(auction.isPending(), body.getBoolean("pending"));
        assertEquals(auction.isClosed(), body.getBoolean("closed"));
        assertEquals(auction.getWinnerCanBid(), body.getBoolean("winnerCanBid"));
        assertEquals(auction.isTransferring(), body.getBoolean("transferring"));
        assertEquals(auction.isEnded(), body.getBoolean("ended"));
    }
}
