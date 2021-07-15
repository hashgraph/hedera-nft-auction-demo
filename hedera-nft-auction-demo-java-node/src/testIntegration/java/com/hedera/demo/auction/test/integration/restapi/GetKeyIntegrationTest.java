package com.hedera.demo.auction.test.integration.restapi;

import com.hedera.demo.auction.app.api.ApiVerticle;
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

import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(VertxExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class GetKeyIntegrationTest extends AbstractIntegrationTest {

    private PostgreSQLContainer postgres;
    Vertx vertx;

    @BeforeAll
    public void beforeAll(VertxTestContext testContext) throws Throwable {
        this.postgres = new PostgreSQLContainer("postgres:12.6");
        this.vertx = Vertx.vertx();

        deployServerAndClient(postgres, this.vertx, testContext, new ApiVerticle());
    }
    @AfterAll
    public void afterAll(VertxTestContext testContext) {
        this.vertx.close(testContext.completing());
        this.postgres.close();
    }

    @Test
    public void getLastBidTest(VertxTestContext testContext) throws SQLException {
        webClient.get(9005, "localhost", "/v1/generatekey/")
                .as(BodyCodec.jsonObject())
                .send(testContext.succeeding(response -> testContext.verify(() -> {
                    assertNotNull(response);
                    JsonObject body = JsonObject.mapFrom(response.body());
                    assertNotNull(body);
                    assertNotNull(body.getString("PrivateKey"));
                    assertNotNull(body.getString("PublicKey"));

                    vertx.close(testContext.succeeding());
                    testContext.completeNow();
                })));
    }
}
