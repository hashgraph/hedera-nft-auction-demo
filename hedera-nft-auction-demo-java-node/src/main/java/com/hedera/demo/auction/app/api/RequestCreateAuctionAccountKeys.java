package com.hedera.demo.auction.app.api;

import com.google.errorprone.annotations.Var;
import com.hedera.hashgraph.sdk.Key;
import com.hedera.hashgraph.sdk.KeyList;
import com.hedera.hashgraph.sdk.PublicKey;

import java.util.ArrayList;
import java.util.List;

/**
 * Data class to map incoming REST JSON to a java object
 */
@SuppressWarnings("unused")
public class RequestCreateAuctionAccountKeys {
    public List<RequestCreateAuctionAccountKey> keys = new ArrayList<>();
    public int threshold = 0;
    public Key toKeyList() {
        @Var KeyList keyList = new KeyList();
        if (threshold != 0) {
            keyList = KeyList.withThreshold(threshold);
        }

        for (RequestCreateAuctionAccountKey auctionKey : keys) {
            PublicKey key = PublicKey.fromString(auctionKey.key);
            keyList.add(key);
        }

        return keyList;
    }
    public boolean containsKey(String key) {
        String publicPrefix = "302a300506032b6570032100";
        for (RequestCreateAuctionAccountKey auctionKey : keys) {
            String key1 = auctionKey.key.replace(publicPrefix, "");
            String key2 = key.replace(publicPrefix, "");
            if (key2.equals(key1)) {
                return true;
            }
        }
        return false;
    }
}
