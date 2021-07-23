package com.hedera.demo.auction.app;

import com.hedera.hashgraph.sdk.PrivateKey;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class GenerateKey {

    public GenerateKey() {
        super();
    }

    /**
     * Generates a private key
     */
    public PrivateKey generate() {
        return PrivateKey.generate();
    }

    /**
     * Generates a private key
     * and outputs the private and public key to log
     */
    public static void main(String[] args) {
        GenerateKey generateKey = new GenerateKey();
        PrivateKey privateKey = generateKey.generate();
        System.out.println("Private Key: ".concat(privateKey.toString());
        System.out.println("Public Key: ".concat(privateKey.getPublicKey().toString());
    }
}
