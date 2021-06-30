package com.hedera.demo.auction.app;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hedera.hashgraph.sdk.Key;
import com.hedera.hashgraph.sdk.PublicKey;
import org.jooq.tools.StringUtils;

/**
 * Data class to map JSON to java Key objects
 */
@JsonIgnoreProperties(ignoreUnknown=true)
public class AuctionKey {
    @JsonProperty("key")
    public String key = "";
    @JsonProperty("keyList")
    public AuctionKeyList auctionKeyList = new AuctionKeyList();

    public Key toKeyList() {
        if (StringUtils.isEmpty(key)) {
            return auctionKeyList.toKeyList();
        } else {
            return PublicKey.fromString(key);
        }
    }

    public boolean isValid() {
        return auctionKeyList != null || !StringUtils.isEmpty(key);
    }
}

