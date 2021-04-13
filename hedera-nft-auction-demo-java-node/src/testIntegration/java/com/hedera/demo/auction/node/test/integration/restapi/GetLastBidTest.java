package com.hedera.demo.auction.node.test.integration.restapi;

import com.hedera.demo.auction.node.app.SqlConnectionManager;
import com.hedera.demo.auction.node.app.api.ApiVerticle;
import com.hedera.demo.auction.node.app.domain.Auction;
import com.hedera.demo.auction.node.app.domain.Bid;
import com.hedera.demo.auction.node.app.repository.AuctionsRepository;
import com.hedera.demo.auction.node.app.repository.BidsRepository;
import com.hedera.demo.auction.node.test.integration.AbstractIntegrationTest;
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

import java.sql.SQLException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(VertxExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class GetLastBidTest extends AbstractIntegrationTest {

    private PostgreSQLContainer postgres;
    private AuctionsRepository auctionsRepository;
    private BidsRepository bidsRepository;
    Vertx vertx;

    @BeforeAll
    public void beforeAll(VertxTestContext testContext) throws Throwable {
        this.postgres = new PostgreSQLContainer("postgres:12.6");
        this.postgres.start();
        migrate(this.postgres);
        SqlConnectionManager connectionManager = new SqlConnectionManager(this.postgres.getJdbcUrl(), this.postgres.getUsername(), this.postgres.getPassword());

        this.auctionsRepository = new AuctionsRepository(connectionManager);
        this.bidsRepository = new BidsRepository(connectionManager);

        this.vertx = Vertx.vertx();

        DeploymentOptions options = getVerticleDeploymentOptions(this.postgres.getJdbcUrl(), this.postgres.getUsername(), this.postgres.getPassword());
        this.vertx.deployVerticle(new ApiVerticle(), options, testContext.completing());

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
    public void getLastBidTest(VertxTestContext testContext) throws SQLException {
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
}
