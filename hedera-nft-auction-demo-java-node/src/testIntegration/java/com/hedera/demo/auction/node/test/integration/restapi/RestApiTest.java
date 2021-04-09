package com.hedera.demo.auction.node.test.integration.restapi;

import com.hedera.demo.auction.node.app.SqlConnectionManager;
import com.hedera.demo.auction.node.app.api.ApiVerticle;
import com.hedera.demo.auction.node.app.domain.Auction;
import com.hedera.demo.auction.node.app.repository.AuctionsRepository;
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

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(VertxExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class RestApiTest extends AbstractIntegrationTest {

    private PostgreSQLContainer postgres;
    private AuctionsRepository auctionsRepository;

    @BeforeAll
    public void setupDatabase() {
        PostgreSQLContainer<?> postgres = new PostgreSQLContainer("postgres:12.6");
//        postgres.setCommand("postgres -c max_connections=10");
        postgres.start();
        migrate(postgres);
        SqlConnectionManager connectionManager = new SqlConnectionManager(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
        auctionsRepository = new AuctionsRepository(connectionManager);
        this.postgres = postgres;
    }
    @AfterAll
    public void stopDatabase() {
        this.postgres.close();
    }

    @Test
    public void getAuctionTest(Vertx vertx, VertxTestContext testContext) throws IOException {
//        @SuppressWarnings("unused")
//        Checkpoint responsesReceived = context.checkpoint(1);

        Auction auction = testAuctionObject(1);
        Auction newAuction = auctionsRepository.createComplete(auction);

        WebClient webClient = WebClient.create(vertx);

        System.out.println(postgres.getJdbcUrl());
        DeploymentOptions options = getVerticleDeploymentOptions(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
        vertx.deployVerticle(new ApiVerticle(), options, testContext.succeeding(id -> {

            webClient.get(9005, "localhost", "/v1/auctions/".concat(String.valueOf(newAuction.getId())))
                    .as(BodyCodec.jsonObject())
                    .send(testContext.succeeding(response -> testContext.verify(() -> {
                        assertNotNull(response);
                        JsonObject body = JsonObject.mapFrom(response.body());
                        assertNotNull(body);

                        assertEquals(newAuction.getId(), body.getInteger("id"));
                        assertEquals(newAuction.getLastconsensustimestamp(), body.getString("lastconsensustimestamp"));
                        assertEquals(newAuction.getWinningbid(), body.getLong("winningbid"));
                        assertEquals(newAuction.getWinningaccount(), body.getString("winningaccount"));
                        assertEquals(newAuction.getWinningtimestamp(), body.getString("winningtimestamp"));
                        assertEquals(newAuction.getTokenid(), body.getString("tokenid"));
                        assertEquals(newAuction.getAuctionaccountid(), body.getString("auctionaccountid"));
                        assertEquals(newAuction.getEndtimestamp(), body.getString("endtimestamp"));
                        assertEquals(newAuction.getReserve(), body.getLong("reserve"));
                        assertEquals(newAuction.getStatus(), body.getString("status"));
                        assertEquals(newAuction.getWinningtxid(), body.getString("winningtxid"));
                        assertEquals(newAuction.getWinningtxhash(), body.getString("winningtxhash"));
                        assertEquals(newAuction.getTokenimage(), body.getString("tokenimage"));
                        assertEquals(newAuction.getMinimumbid(), body.getLong("minimumbid"));
                        assertEquals(newAuction.getStarttimestamp(), body.getString("starttimestamp"));
                        assertEquals(newAuction.getTransfertxid(), body.getString("transfertxid"));
                        assertEquals(newAuction.getTransfertxhash(), body.getString("transfertxhash"));
                        assertEquals(newAuction.isActive(), body.getBoolean("active"));
                        assertEquals(newAuction.isPending(), body.getBoolean("pending"));
                        assertEquals(newAuction.isClosed(), body.getBoolean("closed"));
                        assertEquals(newAuction.getWinnerCanBid(), body.getBoolean("winnerCanBid"));
                        assertEquals(newAuction.isTransferring(), body.getBoolean("transferring"));
                        assertEquals(newAuction.isEnded(), body.getBoolean("ended"));

                        vertx.close(testContext.succeeding());
                        auctionsRepository.deleteAllAuctions();
                        testContext.completeNow();
                    })));
        }));
    }
}
