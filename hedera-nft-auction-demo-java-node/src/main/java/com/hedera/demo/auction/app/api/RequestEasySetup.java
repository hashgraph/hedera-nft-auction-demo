package com.hedera.demo.auction.app.api;

import com.hedera.demo.auction.app.Utils;

/**
 * Data class to map incoming REST JSON to a java object
 */
@SuppressWarnings("unused")
public class RequestEasySetup {
    private String symbol = "TT";
    private String name = "Test Token";
    public boolean clean = true;

    public void setName(String name) {
        this.name = Utils.normalize(name);
    }

    public String getName() {
        return name;
    }

    public void setSymbol(String symbol) {
        this.symbol = Utils.normalize(symbol);
    }

    public String getSymbol() {
        return symbol;
    }
}
