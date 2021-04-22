package com.hedera.demo.auction.node.app.api;

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
}
