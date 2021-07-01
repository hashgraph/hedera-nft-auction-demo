package com.hedera.demo.auction.test.unit.domain;

import com.hedera.demo.auction.app.domain.Validator;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ValidatorTest extends AbstractValidator {
    @Test
    public void testValidatorGetterSetter() {

        Validator validator = testValidatorObject();
        verifyValidatorContent(validator);
    }

    @Test
    public void testValidatorToJson() {
        Validator validator = testValidatorObject();

        JsonObject validatorJson = validator.toJson();

        assertEquals(name, validatorJson.getString("name"));
        assertEquals(url, validatorJson.getString("url"));
        assertEquals(publicKey, validatorJson.getString("publicKey"));
    }

    @Test
    public void testValidatorFromJson() {
        JsonObject validatorJson = testValidatorObject().toJson();
        Validator validator = new Validator(validatorJson);
        verifyValidatorContent(validator);
    }

    @Test
    public void testValidatorToString() {
        Validator validator = testValidatorObject();

        String validatorString = validator.toString();
        assertTrue(validatorString.contains(validator.getName()));
        assertTrue(validatorString.contains(validator.getUrl()));
        assertTrue(validatorString.contains(validator.getPublicKey()));
    }
}
