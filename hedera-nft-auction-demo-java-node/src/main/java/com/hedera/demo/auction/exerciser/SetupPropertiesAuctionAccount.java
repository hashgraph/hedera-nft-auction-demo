package com.hedera.demo.auction.exerciser;
import java.util.ArrayList;
import java.util.List;

public class SetupPropertiesAuctionAccount {
  private List<String> publicKeys = new ArrayList<>();
  private int threshold = 1;
  private long balance = 10;

  public void setPublicKeys(List<String> publicKeys) {
    this.publicKeys = publicKeys;
  }

  public List<String> getPublicKeys() {
    return publicKeys;
  }

  public void setThreshold(int treshold) {
    this.threshold = treshold;
  }

  public int getThreshold() {
    return threshold;
  }

  public void setBalance(long balance) {
    this.balance = balance;
  }

  public long getBalance() {
    return balance;
  }
}
