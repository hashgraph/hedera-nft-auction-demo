package com.hedera.demo.auction.test.unit.app;

import com.hedera.demo.auction.app.GenerateKey;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class GenerateKeyTest {

    @Test
    public void testGenerateKey() {
        var generateKey = new GenerateKey();
        var privateKey = generateKey.generate();

        assertNotNull(privateKey);
    }

    @Test
    public void testGenerateKeyMain() {
        GenerateKey.main(new String[0]);
    }

}
