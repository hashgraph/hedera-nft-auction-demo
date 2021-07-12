package com.hedera.demo.auction.app.api;

import java.util.ArrayList;
import java.util.List;

/**
 * Data class to map incoming REST JSON to a java object
 */
@SuppressWarnings("unused")
public class RequestCreateAuctionAccountKeys {
    public List<RequestCreateAuctionAccountKey> keys = new ArrayList<>();
    public int threshold = 0;
}
