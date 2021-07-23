package com.hedera.demo.auction.test.integration.database;

import com.google.errorprone.annotations.Var;
import com.hedera.demo.auction.app.SqlConnectionManager;
import com.hedera.demo.auction.app.domain.Validator;
import com.hedera.demo.auction.app.repository.ValidatorsRepository;
import com.hedera.demo.auction.test.integration.AbstractIntegrationTest;
import com.hedera.hashgraph.sdk.PrivateKey;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ValidatorDatabaseIntegrationTest extends AbstractIntegrationTest {

    public ValidatorDatabaseIntegrationTest() {
    }

    private PostgreSQLContainer postgres;
    private ValidatorsRepository validatorsRepository;
    private Validator validator;

    @BeforeAll
    public void beforeAll() {
        PostgreSQLContainer<?> postgres = new PostgreSQLContainer("postgres:12.6");
        postgres.start();
        migrate(postgres);
        SqlConnectionManager connectionManager = new SqlConnectionManager(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
        validatorsRepository = new ValidatorsRepository(connectionManager);
        this.postgres = postgres;
    }
    @AfterAll
    public void afterAll() {
        this.postgres.close();
    }

    @BeforeEach
    public void beforeEach() throws Exception {
        validatorsRepository.deleteAllValidators();
        validator = testValidatorObject(1);
        JsonObject validatorJson = new JsonObject();
        validatorJson.put("name", validator.getName());
        validatorJson.put("url", validator.getUrl());
        validatorJson.put("publicKey", validator.getPublicKey());
        validatorJson.put("operation", "add");
        JsonArray validators = new JsonArray();
        validators.add(validatorJson);
        validatorsRepository.manage(validators);
    }

    @AfterEach
    public void afterEach() throws SQLException {
        validatorsRepository.deleteAllValidators();
    }

    @Test
    public void addValidatorTest() throws Exception {

        List<Validator> validators = validatorsRepository.getValidatorsList();
        assertEquals(1, validators.size());
        testNewValidator(validator, validators.get(0));
    }

    @Test
    public void udpateValidatorTest() throws Exception {
        JsonObject validatorJson = new JsonObject();
        validatorJson.put("nameToUpdate", validator.getName());
        validatorJson.put("name", "newName");
        validatorJson.put("url", "https://hedera2.com");
        String newPubKey = PrivateKey.generate().getPublicKey().toString();
        validatorJson.put("publicKey", newPubKey);
        validatorJson.put("operation", "update");
        JsonArray validatorsJson = new JsonArray();
        validatorsJson.add(validatorJson);
        validatorsRepository.manage(validatorsJson);

        List<Validator> validators = validatorsRepository.getValidatorsList();
        assertEquals(1, validators.size());
        assertEquals("newName", validators.get(0).getName());
        assertEquals("https://hedera2.com", validators.get(0).getUrl());
        assertEquals(newPubKey, validators.get(0).getPublicKey());
    }

    @Test
    public void multipleValidatorsTest() throws Exception {
        @Var JsonObject validatorJson = new JsonObject();
        validatorJson.put("name", "name1");
        validatorJson.put("url", "https://hedera.com");
        validatorJson.put("publicKey", validator.getPublicKey());
        validatorJson.put("operation", "add");
        JsonArray validatorsJson = new JsonArray();
        validatorsJson.add(validatorJson);

        validatorJson = new JsonObject();
        validatorJson.put("name", "name2");
        validatorJson.put("url", "https://hedera.com");
        validatorJson.put("publicKey", validator.getPublicKey());
        validatorJson.put("operation", "add");
        validatorsJson.add(validatorJson);

        validatorsRepository.manage(validatorsJson);

        List<Validator> validators = validatorsRepository.getValidatorsList();
        assertEquals(3, validators.size());
    }

    @Test
    public void deleteValidatorTest() throws Exception {
        JsonObject validatorJson = new JsonObject();
        JsonArray validatorsJson = new JsonArray();
        validatorJson.put("name", validator.getName());
        validatorJson.put("operation", "delete");
        validatorsJson.clear();
        validatorsJson.add(validatorJson);

        validatorsRepository.manage(validatorsJson);

        List<Validator> deletedValidators = validatorsRepository.getValidatorsList();
        assertEquals(0, deletedValidators.size());
    }

    @Test
    public void deleteValidatorsTest() throws Exception {
        JsonObject validatorJson = new JsonObject();
        JsonArray validatorsJson = new JsonArray();

        validatorJson.put("name", "name2");
        validatorJson.put("url", "https://hedera.com");
        validatorJson.put("publicKey", validator.getPublicKey());
        validatorJson.put("operation", "add");
        validatorsJson.add(validatorJson);

        validatorsRepository.manage(validatorsJson);

        List<Validator> validators = validatorsRepository.getValidatorsList();
        assertEquals(2, validators.size());

        validatorsRepository.deleteAllValidators();
        List<Validator> deletedValidators = validatorsRepository.getValidatorsList();
        assertEquals(0, deletedValidators.size());
    }

    @Test
    public void duplicateValidatorTest() throws SQLException {
        JsonObject validatorJson = new JsonObject();
        validatorJson.put("name", validator.getName());
        validatorJson.put("url", validator.getUrl());
        validatorJson.put("publicKey", validator.getPublicKey());
        validatorJson.put("operation", "add");
        JsonArray validatorsJson = new JsonArray();
        validatorsJson.add(validatorJson);

        try {
            validatorsRepository.manage(validatorsJson);
        } catch (Exception e) {
            // do nothing, it's expected
        }

        List<Validator> validators = validatorsRepository.getValidatorsList();
        assertEquals(1, validators.size());
    }
}
