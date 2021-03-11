package com.hedera.demo.auction.web.app.domain;


import io.vertx.sqlclient.Row;

import java.io.Serializable;

import static com.hedera.demo.auction.web.db.Tables.BIDS;

public class Bid implements Serializable {

    private static final long serialVersionUID = -286838882;

    public String timestamp;
    public Integer auctionid;
    public String bidderaccountid;
    public Long bidamount;
    public String status;
    public String refundtxid;
    public String refundtxhash;
    public String transactionid;
    public String transactionhash;

    public Bid() {}

    public Bid(Row row) {
        this.auctionid = row.getInteger(BIDS.AUCTIONID.getName());
        this.bidamount = row.getLong(BIDS.BIDAMOUNT.getName());
        this.bidderaccountid = row.getString(BIDS.BIDDERACCOUNTID.getName());
        this.refundtxid = row.getString(BIDS.REFUNDTXID.getName());
        this.refundtxhash = row.getString(BIDS.REFUNDTXHASH.getName());
        this.transactionid = row.getString(BIDS.TRANSACTIONID.getName());
        this.status = row.getString(BIDS.STATUS.getName());
        this.timestamp = row.getString(BIDS.TIMESTAMP.getName());
        this.transactionhash = row.getString(BIDS.TRANSACTIONHASH.getName());
    }
}
