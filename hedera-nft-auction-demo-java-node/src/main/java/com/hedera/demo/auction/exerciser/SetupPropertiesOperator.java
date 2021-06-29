package com.hedera.demo.auction.exerciser;

import com.hedera.hashgraph.sdk.AccountId;
import com.hedera.hashgraph.sdk.PrivateKey;

public class SetupPropertiesOperator {
  private String accountId = "";
  private String privateKey = "";

  public void setAccountId(String accountId) {
    this.accountId = accountId;
  }

  public String getAccountId() {
    return accountId;
  }

  public void setPrivateKey(String privateKey) {
    this.privateKey = privateKey;
  }

  public String getPrivateKey() {
    return privateKey;
  }

  public AccountId accountId() {
    return AccountId.fromString(accountId);
  }

  public PrivateKey privateKey() {
    return PrivateKey.fromString(privateKey);
  }
}
