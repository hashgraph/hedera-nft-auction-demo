/*
 * This file is generated by jOOQ.
 */
package com.hedera.demo.auction.node.app.domain;


import io.github.jklingsporn.vertx.jooq.shared.internal.VertxPojo;
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.Row;
import org.jooq.Record;

import java.io.Serializable;

import static com.hedera.demo.auction.node.app.db.Tables.BIDS;
public class Bid implements VertxPojo, Serializable {

    private static final long serialVersionUID = -286838882;

    private String timestamp = "";
    private Integer auctionid = 0;
    private String bidderaccountid = "";
    private Long bidamount = 0L;
    private String status = "";
    public Boolean refunded = false;
    private String refundtxid = "";
    private String refundtxhash = "";
    private String transactionid = "";
    private String transactionhash = "";

    public Bid() {}

    public Bid(Row row) {
        this.auctionid = row.getInteger(BIDS.AUCTIONID.getName());
        this.bidamount = row.getLong(BIDS.BIDAMOUNT.getName());
        this.bidderaccountid = row.getString(BIDS.BIDDERACCOUNTID.getName());
        this.refunded = row.getBoolean(BIDS.REFUNDED.getName());
        this.refundtxid = row.getString(BIDS.REFUNDTXID.getName());
        this.refundtxhash = row.getString(BIDS.REFUNDTXHASH.getName());
        this.transactionid = row.getString(BIDS.TRANSACTIONID.getName());
        this.status = row.getString(BIDS.STATUS.getName());
        this.timestamp = row.getString(BIDS.TIMESTAMP.getName());
        this.transactionhash = row.getString(BIDS.TRANSACTIONHASH.getName());
    }

    public Bid(Record record) {
        this.auctionid = record.get(BIDS.AUCTIONID);
        this.bidamount = record.get(BIDS.BIDAMOUNT);
        this.bidderaccountid = record.get(BIDS.BIDDERACCOUNTID);
        this.refunded = record.get(BIDS.REFUNDED);
        this.refundtxid = record.get(BIDS.REFUNDTXID);
        this.refundtxhash = record.get(BIDS.REFUNDTXHASH);
        this.transactionid = record.get(BIDS.TRANSACTIONID);
        this.status = record.get(BIDS.STATUS);
        this.timestamp = record.get(BIDS.TIMESTAMP);
        this.transactionhash = record.get(BIDS.TRANSACTIONHASH);
    }

//    public Bid(
//        String timestamp,
//        Integer auctionid,
//        String bidderaccountid,
//        Long bidamount,
//        String status,
//        Boolean refunded,
//        String refundtxid,
//        String refundtxhash,
//        String transactionid,
//        String transactionhash
//    ) {
//        this.timestamp = timestamp;
//        this.auctionid = auctionid;
//        this.bidderaccountid = bidderaccountid;
//        this.bidamount = bidamount;
//        this.status = status;
//        this.refunded = refunded;
//        this.refundtxid = refundtxid;
//        this.refundtxhash = refundtxhash;
//        this.transactionid = transactionid;
//        this.transactionhash = transactionhash;
//    }

    public Bid(io.vertx.core.json.JsonObject json) {
        this();
        fromJson(json);
    }

    public String getTimestamp() {
        return this.timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public Integer getAuctionid() {
        return this.auctionid;
    }

    public void setAuctionid(Integer auctionid) {
        this.auctionid = auctionid;
    }

    public String getBidderaccountid() {
        return this.bidderaccountid;
    }

    public void setBidderaccountid(String bidderaccountid) {
        this.bidderaccountid = bidderaccountid;
    }

    public Long getBidamount() {
        return this.bidamount;
    }

    public void setBidamount(Long bidamount) {
        this.bidamount = bidamount;
    }

    public String getStatus() {
        return this.status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Boolean getRefunded() {
        return this.refunded;
    }

    public void setRefunded(Boolean refunded) {
        this.refunded = refunded;
    }

    public String getRefundtxid() {
        return this.refundtxid;
    }

    public void setRefundtxid(String refundtxid) {
        this.refundtxid = refundtxid;
    }

    public String getRefundtxhash() {
        return this.refundtxhash;
    }

    public void setRefundtxhash(String refundtxhash) {
        this.refundtxhash = refundtxhash;
    }

    public String getTransactionid() {
        return this.transactionid;
    }

    public void setTransactionid(String transactionid) {
        this.transactionid = transactionid;
    }

    public String getTransactionhash() {
        return this.transactionhash;
    }

    public void setTransactionhash(String transactionhash) {
        this.transactionhash = transactionhash;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Bids (");

        sb.append(timestamp);
        sb.append(", ").append(auctionid);
        sb.append(", ").append(bidderaccountid);
        sb.append(", ").append(bidamount);
        sb.append(", ").append(status);
        sb.append(", ").append(refunded);
        sb.append(", ").append(refundtxid);
        sb.append(", ").append(refundtxhash);
        sb.append(", ").append(transactionid);
        sb.append(", ").append(transactionhash);

        sb.append(")");
        return sb.toString();
    }

    @Override
    public Bid fromJson(io.vertx.core.json.JsonObject json) {
        this.setTimestamp(json.getString("timestamp"));
        this.setAuctionid(json.getInteger("auctionid"));
        this.setBidderaccountid(json.getString("bidderaccountid"));
        this.setBidamount(json.getLong("bidamount"));
        this.setStatus(json.getString("status"));
        this.setRefunded(json.getBoolean("refunded"));
        this.setRefundtxid(json.getString("refundtxid"));
        this.setRefundtxhash(json.getString("refundtxhash"));
        this.setTransactionid(json.getString("transactionid"));
        this.setTransactionhash(json.getString("transactionhash"));
        return this;
    }

    @Override
    public JsonObject toJson() {
        JsonObject json = new io.vertx.core.json.JsonObject();
        json.put("timestamp",getTimestamp());
        json.put("auctionid",getAuctionid());
        json.put("bidderaccountid",getBidderaccountid());
        json.put("bidamount",getBidamount());
        json.put("status",getStatus());
        json.put("refunded",getRefunded());
        json.put("refundtxid",getRefundtxid());
        json.put("refundtxhash", getRefundtxhash());
        json.put("transactionid", getTransactionid());
        json.put("transactionhash", getTransactionhash());
        return json;
    }
}
