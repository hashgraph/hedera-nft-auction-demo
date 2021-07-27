package com.hedera.demo.auction.test.unit.app;

import com.hedera.demo.auction.app.GenerateApiKey;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class GenerateApiKeyTest {

    @Test
    public void testGenerateApiKey() {

        GenerateApiKey generateApiKey = new GenerateApiKey();
        String apiKey = generateApiKey.generate();

        assertNotNull(apiKey);
    }

    @Test
    public void testGenerateApiKeyMain() {
        GenerateApiKey.main(new String[0]);
    }
}
