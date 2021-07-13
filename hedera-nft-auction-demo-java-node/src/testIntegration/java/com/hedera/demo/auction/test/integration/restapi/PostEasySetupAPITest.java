package com.hedera.demo.auction.test.integration.restapi;

import com.hedera.demo.auction.app.api.AdminApiVerticle;
import com.hedera.demo.auction.test.integration.AbstractIntegrationTest;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testcontainers.containers.PostgreSQLContainer;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(VertxExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class PostEasySetupAPITest extends AbstractIntegrationTest {

    private PostgreSQLContainer postgres;
    private JsonObject setup;
    Vertx vertx;
    private final static String url = "/v1/admin/easysetup";

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

    @BeforeEach
    public void beforeEach() {
      basicSetup();
    }

    @AfterAll
    public void afterAll(VertxTestContext testContext) {
        this.vertx.close(testContext.completing());
        this.postgres.close();
    }

    @Test
    public void createEasySetupWithoutBody(VertxTestContext testContext) {
      failingAdminAPITest(testContext, url, new JsonObject());
    }

    private void createEasySetupWithMissingData(VertxTestContext testContext, String attributeToRemove) {
      setup.remove(attributeToRemove);
      failingAdminAPITest(testContext, url, setup);
    }

    @Test
    public void createEasySetupWithMissingName(VertxTestContext testContext) {
      createEasySetupWithMissingData(testContext, "name");
    }

    @Test
    public void createEasySetupWithMissingSymbol(VertxTestContext testContext) {
      createEasySetupWithMissingData(testContext, "symbol");
    }

  @Test
  public void createEasySetupWithMissingClean(VertxTestContext testContext) {
    createEasySetupWithMissingData(testContext, "clean");
  }

    private void createEasySetupWithLongString(VertxTestContext testContext, String attributeToUpdate) {
      setup.put(attributeToUpdate, VERY_LONG_STRING);
      failingAdminAPITest(testContext, url, setup);
    }

    @Test
    public void createEasySetupWithLongName(VertxTestContext testContext) {
      createEasySetupWithLongString(testContext, "name");
    }

    @Test
    public void createEasySetupWithLongSymbol(VertxTestContext testContext) {
      createEasySetupWithLongString(testContext, "symbol");
    }

    @Test
    public void createEasySetupWithInvalidClean(VertxTestContext testContext) {
      setup.put("clean", "abcdef");
      failingAdminAPITest(testContext, url, setup);
    }

    private void basicSetup() {
      setup = new JsonObject();
      setup.put("symbol", "symbol");
      setup.put("name", "name");
      setup.put("clean", false);
    }
}
