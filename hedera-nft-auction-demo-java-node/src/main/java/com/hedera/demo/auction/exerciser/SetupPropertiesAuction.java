package com.hedera.demo.auction.exerciser;

public class SetupPropertiesAuction {
  private long reserve = 0;
  private long minimumbid = 10;
  private String endtimestamp = "2d";
  private boolean winnercanbid = true;
  private String title = "auction title";
  private String description = "auction description";

  public void setReserve(long reserve) {
    this.reserve = reserve;
  }

  public long getReserve() {
    return reserve;
  }

  public void setMinimumbid(long minimumbid) {
    this.minimumbid = minimumbid;
  }

  public long getMinimumbid() {
    return minimumbid;
  }

  public void setEndtimestamp(String endtimestamp) {
    this.endtimestamp = endtimestamp;
  }

  public String getEndtimestamp() {
    return endtimestamp;
  }

  public void setWinnercanbid(boolean winnercanbid) {
    this.winnercanbid = winnercanbid;
  }

  public boolean isWinnercanbid() {
    return winnercanbid;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getTitle() {
    return title;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getDescription() {
    return description;
  }
}
