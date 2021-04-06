package com.hedera.demo.auction.node.mirrormapping;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown=true)
public class MirrorTokenTransfer {
    @JsonProperty("account")
    public String account;
    @JsonProperty("token_id")
    public String tokenId;
    @JsonProperty("amount")
    public long amount;
}
