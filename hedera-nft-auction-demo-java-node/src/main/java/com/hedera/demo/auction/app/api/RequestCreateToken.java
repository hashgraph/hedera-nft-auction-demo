package com.hedera.demo.auction.app.api;

@SuppressWarnings("unused")
public class RequestCreateToken {
    public String name = "Token";
    public String symbol = "TT";
    public long initialSupply = 1;
    public int decimals = 0;
    public String memo = "";
}