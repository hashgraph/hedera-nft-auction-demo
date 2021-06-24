/*
 * This file is generated by jOOQ.
 */
package com.hedera.demo.auction.app.domain;


import com.hedera.demo.auction.app.db.Tables;
import io.github.jklingsporn.vertx.jooq.shared.internal.VertxPojo;
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.Row;
import org.jooq.Record;

import java.io.Serializable;

public class ScheduledOperation implements VertxPojo, Serializable {

    private static final long serialVersionUID = -495816041;

    public static final String TYPE_TOKENASSOCIATE = "TOKENASSOCIATE";
    public static final String TYPE_CRYPTOTRANSFER = "CRYPTOTRANSFER";

    public static final String PENDING = "PENDING";
    public static final String EXECUTING = "EXECUTING";
    public static final String SUCCESSFUL = "SUCCESSFUL";

    private String transactiontype = "";
    private String transactiontimestamp = "";
    private int auctionid = 0;
    private String transactionid = "";
    private String memo = "";
    private String result = "";
    private String status = "";

    public ScheduledOperation() {}

    public ScheduledOperation(JsonObject json) {
        this();
        fromJson(json);
    }

    public ScheduledOperation(Record record) {
        this.transactiontype = record.get(Tables.SCHEDULEDOPERATIONS.TRANSACTIONTYPE);
        this.transactiontimestamp = record.get(Tables.SCHEDULEDOPERATIONS.TRANSACTIONTIMESTAMP);
        this.auctionid = record.get(Tables.SCHEDULEDOPERATIONS.AUCTIONID);
        this.transactionid = record.get(Tables.SCHEDULEDOPERATIONS.TRANSACTIONID);
        this.memo = record.get(Tables.SCHEDULEDOPERATIONS.MEMO);
        this.result = record.get(Tables.SCHEDULEDOPERATIONS.RESULT);
        this.status = record.get(Tables.SCHEDULEDOPERATIONS.STATUS);
    }

    public ScheduledOperation(Row row) {
        this.transactiontype = row.getString(Tables.SCHEDULEDOPERATIONS.TRANSACTIONTYPE.getName());
        this.transactiontimestamp = row.getString(Tables.SCHEDULEDOPERATIONS.TRANSACTIONTIMESTAMP.getName());
        this.auctionid = row.getInteger(Tables.SCHEDULEDOPERATIONS.AUCTIONID.getName());
        this.transactionid = row.getString(Tables.SCHEDULEDOPERATIONS.TRANSACTIONID.getName());
        this.memo = row.getString(Tables.SCHEDULEDOPERATIONS.MEMO.getName());
        this.result = row.getString(Tables.SCHEDULEDOPERATIONS.RESULT.getName());
        this.status = row.getString(Tables.SCHEDULEDOPERATIONS.STATUS.getName());
    }

    public boolean isPending() {
        return this.status.equals(ScheduledOperation.PENDING);
    }
    public boolean isExecuting() {
        return this.status.equals(ScheduledOperation.EXECUTING);
    }
    public boolean isSuccessful() {
        return this.status.equals(ScheduledOperation.SUCCESSFUL);
    }

    public void setTransactiontype(String transactiontype) {
        this.transactiontype = transactiontype;
    }
    public String getTransactiontype() {
        return this.transactiontype;
    }

    public void setTransactiontimestamp(String transactiontimestamp) {
        this.transactiontimestamp = transactiontimestamp;
    }
    public String getTransactiontimestamp() {
        return this.transactiontimestamp;
    }

    public void setAuctionid(int auctionid) {
        this.auctionid = auctionid;
    }
    public int getAuctionid() {
        return this.auctionid;
    }

    public void setTransactionid(String transactionid) {
        this.transactionid = transactionid;
    }
    public String getTransactionid() {
        return this.transactionid;
    }

    public void setMemo(String memo) {
        this.memo = memo;
    }
    public String getMemo() {
        return this.memo;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getResult() {
        return result;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("ScheduledOperations (");

        sb.append(transactiontype);
        sb.append(", ").append(transactiontimestamp);
        sb.append(", ").append(auctionid);
        sb.append(", ").append(transactionid);
        sb.append(", ").append(memo);
        sb.append(", ").append(result);
        sb.append(", ").append(status);

        sb.append(")");
        return sb.toString();
    }

    @Override
    public ScheduledOperation fromJson(JsonObject json) {
        this.setTransactiontype(json.getString("transactiontype"));
        this.setTransactiontimestamp(json.getString("transactiontimestamp"));
        this.setAuctionid(json.getInteger("auctionid"));
        this.setTransactionid(json.getString("transactionid"));
        this.setMemo(json.getString("memo"));
        this.setResult(json.getString("result"));
        this.setStatus(json.getString("status"));

        return this;
    }


    @Override
    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.put("transactiontype",getTransactiontype());
        json.put("transactiontimestamp",getTransactiontimestamp());
        json.put("auctionid",getAuctionid());
        json.put("transactionid",getTransactionid());
        json.put("memo",getMemo());
        json.put("result",getResult());
        json.put("status",getStatus());

        return json;
    }
}