package com.hedera.demo.auction.app;

import java.util.UUID;

public class GenerateApiKey {

    public GenerateApiKey() {
    }

    public String generate() {
        return UUID.randomUUID().toString();
    }

    /**
     * Generates an api key
     * and outputs the key to log
     */
    public static void main(String[] args) {
        GenerateApiKey generateApiKey = new GenerateApiKey();
        String apiKey = generateApiKey.generate();
        System.out.println("X_API_KEY=".concat(apiKey));
    }
}
