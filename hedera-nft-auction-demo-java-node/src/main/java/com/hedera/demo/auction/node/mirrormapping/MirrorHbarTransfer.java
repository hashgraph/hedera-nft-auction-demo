package com.hedera.demo.auction.node.mirrormapping;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown=true)
public class MirrorHbarTransfer {
    private String account = "";
    private long amount = 0L;

    @JsonProperty("account")
    public void setAccount(String account) {
        this.account = account;
    }

    @JsonProperty("account")
    public String getAccount() {
        return this.account;
    }

    @JsonProperty("amount")
    public void setAmount(long amount) {
        this.amount = amount;
    }

    @JsonProperty("amount")
    public long getAmount() {
        return this.amount;
    }
}
