package com.hedera.demo.auction.app.mirrormapping;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hedera.demo.auction.app.Utils;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown=true)
public class MirrorTransaction {

    @JsonProperty("consensus_timestamp")
    public String consensusTimestamp = "";

    @JsonProperty("memo_base64")
    public String memo = "";

    public String getMemoString() {
        byte[] transactionMemoBytes = Base64.getDecoder().decode(memo);
        return new String(transactionMemoBytes, StandardCharsets.UTF_8);
    }

    @JsonProperty("name")
    public String name = "";

    @JsonProperty("result")
    public String result = "";

    @JsonProperty("transaction_hash")
    public String transactionHash = "";

    public String getTransactionHashString() {
        return Utils.base64toString(transactionHash);
    }

    @JsonProperty("transaction_id")
    public String transactionId = "";

    @JsonProperty("transfers")
    public List<MirrorHbarTransfer> hbarTransfers = new ArrayList<>();

    @JsonProperty("token_transfers")
    public List<MirrorTokenTransfer> tokenTransfers = new ArrayList<>();

    public String payer() {
        //TODO: Kabuto and Dragonglass
        String id = this.transactionId;
        return id.substring(0, id.indexOf("-"));
    }

    public boolean isSuccessful() {
        return this.result.equals("SUCCESS");
    }
}
