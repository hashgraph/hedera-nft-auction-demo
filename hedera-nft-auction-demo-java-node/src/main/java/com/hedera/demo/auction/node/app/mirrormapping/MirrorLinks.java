package com.hedera.demo.auction.node.app.mirrormapping;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown=true)
public class MirrorLinks {
    @JsonProperty("next")
    public String next = "";
}
