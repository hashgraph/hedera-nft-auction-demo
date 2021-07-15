package com.hedera.demo.auction.test.integration.restapi;

import com.hedera.demo.auction.app.api.AdminApiVerticle;
import com.hedera.demo.auction.test.integration.AbstractIntegrationTest;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
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
public class PostValidatorAPITest extends AbstractIntegrationTest {

    private PostgreSQLContainer postgres;
    Vertx vertx;
    private JsonObject validators;
    private final static String url = "/v1/admin/validators";

    @BeforeAll
    public void beforeAll(VertxTestContext testContext) throws Throwable {
      this.postgres = new PostgreSQLContainer("postgres:12.6");
      this.vertx = Vertx.vertx();

      deployServerAndClient(postgres, this.vertx, testContext, new AdminApiVerticle());
    }

    @BeforeEach
    public void beforeEach() {
      basicValidator();
    }

    @AfterAll
    public void afterAll(VertxTestContext testContext) {
        this.vertx.close(testContext.completing());
        this.postgres.close();
    }

    @Test
    public void validatorWithoutKey(VertxTestContext testContext) {
      failingAdminAPITest(testContext, url, new JsonObject(), "");
    }

    @Test
    public void validatorWithoutBody(VertxTestContext testContext) {
      failingAdminAPITest(testContext, url, new JsonObject());
    }

    @Test
    public void validatorEmptyArray(VertxTestContext testContext) {
      validators.getJsonArray("validators").remove(0);

      failingAdminAPITest(testContext, url, validators);
    }

    private void validatorWithMissingData(VertxTestContext testContext, String attributeToRemove) {
      validators.getJsonArray("validators").getJsonObject(0).remove(attributeToRemove);
      failingAdminAPITest(testContext, url, validators);
    }

    @Test
    public void validatorWithMissingName(VertxTestContext testContext) {
      validatorWithMissingData(testContext, "name");
    }

    @Test
    public void validatorWithMissingOperation(VertxTestContext testContext) {
      validatorWithMissingData(testContext, "operation");
    }

    private void validatorWithLongString(VertxTestContext testContext, String attributeToUpdate) {
      validators.getJsonArray("validators").getJsonObject(0).put(attributeToUpdate, VERY_LONG_STRING);
      failingAdminAPITest(testContext, url, validators);
    }

    @Test
    public void validatorWithLongName(VertxTestContext testContext) {
      validatorWithLongString(testContext, "name");
    }

    @Test
    public void validatorWithLongNameToUpdate(VertxTestContext testContext) {
      validatorWithLongString(testContext, "nameToUpdate");
    }

  @Test
  public void validatorWithLongUrl(VertxTestContext testContext) {
    validatorWithLongString(testContext, "url");
  }

  @Test
  public void validatorWithLongPublicKey(VertxTestContext testContext) {
    validatorWithLongString(testContext, "publicKey");
  }

  @Test
  public void validatorWithLongOperation(VertxTestContext testContext) {
    validatorWithLongString(testContext, "operation");
  }

    @Test
    public void validatorWithInvalidOperation(VertxTestContext testContext) {
      validators.getJsonArray("validators").getJsonObject(0).put("operation", "noop");
      failingAdminAPITest(testContext, url, validators);
    }

  @Test
  public void validatorWithInvalidURL(VertxTestContext testContext) {
    validators.getJsonArray("validators").getJsonObject(0).put("url", "invalid");
    failingAdminAPITest(testContext, url, validators);
  }

  @Test
  public void validatorWithInvalidPublicKey(VertxTestContext testContext) {
    validators.getJsonArray("validators").getJsonObject(0).put("publicKey", "invalid");
    failingAdminAPITest(testContext, url, validators);
  }

  @Test
  public void validatorWithUpdateNoNameToUpdate(VertxTestContext testContext) {
    validators.getJsonArray("validators").getJsonObject(0).put("operation", "update");
    validators.getJsonArray("validators").getJsonObject(0).remove("nameToUpdate");
    failingAdminAPITest(testContext, url, validators);
  }

    private void basicValidator() {
      validators = new JsonObject();
      JsonArray validatorsArray = new JsonArray();
      JsonObject validator = new JsonObject();

      validator.put("name", "name");
      validator.put("nameToUpdate", "nametoupdate");
      validator.put("url", "https://hedera.com");
      validator.put("publicKey", "publicKey");
      validator.put("operation", "add");

      validatorsArray.add(validator);
      validators.put("validators", validatorsArray);
    }
}
