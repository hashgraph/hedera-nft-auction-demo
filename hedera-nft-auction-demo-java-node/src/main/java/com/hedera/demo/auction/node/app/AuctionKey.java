package com.hedera.demo.auction.node.app;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hedera.hashgraph.sdk.KeyList;
import com.hedera.hashgraph.sdk.PublicKey;
import org.jooq.tools.StringUtils;

@JsonIgnoreProperties(ignoreUnknown=true)
public class AuctionKey {
    @JsonProperty("key")
    public String key = "";
    @JsonProperty("keyList")
    public AuctionKeyList auctionKeyList;

    public KeyList toKeyList() {
        KeyList keyList = new KeyList();
        if (StringUtils.isEmpty(key)) {
            keyList.add( auctionKeyList.toKeyList());
        } else {
            keyList.add(PublicKey.fromString(key));
        }

        return keyList;
    }
}

