package com.hedera.demo.auction.exerciser;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SetupProperties {
  private boolean createTopic = true;
  private SetupPropertiesOperator setupOperator = new SetupPropertiesOperator();
  private SetupPropertiesToken token = new SetupPropertiesToken();
  private SetupPropertiesAuctionAccount auctionAccount = new SetupPropertiesAuctionAccount();
  private SetupPropertiesAuction auction = new SetupPropertiesAuction();
  private String adminApiHost = "https://localhost:8082";
  private String xApiKey = "";

  private SetupExerciser exerciser = new SetupExerciser();

}
