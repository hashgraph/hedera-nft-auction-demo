package com.hedera.demo.auction.test.unit.domain;

import com.hedera.demo.auction.app.domain.Validator;

import static org.junit.jupiter.api.Assertions.assertEquals;

public abstract class AbstractValidator {
    String name = "name";
    String url = "url";
    String publicKey = "publicKey";

    Validator testValidatorObject() {
        Validator validator = new Validator();

        validator.setName(name);
        validator.setUrl(url);
        validator.setPublicKey(publicKey);

        return validator;
    }

    public void verifyValidatorContent(Validator validator) {
        assertEquals(name, validator.getName());
        assertEquals(url, validator.getUrl());
        assertEquals(publicKey, validator.getPublicKey());
    }
}
