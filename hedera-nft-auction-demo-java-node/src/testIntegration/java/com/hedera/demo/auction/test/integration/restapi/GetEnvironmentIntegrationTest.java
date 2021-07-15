package com.hedera.demo.auction.test.integration.restapi;

import com.hedera.demo.auction.app.SqlConnectionManager;
import com.hedera.demo.auction.app.api.ApiVerticle;
import com.hedera.demo.auction.app.repository.ValidatorsRepository;
import com.hedera.demo.auction.test.integration.AbstractIntegrationTest;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
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

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(VertxExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class GetEnvironmentIntegrationTest extends AbstractIntegrationTest {

    private PostgreSQLContainer postgres;
    Vertx vertx;
    ValidatorsRepository validatorsRepository;

    @BeforeAll
    public void beforeAll(VertxTestContext testContext) throws Throwable {
        this.postgres = new PostgreSQLContainer("postgres:12.6");
        this.vertx = Vertx.vertx();

        deployServerAndClient(postgres, this.vertx, testContext, new ApiVerticle());
        SqlConnectionManager connectionManager = new SqlConnectionManager(this.postgres.getJdbcUrl(), this.postgres.getUsername(), this.postgres.getPassword());
        validatorsRepository = new ValidatorsRepository(connectionManager);
    }

    @AfterAll
    public void afterAll(VertxTestContext testContext) {
        this.vertx.close(testContext.completing());
        this.postgres.close();
    }

    @Test
    public void getEnvironmentTest(VertxTestContext testContext) throws SQLException {
      validatorsRepository.add("validatorName", "https://hedera.com", "validatorPublicKey");
      validatorsRepository.add("validatorName1", "https://hedera.com1", "validatorPublicKey1");

        webClient.get(9005, "localhost", "/v1/environment/")
                .as(BodyCodec.jsonObject())
                .send(testContext.succeeding(response -> testContext.verify(() -> {

                    assertNotNull(response.body());
                    JsonObject body = JsonObject.mapFrom(response.body());
                    assertNotNull(body);

                    assertEquals("ATopicId", body.getString("topicId"));
                    assertEquals(env.get("NODE_OWNER"), body.getString("nodeOperator"));
                    assertEquals(env.get("NEXT_PUBLIC_NETWORK"), body.getString("network"));

                    assertTrue(body.containsKey("validators"));
                    JsonArray validators = body.getJsonArray("validators");
                    assertEquals(2, validators.size());
                    JsonObject validator = validators.getJsonObject(0);
                    assertEquals("validatorName", validator.getString("name"));
                    assertEquals("https://hedera.com", validator.getString("url"));
                    assertEquals("validatorPublicKey", validator.getString("publicKey"));

                    JsonObject validator2 = validators.getJsonObject(1);
                    assertEquals("validatorName1", validator2.getString("name"));
                    assertEquals("https://hedera.com1", validator2.getString("url"));
                    assertEquals("validatorPublicKey1", validator2.getString("publicKey"));

                    testContext.completeNow();
                })));
    }
}
