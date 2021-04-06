package com.hedera.demo.auction.node.mirrormapping;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown=true)
public class MirrorTransactions {
    @JsonProperty("transactions")
    public List<MirrorTransaction> transactions;
    @JsonProperty("links")
    public MirrorLinks links;
}
