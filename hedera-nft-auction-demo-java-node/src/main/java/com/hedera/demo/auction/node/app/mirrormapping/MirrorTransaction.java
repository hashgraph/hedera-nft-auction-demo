package com.hedera.demo.auction.node.app.mirrormapping;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.codec.binary.Hex;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown=true)
public class MirrorTransaction {

    private String memo = "";
    private String transactionHash = "";

    @JsonProperty("consensus_timestamp")
    public String consensusTimestamp = "";

    @JsonProperty("memo_base64")
    public void setMemo(String memoBase64) {
        byte[] transactionMemoBytes = Base64.getDecoder().decode(memoBase64);
        this.memo = new String(transactionMemoBytes, StandardCharsets.UTF_8);
    }
    @JsonProperty("memo_base64")
    public String getMemo() {
        return this.memo;
    }

    @JsonProperty("result")
    public String result = "";

    @JsonProperty("scheduled")
    public boolean scheduled = false;

    @JsonProperty("transaction_hash")
    public void setTransactionHash(String transactionHash) {
        byte[] txHashBytes = Base64.getDecoder().decode(transactionHash);
        this.transactionHash = Hex.encodeHexString(txHashBytes);
    }
    @JsonProperty("transaction_hash")
    public String getTransactionHash() {
        return this.transactionHash;
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
