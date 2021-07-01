package com.hedera.demo.auction.app.mirrormapping;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

/**
 * Data class to map JSON to a java object
 */
@JsonIgnoreProperties(ignoreUnknown=true)
public class MirrorTransactions {
    @JsonProperty("transactions")
    public List<MirrorTransaction> transactions = new ArrayList<>();
    @JsonProperty("links")
    public MirrorLinks links = new MirrorLinks();
}
