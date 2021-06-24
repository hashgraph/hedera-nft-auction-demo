package com.hedera.demo.auction.app.mirrormapping;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jooq.tools.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@JsonIgnoreProperties(ignoreUnknown=true)
public class MirrorTopicMessage {
    @JsonProperty("consensus_timestamp")
    public String consensusTimestamp = "";

    @JsonProperty("message")
    public String messageBase64 = "";

    public String message() {
        if (!StringUtils.isEmpty(messageBase64)) {
            byte[] messageBytes = Base64.getDecoder().decode(messageBase64);
            return new String(messageBytes, StandardCharsets.UTF_8);
        } else {
            return "";
        }
    }
}


