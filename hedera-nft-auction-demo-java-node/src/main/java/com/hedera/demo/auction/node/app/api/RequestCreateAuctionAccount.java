package com.hedera.demo.auction.node.app.api;

import io.vertx.core.json.JsonArray;

@SuppressWarnings("unused")
public class RequestCreateAuctionAccount {
    public JsonArray keylist = new JsonArray();
    public long initialBalance = 0;
}
