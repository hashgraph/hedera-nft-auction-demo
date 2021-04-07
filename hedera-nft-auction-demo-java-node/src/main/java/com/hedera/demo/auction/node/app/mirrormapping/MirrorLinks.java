package com.hedera.demo.auction.node.app.mirrormapping;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown=true)
public class MirrorLinks {
    private String next = "";
    @JsonProperty("next")
    public void setNext(String next) {
        this.next = next;
    }
    @JsonProperty("next")
    public String getNext() {
        return this.next;
    }
}
