package com.hedera.demo.auction.app.api;

/**
 * Data class to map incoming REST JSON to a java object
 */
@SuppressWarnings("unused")
public class RequestCreateAuction {
    public String auctionFile = "";
    public String tokenid = "";
    public String auctionaccountid = "";
    public long reserve = 0L;
    public long minimumbid = 0L;
    public String endtimestamp = "";
    public boolean winnercanbid = false;
    public String topicId = "";
    public String description = "";
    public String title = "";
}
