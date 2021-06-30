package com.hedera.demo.auction.app.mirrormapping;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hedera.demo.auction.app.Utils;
import org.jooq.tools.StringUtils;

/**
 * Data class to map JSON to a java object
 */
@JsonIgnoreProperties(ignoreUnknown=true)
public class MirrorTopicMessage {
    @JsonProperty("consensus_timestamp")
    public String consensusTimestamp = "";

    @JsonProperty("message")
    public String messageBase64 = "";

    /**
     * Gets the base64 message as a String
     * @return String containing the decoded base64 message
     */
    public String message() {
        if (!StringUtils.isEmpty(messageBase64)) {
            return Utils.base64toString(messageBase64);
        } else {
            return "";
        }
    }
}


