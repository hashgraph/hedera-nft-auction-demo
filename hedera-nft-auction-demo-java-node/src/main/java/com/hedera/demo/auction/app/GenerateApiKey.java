package com.hedera.demo.auction.app;

import java.util.UUID;

public class GenerateApiKey {

    private GenerateApiKey() {

    }
    /**
     * Generates an api key
     * and outputs the key to log
     */
    public static void main(String[] args) {
        UUID uuid = UUID.randomUUID();
        System.out.println("X_API_KEY=".concat(uuid.toString()));
    }
}
