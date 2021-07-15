package com.hedera.demo.auction.test.integration.restapi;

import com.hedera.demo.auction.app.api.AdminApiVerticle;
import com.hedera.demo.auction.test.integration.AbstractIntegrationTest;
import io.vertx.core.Vertx;
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
public class PostCreateTokenAPITest extends AbstractIntegrationTest {

    private PostgreSQLContainer postgres;
    Vertx vertx;
    private final static String url = "/v1/admin/token";
    private JsonObject token;

    @BeforeAll
    public void beforeAll(VertxTestContext testContext) throws Throwable {
      this.postgres = new PostgreSQLContainer("postgres:12.6");
      this.vertx = Vertx.vertx();

      deployServerAndClient(postgres, this.vertx, testContext, new AdminApiVerticle());
    }

    @BeforeEach
    public void beforEach() {
      basicToken();
    }

    @AfterAll
    public void afterAll(VertxTestContext testContext) {
        this.vertx.close(testContext.completing());
        this.postgres.close();
    }

    @Test
    public void createTokenWithoutKey(VertxTestContext testContext) {
      failingAdminAPITest(testContext, url, new JsonObject(), "");
    }

  @Test
    public void createTokenWithoutBody(VertxTestContext testContext) {
      failingAdminAPITest(testContext, url, new JsonObject());
    }

    private void createTokenWithMissingData(VertxTestContext testContext, String attributeToRemove) {
      token.remove(attributeToRemove);
      failingAdminAPITest(testContext, url, token);
    }

    @Test
    public void createTokenWithMissingName(VertxTestContext testContext) {
      createTokenWithMissingData(testContext, "name");
    }

    @Test
    public void createTokenWithMissingSymbol(VertxTestContext testContext) {
      createTokenWithMissingData(testContext, "symbol");
    }

    private void createTokenWithLongString(VertxTestContext testContext, String attributeToUpdate) {
      token.put(attributeToUpdate, VERY_LONG_STRING);
      failingAdminAPITest(testContext, url, token);
    }

    @Test
    public void createTokenWithLongName(VertxTestContext testContext) {
      createTokenWithLongString(testContext, "name");
    }

    @Test
    public void createTokenWithLongSymbol(VertxTestContext testContext) {
      createTokenWithLongString(testContext, "symbol");
    }

    @Test
    public void createTokenWithLongMemo(VertxTestContext testContext) {
      createTokenWithLongString(testContext, "memo");
    }

    @Test
    public void createTokenWithInvalidSupply(VertxTestContext testContext) {
      token.put("initialSupply", "abcdef");
      failingAdminAPITest(testContext, url, token);
    }

    @Test
    public void createTokenWithNegativeSupply(VertxTestContext testContext) {
      token.put("initialSupply", -1);
      failingAdminAPITest(testContext, url, token);
    }

    @Test
    public void createTokenWithInvalidDecimals(VertxTestContext testContext) {
      token.put("decimals", "abcdef");
      failingAdminAPITest(testContext, url, token);
    }

    @Test
    public void createTokenWithNegativeDecimals(VertxTestContext testContext) {
      token.put("decimals", -1);
      failingAdminAPITest(testContext, url, token);
    }

  @Test
  public void createTokenWithDescriptionNoType(VertxTestContext testContext) {
    JsonObject description = new JsonObject();
    token.put("description", description);

    failingAdminAPITest(testContext, url, token);
  }

  @Test
  public void createTokenWithDescriptionDescriptionNoType(VertxTestContext testContext) {
    JsonObject description = new JsonObject();
    description.put("description","string");
    token.put("description", description);

    failingAdminAPITest(testContext, url, token);
  }

  @Test
  public void createTokenWithDescriptionLongType(VertxTestContext testContext) {
    JsonObject description = new JsonObject();
    description.put("type", VERY_LONG_STRING);
    description.put("description","string");
    token.put("description", description);

    failingAdminAPITest(testContext, url, token);
  }

  @Test
  public void createTokenWithDescriptionLongDescription(VertxTestContext testContext) {
    JsonObject description = new JsonObject();
    description.put("type", "string");
    description.put("description", VERY_LONG_STRING);
    token.put("description", description);

    failingAdminAPITest(testContext, url, token);
  }

  @Test
  public void createTokenWithDescriptionInvalidType(VertxTestContext testContext) {
    JsonObject description = new JsonObject();
    description.put("type", "invalid");
    description.put("description", "a description");
    token.put("description", description);

    failingAdminAPITest(testContext, url, token);
  }

  @Test
  public void createTokenWithImageNoType(VertxTestContext testContext) {
    JsonObject image = new JsonObject();
    token.put("image", image);

    failingAdminAPITest(testContext, url, token);
  }

  @Test
  public void createTokenWithImageDescriptionNoType(VertxTestContext testContext) {
    JsonObject image = new JsonObject();
    image.put("description","string");
    token.put("image", image);

    failingAdminAPITest(testContext, url, token);
  }

