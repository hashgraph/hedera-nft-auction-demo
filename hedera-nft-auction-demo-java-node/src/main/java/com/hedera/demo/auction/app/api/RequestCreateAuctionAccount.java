package com.hedera.demo.auction.app.api;

/**
 * Data class to map incoming REST JSON to a java object
 */
@SuppressWarnings("unused")
public class RequestCreateAuctionAccount {
    public RequestCreateAuctionAccountKeys keylist = new RequestCreateAuctionAccountKeys();
    public long initialBalance = 0;
}
