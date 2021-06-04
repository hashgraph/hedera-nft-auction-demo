package com.hedera.demo.auction.app.mirrormapping;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown=true)
public class MirrorHbarTransfer {

    @JsonProperty("account")
    public String account = "";

    @JsonProperty("amount")
    public long amount = 0L;
}
