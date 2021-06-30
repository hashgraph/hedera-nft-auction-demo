package com.hedera.demo.auction.app.mirrormapping;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Data class to map JSON to a java object
 */
@JsonIgnoreProperties(ignoreUnknown=true)
public class MirrorScheduleSignature {

  @JsonProperty("consensus_timestamp")
  public String consensusTimestamp = "";

  @JsonProperty("public_key_prefix")
  public String publicKeyPrefix = "";

  public String getPublicKeyPrefix() {
    byte[] keyBytes = Base64.getDecoder().decode(publicKeyPrefix);
    return new String(keyBytes, StandardCharsets.UTF_8);
  }

}
