package com.hedera.demo.auction.app.mirrormapping;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Data class to map JSON to a java object
 */
@JsonIgnoreProperties(ignoreUnknown=true)
public class MirrorSchedule {

  @JsonProperty("consensus_timestamp")
  public String consensusTimestamp = "";

  @JsonProperty("executed_timestamp")
  public String executedTimestamp = "";

  @JsonProperty("signatures")
  public MirrorScheduleSignature[] mirrorScheduleSignatures = new MirrorScheduleSignature[0];

  public int getSignatureCount() {
    return (mirrorScheduleSignatures == null) ? 0 : mirrorScheduleSignatures.length;
  }
}
