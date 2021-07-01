package com.hedera.demo.auction.app.mirrormapping;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hedera.demo.auction.app.Utils;

import java.util.ArrayList;
import java.util.List;

/**
 * Data class to map JSON to a java object
 */
@JsonIgnoreProperties(ignoreUnknown=true)
public class MirrorTransaction {

    @JsonProperty("consensus_timestamp")
    public String consensusTimestamp = "";

    @JsonProperty("memo_base64")
    public String memo = "";

    /**
     * Gets the memo as a string from base64
     * @return String representation of a base64 memo
     */
    public String getMemoString() {
        return Utils.base64toString(memo);
    }

    @JsonProperty("name")
    public String name = "";

    @JsonProperty("result")
    public String result = "";

    @JsonProperty("transaction_hash")
    public String transactionHash = "";

    /**
     * Gets the transaction hash as a string from base64
     * @return String representation of a base64 transaction hash
     */
    public String getTransactionHashString() {
        return Utils.base64toStringHex(transactionHash);
    }

    @JsonProperty("transaction_id")
    public String transactionId = "";

    @JsonProperty("transfers")
    public List<MirrorHbarTransfer> hbarTransfers = new ArrayList<>();

    @JsonProperty("token_transfers")
    public List<MirrorTokenTransfer> tokenTransfers = new ArrayList<>();

    /**
     * Extracts the payer account id from the transaction id
     * @return String payer account id
     */
    public String payer() {
        String id = this.transactionId;
        return id.substring(0, id.indexOf("-"));
    }

    public boolean isSuccessful() {
        return this.result.equals("SUCCESS");
    }
}
