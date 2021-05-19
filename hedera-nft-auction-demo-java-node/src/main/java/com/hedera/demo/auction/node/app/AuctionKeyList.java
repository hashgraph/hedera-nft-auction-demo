package com.hedera.demo.auction.node.app;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.errorprone.annotations.Var;
import com.hedera.hashgraph.sdk.KeyList;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown=true)
public class AuctionKeyList {
    @JsonProperty("keys")
    public List<AuctionKey> auctionKeys = new ArrayList<>();
    @JsonProperty("threshold")
    public int threshold = 0;

    public KeyList toKeyList() {
        @Var KeyList keyList = new KeyList();
        if (threshold != 0) {
            keyList = KeyList.withThreshold(threshold);
        }

        for (AuctionKey auctionKey : auctionKeys) {
            keyList.add(auctionKey.toKeyList());
        }

        return keyList;
    }

}

