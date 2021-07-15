package com.hedera.demo.auction.exerciser;

public class SetupProperties {
  private boolean createTopic = true;
  private SetupPropertiesOperator setupOperator = new SetupPropertiesOperator();
  private SetupPropertiesToken token = new SetupPropertiesToken();
  private SetupPropertiesAuctionAccount auctionAccount = new SetupPropertiesAuctionAccount();
  private SetupPropertiesAuction auction = new SetupPropertiesAuction();
  private String adminApiHost = "https://localhost:8082";
  private String xApiKey = "";

  private SetupExerciser exerciser = new SetupExerciser();

  public void setCreateTopic(boolean createTopic) {
    this.createTopic = createTopic;
  }

  public boolean isCreateTopic() {
    return createTopic;
  }

  public void setSetupOperator(SetupPropertiesOperator operator) {
    this.setupOperator = operator;
  }

  public SetupPropertiesOperator getSetupOperator() {
    return setupOperator;
  }

  public void setToken(SetupPropertiesToken token) {
    this.token = token;
  }

  public SetupPropertiesToken getToken() {
    return token;
  }

  public void setAuctionAccount(SetupPropertiesAuctionAccount auctionAccount) {
    this.auctionAccount = auctionAccount;
  }

  public SetupPropertiesAuctionAccount getAuctionAccount() {
    return auctionAccount;
  }

  public void setAuction(SetupPropertiesAuction auction) {
    this.auction = auction;
  }

  public SetupPropertiesAuction getAuction() {
    return auction;
  }

  public void setAdminApiHost(String adminApiHost) {
    this.adminApiHost = adminApiHost;
  }

  public String getAdminApiHost() {
    return adminApiHost;
  }

  public void setxApiKey(String xApiKey) {
    this.xApiKey = xApiKey;
  }

  public String getxApiKey() {
    return xApiKey;
  }

  public void setExerciser(SetupExerciser exerciser) {
    this.exerciser = exerciser;
  }

  public SetupExerciser getExerciser() {
    return exerciser;
  }
}
