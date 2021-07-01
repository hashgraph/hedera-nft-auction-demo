package com.hedera.demo.auction.app.mirrormapping;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Data class to map JSON to a java object
 */
@JsonIgnoreProperties(ignoreUnknown=true)
public class MirrorLinks {
    @JsonProperty("next")
    public String next = "";
}
