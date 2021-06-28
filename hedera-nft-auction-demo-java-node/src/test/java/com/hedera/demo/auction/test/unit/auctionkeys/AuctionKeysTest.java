package com.hedera.demo.auction.test.unit.auctionkeys;

import com.hedera.demo.auction.app.AuctionKey;
import com.hedera.hashgraph.sdk.PrivateKey;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AuctionKeysTest {

    String publicKeyMaster = PrivateKey.generate().getPublicKey().toString();
    String publicKey1 = PrivateKey.generate().getPublicKey().toString();
    String publicKey2 = PrivateKey.generate().getPublicKey().toString();
    int threshold1 = 1;
    int threshold2 = 2;

    @Test
    public void testAuctionKeyEmpty() {

        JsonObject key = new JsonObject();
        AuctionKey auctionKey = key.mapTo(AuctionKey.class);

        assertEquals("", auctionKey.key);
        assertEquals(0, auctionKey.auctionKeyList.auctionKeys.size());
        assertEquals(0, auctionKey.auctionKeyList.threshold);
    }

    @Test
    public void testAuctionKeySingle() {

        JsonObject key = new JsonObject();
        key.put("key", publicKeyMaster);

        AuctionKey auctionKey = key.mapTo(AuctionKey.class);

        assertEquals(publicKeyMaster, auctionKey.key);
        assertEquals(0, auctionKey.auctionKeyList.auctionKeys.size());
        assertEquals(0, auctionKey.auctionKeyList.threshold);
    }

    @Test
    public void testAuctionKeyListSimple() {

        JsonArray keys = new JsonArray();
        JsonObject key1 = new JsonObject().put("key", publicKey1);
        JsonObject key2 = new JsonObject().put("key", publicKey2);
        keys.add(key1).add(key2);

        JsonObject keyList = new JsonObject();
        keyList.put("keys", keys);
        keyList.put("threshold", threshold1);

        JsonObject key = new JsonObject();
        key.put("keyList", keyList);

        AuctionKey auctionKey = key.mapTo(AuctionKey.class);

        assertEquals(keys.size(), auctionKey.auctionKeyList.auctionKeys.size());
        assertEquals(threshold1, auctionKey.auctionKeyList.threshold);

    }

    @Test
    public void testAuctionKeyListComplex() {

        JsonObject masterKey = new JsonObject().put("key", publicKeyMaster);

        JsonArray otherKeys = new JsonArray();
        JsonObject key1 = new JsonObject().put("key", publicKey1);
        JsonObject key2 = new JsonObject().put("key", publicKey2);
        otherKeys.add(key1).add(key2);

        JsonObject otherKeyList = new JsonObject();
        otherKeyList.put("keys", otherKeys);
        otherKeyList.put("threshold", threshold2);

        JsonArray keys = new JsonArray();
        keys.add(masterKey);
        keys.add(otherKeyList);

        JsonObject key = new JsonObject();
        JsonObject keyList = new JsonObject();
        keyList.put("keys", keys);
        keyList.put("threshold", threshold1);
        key.put("keyList", keyList);

        AuctionKey auctionKey = key.mapTo(AuctionKey.class);

        assertEquals(keys.size(), auctionKey.auctionKeyList.auctionKeys.size());
        assertEquals(threshold1, auctionKey.auctionKeyList.threshold);

    }
}