  @Test
  public void createTokenWithImageLongType(VertxTestContext testContext) {
    JsonObject image = new JsonObject();
    image.put("type", VERY_LONG_STRING);
    image.put("description","string");
    token.put("image", image);

    failingAdminAPITest(testContext, url, token);
  }

  @Test
  public void createTokenWithImageLongDescription(VertxTestContext testContext) {
    JsonObject image = new JsonObject();
    image.put("type", "string");
    image.put("description", VERY_LONG_STRING);
    token.put("image", image);

    failingAdminAPITest(testContext, url, token);
  }

  @Test
  public void createTokenWithImageInvalidType(VertxTestContext testContext) {
    JsonObject image = new JsonObject();
    image.put("type", "invalid");
    image.put("description", "a description");
    token.put("image", image);

    failingAdminAPITest(testContext, url, token);
  }

  @Test
  public void createTokenWithImageInvalidBase64(VertxTestContext testContext) {
    JsonObject image = new JsonObject();
    image.put("type", "base64");
    image.put("description", "not base64");
    token.put("image", image);

    failingAdminAPITest(testContext, url, token);
  }

  @Test
  public void createTokenWithImageInvalidFile(VertxTestContext testContext) {
    JsonObject image = new JsonObject();
    image.put("type", "file");
    image.put("description", "thisfiledoesnotexist.txt");
    token.put("image", image);

    failingAdminAPITest(testContext, url, token);
  }

  @Test
  public void createTokenWithImageFileContainingPath(VertxTestContext testContext) {
    JsonObject image = new JsonObject();
    image.put("type", "file");
    image.put("description", "../folder/thisfiledoesnotexist.txt");
    token.put("image", image);

    failingAdminAPITest(testContext, url, token);
  }

  @Test
  public void createTokenWithImageInvalidURL(VertxTestContext testContext) {
    JsonObject image = new JsonObject();
    image.put("type", "string");
    image.put("description", "thisisnotavalidurl");
    token.put("image", image);

    failingAdminAPITest(testContext, url, token);
  }

  @Test
  public void createTokenWithCertificateNoType(VertxTestContext testContext) {
    JsonObject certificate = new JsonObject();
    token.put("certificate", certificate);

    failingAdminAPITest(testContext, url, token);
  }

  @Test
  public void createTokenWithCertificateDescriptionNoType(VertxTestContext testContext) {
    JsonObject certificate = new JsonObject();
    certificate.put("description","string");
    token.put("certificate", certificate);

    failingAdminAPITest(testContext, url, token);
  }

  @Test
  public void createTokenWithCertificateLongType(VertxTestContext testContext) {
    JsonObject certificate = new JsonObject();
    certificate.put("type", VERY_LONG_STRING);
    certificate.put("description","string");
    token.put("certificate", certificate);

    failingAdminAPITest(testContext, url, token);
  }

  @Test
  public void createTokenWithCertificateLongDescription(VertxTestContext testContext) {
    JsonObject certificate = new JsonObject();
    certificate.put("type", "string");
    certificate.put("description", VERY_LONG_STRING);
    token.put("certificate", certificate);

    failingAdminAPITest(testContext, url, token);
  }

  @Test
  public void createTokenWithCertificateInvalidType(VertxTestContext testContext) {
    JsonObject certificate = new JsonObject();
    certificate.put("type", "invalid");
    certificate.put("description", "a description");
    token.put("certificate", certificate);

    failingAdminAPITest(testContext, url, token);
  }

  @Test
  public void createTokenWithCertificateInvalidBase64(VertxTestContext testContext) {
    JsonObject certificate = new JsonObject();
    certificate.put("type", "base64");
    certificate.put("description", "not base64");
    token.put("certificate", certificate);

    failingAdminAPITest(testContext, url, token);
  }

  @Test
  public void createTokenWithCertificateInvalidFile(VertxTestContext testContext) {
    JsonObject certificate = new JsonObject();
    certificate.put("type", "file");
    certificate.put("description", "thisfiledoesnotexist.txt");
    token.put("certificate", certificate);

    failingAdminAPITest(testContext, url, token);
  }

  @Test
  public void createTokenWithCertificateFileContainingPath(VertxTestContext testContext) {
    JsonObject image = new JsonObject();
    image.put("type", "file");
    image.put("description", "../folder/thisfiledoesnotexist.txt");
    token.put("certificate", image);

    failingAdminAPITest(testContext, url, token);
  }

  @Test
  public void createTokenWithCertificateInvalidURL(VertxTestContext testContext) {
    JsonObject certificate = new JsonObject();
    certificate.put("type", "string");
    certificate.put("description", "thisisnotavalidurl");
    token.put("certificate", certificate);

    failingAdminAPITest(testContext, url, token);
  }

    private void basicToken() {
      token = new JsonObject();
      token.put("name", "name");
      token.put("symbol", "symbol");
      token.put("initialSupply", 1);
      token.put("decimals", 0);
      token.put("memo", "memo");
    }
}
