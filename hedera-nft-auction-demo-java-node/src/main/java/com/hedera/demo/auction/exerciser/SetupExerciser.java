package com.hedera.demo.auction.exerciser;

public class SetupExerciser {
  private String auctionAccount = "";
  private int numAccounts = 10;
  private int numThreads = 4;
  private int numTransfers = 4;

  public void setAuctionAccount(String auctionAccount) {
    this.auctionAccount = auctionAccount;
  }

  public String getAuctionAccount() {
    return auctionAccount;
  }

  public void setNumAccounts(int numAccounts) {
    this.numAccounts = numAccounts;
  }

  public int getNumAccounts() {
    return numAccounts;
  }

  public void setNumThreads(int numThreads) {
    this.numThreads = numThreads;
  }

  public int getNumThreads() {
    return numThreads;
  }

  public void setNumTransfers(int numTransfers) {
    this.numTransfers = numTransfers;
  }

  public int getNumTransfers() {
    return numTransfers;
  }
}
