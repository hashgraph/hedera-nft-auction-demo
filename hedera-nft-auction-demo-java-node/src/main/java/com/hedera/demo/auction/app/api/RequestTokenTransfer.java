package com.hedera.demo.auction.app.api;

import com.hedera.hashgraph.sdk.AccountId;
import com.hedera.hashgraph.sdk.TokenId;

/**
 * Data class to map incoming REST JSON to a java object
 */
@SuppressWarnings("unused")
public class RequestTokenTransfer {
    public String tokenid = "";
    public String auctionaccountid = "";

    public String isValid() {
        try {
            TokenId.fromString(tokenid);
        } catch (@SuppressWarnings("UnusedException") Exception e) {
            return "invalid format for tokenid, should be 0.0.1234";
        }

        try {
            AccountId.fromString(auctionaccountid);
        } catch (@SuppressWarnings("UnusedException") Exception e) {
            return "invalid format for auctionaccountid, should be 0.0.1234";
        }

        return "";
    }
}
