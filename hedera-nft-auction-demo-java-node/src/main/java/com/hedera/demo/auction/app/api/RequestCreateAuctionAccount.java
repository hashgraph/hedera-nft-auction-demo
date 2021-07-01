package com.hedera.demo.auction.app.api;

import io.vertx.core.json.JsonArray;

/**
 * Data class to map incoming REST JSON to a java object
 */
@SuppressWarnings("unused")
public class RequestCreateAuctionAccount {
    public JsonArray keylist = new JsonArray();
    public long initialBalance = 0;
}
