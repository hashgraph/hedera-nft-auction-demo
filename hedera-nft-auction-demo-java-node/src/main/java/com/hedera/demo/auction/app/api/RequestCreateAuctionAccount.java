package com.hedera.demo.auction.app.api;

import com.hedera.hashgraph.sdk.Key;

/**
 * Data class to map incoming REST JSON to a java object
 */
@SuppressWarnings("unused")
public class RequestCreateAuctionAccount {
    public RequestCreateAuctionAccountKeys keylist = new RequestCreateAuctionAccountKeys();
    public long initialBalance = 0;

    public Key toKeyList() {
        return keylist.toKeyList();
    }
}
