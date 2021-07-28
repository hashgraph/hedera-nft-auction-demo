package com.hedera.demo.auction.app;

import com.hedera.hashgraph.sdk.PrivateKey;

public class GenerateKey {

    public GenerateKey() {
        super();
    }

    /**
     * Generates a private key
     */
    public PrivateKey generate() {

        PrivateKey privateKey = PrivateKey.generate();
        System.out.println("Private Key: ".concat(privateKey.toString()));
        System.out.println("Public Key: ".concat(privateKey.getPublicKey().toString()));

        return privateKey;
    }

    /**
     * Generates a private key
     * and outputs the private and public key to log
     */
    public static void main(String[] args) {
        GenerateKey generateKey = new GenerateKey();
        generateKey.generate();
    }
}
