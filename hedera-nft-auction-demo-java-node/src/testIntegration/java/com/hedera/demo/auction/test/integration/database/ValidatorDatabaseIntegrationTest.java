package com.hedera.demo.auction.test.integration.database;

import com.hedera.demo.auction.app.SqlConnectionManager;
import com.hedera.demo.auction.app.domain.Validator;
import com.hedera.demo.auction.app.repository.ValidatorsRepository;
import com.hedera.demo.auction.test.integration.AbstractIntegrationTest;
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
    public void beforeEach() throws SQLException {
        validator = testValidatorObject(1);
        validatorsRepository.add(validator.getName(), validator.getUrl(), validator.getPublicKey());
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

        validatorsRepository.update(validator.getName(), "newName", "newUrl", "newPublicKey");
        List<Validator> validators = validatorsRepository.getValidatorsList();
        assertEquals(1, validators.size());
        assertEquals("newName", validators.get(0).getName());
        assertEquals("newUrl", validators.get(0).getUrl());
        assertEquals("newPublicKey", validators.get(0).getPublicKey());
    }

    @Test
    public void multipleValidatorsTest() throws SQLException {
        validatorsRepository.add(validator.getName(), validator.getUrl(), validator.getPublicKey());
        Validator validator1 = testValidatorObject(2);
        validatorsRepository.add(validator1.getName(), validator1.getUrl(), validator1.getPublicKey());

        List<Validator> validators = validatorsRepository.getValidatorsList();
        assertEquals(2, validators.size());
    }

    @Test
    public void deleteValidatorTest() throws SQLException {
        validatorsRepository.delete(validator.getName());
        List<Validator> deletedValidators = validatorsRepository.getValidatorsList();
        assertEquals(0, deletedValidators.size());
    }

    @Test
    public void deleteValidatorsTest() throws SQLException {
        validatorsRepository.add(validator.getName(), validator.getUrl(), validator.getPublicKey());
        Validator validator1 = testValidatorObject(2);
        validatorsRepository.add(validator1.getName(), validator1.getUrl(), validator1.getPublicKey());

        List<Validator> validators = validatorsRepository.getValidatorsList();
        assertEquals(2, validators.size());

        validatorsRepository.deleteAllValidators();
        List<Validator> deletedValidators = validatorsRepository.getValidatorsList();
        assertEquals(0, deletedValidators.size());
    }

    @Test
    public void duplicateValidatorTest() throws SQLException {
        validatorsRepository.add(validator.getName(), validator.getName(), validator.getPublicKey());

        List<Validator> validators = validatorsRepository.getValidatorsList();
        assertEquals(1, validators.size());
    }
}
