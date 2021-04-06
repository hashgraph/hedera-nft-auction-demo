package com.hedera.demo.auction.node.mirrormapping;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.codec.binary.Hex;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown=true)
public class MirrorTransaction {

    private String consensusTimestamp = "";
    private String memo = "";
    private String result = "";
    private boolean successful = false;
    private boolean scheduled = false;
    private String transactionHash = "";
    private String transactionId = "";

    @JsonProperty("consensus_timestamp")
    public void setConsensusTimestamp(String consensusTimestamp) {
        this.consensusTimestamp = consensusTimestamp;
    }
    @JsonProperty("consensus_timestamp")
    public String getConsensusTimestamp() {
        return this.consensusTimestamp;
    }

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
    public void setResult(String result) {
        this.result = result;
        this.successful = this.result.equals("SUCCESS");
    }
    @JsonProperty("result")
    public String getResult() {
        return this.result;
    }

    @JsonProperty("scheduled")
    public void setScheduled(boolean scheduled) {
        this.scheduled = scheduled;
    }
    @JsonProperty("scheduled")
    public boolean getScheduled() {
        return this.scheduled;
    }

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
    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }
    @JsonProperty("transaction_id")
    public String getTransactionId() {
        return this.transactionId;
    }

    @JsonProperty("transfers")
    public List<MirrorHbarTransfer> hbarTransfers;

    @JsonProperty("token_transfers")
    public List<MirrorTokenTransfer> tokenTransfers;

    public String payer() {
        //TODO: Kabuto and Dragonglass
        String id = this.transactionId;
        return id.substring(0, id.indexOf("-"));
    }

    public boolean isSuccessful() {
        return this.successful;
    }
}
