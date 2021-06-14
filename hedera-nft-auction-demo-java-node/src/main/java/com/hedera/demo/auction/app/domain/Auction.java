/*
 * This file is generated by jOOQ.
 */
package com.hedera.demo.auction.app.domain;


import io.github.jklingsporn.vertx.jooq.shared.internal.VertxPojo;
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.Row;
import org.jooq.Record;

import java.io.Serializable;

import static com.hedera.demo.auction.app.db.Tables.AUCTIONS;
public class Auction implements VertxPojo, Serializable {

    private static final long serialVersionUID = -495816041;

    private Integer id = 0;
    private String lastconsensustimestamp = "";
    private Long winningbid = 0L;
    private String winningaccount = "";
    private String winningtimestamp = "";
    private String tokenid = "";
    private String auctionaccountid = "";
    private String endtimestamp = "";
    private Long reserve = 0L;
    private String status = "";
    private boolean winnercanbid = false;
    private String winningtxid = "";
    private String winningtxhash = "";
    private String tokenmetadata = "";
    private long minimumbid = 0L;
    private String starttimestamp = "";
    private String transfertxid = "";
    private String transfertxhash = "";
    private String tokenowneraccount = "";
    private String transfertimestamp = "";
    private String transferstatus = "";
    private String title = "";
    private String description = "";

    public Auction() {}

    public Auction(Record record) {
        this.id = record.get(AUCTIONS.ID);
        this.lastconsensustimestamp = record.get(AUCTIONS.LASTCONSENSUSTIMESTAMP);
        this.winningbid = record.get(AUCTIONS.WINNINGBID);
        this.winningaccount = record.get(AUCTIONS.WINNINGACCOUNT);
        this.winningtimestamp = record.get(AUCTIONS.WINNINGTIMESTAMP);
        this.tokenid = record.get(AUCTIONS.TOKENID);
        this.auctionaccountid = record.get(AUCTIONS.AUCTIONACCOUNTID);
        this.endtimestamp = record.get(AUCTIONS.ENDTIMESTAMP);
        this.reserve = record.get(AUCTIONS.RESERVE);
        this.status = record.get(AUCTIONS.STATUS);
        this.winnercanbid = record.get(AUCTIONS.WINNERCANBID);
        this.winningtxid = record.get(AUCTIONS.WINNINGTXID);
        this.winningtxhash = record.get(AUCTIONS.WINNINGTXHASH);
        this.tokenmetadata = record.get(AUCTIONS.TOKENMETADATA);
        this.minimumbid = record.get(AUCTIONS.MINIMUMBID);
        this.starttimestamp = record.get(AUCTIONS.STARTTIMESTAMP);
        this.transfertxid = record.get(AUCTIONS.TRANSFERTXID);
        this.transfertxhash = record.get(AUCTIONS.TRANSFERTXHASH);
        this.tokenowneraccount = record.get(AUCTIONS.TOKENOWNER);
        this.transfertimestamp = record.get(AUCTIONS.TRANSFERTIMESTAMP);
        this.transferstatus = record.get(AUCTIONS.TRANSFERSTATUS);
        this.title = record.get(AUCTIONS.TITLE);
        this.description = record.get(AUCTIONS.DESCRIPTION);
    }

    public Auction (Row row) {
        this.id = row.getInteger(AUCTIONS.ID.getName());
        this.lastconsensustimestamp = row.getString(AUCTIONS.LASTCONSENSUSTIMESTAMP.getName());
        this.auctionaccountid = row.getString(AUCTIONS.AUCTIONACCOUNTID.getName());
        this.endtimestamp = row.getString(AUCTIONS.ENDTIMESTAMP.getName());
        this.winningbid = row.getLong(AUCTIONS.WINNINGBID.getName());
        this.winningaccount = row.getString(AUCTIONS.WINNINGACCOUNT.getName());
        this.winningtimestamp = row.getString(AUCTIONS.WINNINGTIMESTAMP.getName());
        this.tokenid = row.getString(AUCTIONS.TOKENID.getName());
        this.reserve = row.getLong(AUCTIONS.RESERVE.getName());
        this.status = row.getString(AUCTIONS.STATUS.getName());
        this.winnercanbid = row.getBoolean(AUCTIONS.WINNERCANBID.getName());
        this.winningtxid = row.getString(AUCTIONS.WINNINGTXID.getName());
        this.winningtxhash = row.getString(AUCTIONS.WINNINGTXHASH.getName());
        this.tokenmetadata = row.getString(AUCTIONS.TOKENMETADATA.getName());
        this.minimumbid = row.getLong(AUCTIONS.MINIMUMBID.getName());
        this.starttimestamp = row.getString(AUCTIONS.STARTTIMESTAMP.getName());
        this.transfertxid = row.getString(AUCTIONS.TRANSFERTXID.getName());
        this.transfertxhash = row.getString(AUCTIONS.TRANSFERTXHASH.getName());
        this.tokenowneraccount = row.getString(AUCTIONS.TOKENOWNER.getName());
        this.transfertimestamp = row.getString(AUCTIONS.TRANSFERTIMESTAMP.getName());
        this.transferstatus = row.getString(AUCTIONS.TRANSFERSTATUS.getName());
        this.title = row.getString(AUCTIONS.TITLE.getName());
        this.description = row.getString(AUCTIONS.DESCRIPTION.getName());
    }

    public Auction(io.vertx.core.json.JsonObject json) {
        this();
        fromJson(json);
    }

    public static final String PENDING = "PENDING";
    public static final String ACTIVE = "ACTIVE";
    public static final String CLOSED = "CLOSED"; // auction is closed, bids can no longer be accepted
    public static final String ENDED = "ENDED";

    public static final String TRANSFER_STATUS_PENDING = "PENDING";
    public static final String TRANSFER_STATUS_IN_PROGRESS = "IN_PROGRESS";
    public static final String TRANSFER_STATUS_COMPLETE = "COMPLETE";

    public boolean isPending() {
        return this.status.equals(Auction.PENDING);
    }
    public boolean isActive() {
        return this.status.equals(Auction.ACTIVE);
    }
    public boolean isTransferPending() {
        return this.transferstatus.equals(Auction.TRANSFER_STATUS_PENDING);
    }
    public boolean isTransferInProgress() {
        return this.transferstatus.equals(Auction.TRANSFER_STATUS_IN_PROGRESS);
    }
    public boolean isTransferComplete() {
        return this.transferstatus.equals(Auction.TRANSFER_STATUS_COMPLETE);
    }
    public boolean isEnded() {
        return this.status.equals(Auction.ENDED);
    }
    public boolean isClosed() {
        return this.status.equals(Auction.CLOSED);
    }

    public Integer getId() {
        return this.id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getLastconsensustimestamp() {
        return this.lastconsensustimestamp;
    }

    public void setLastconsensustimestamp(String lastconsensustimestamp) {
        this.lastconsensustimestamp = lastconsensustimestamp;
    }

    public Long getWinningbid() {
        return this.winningbid;
    }

    public void setWinningbid(Long winningbid) {
        this.winningbid = winningbid;
    }

    public String getWinningaccount() {
        return this.winningaccount;
    }

    public void setWinningaccount(String winningaccount) {
        this.winningaccount = winningaccount;
    }

    public String getWinningtimestamp() {
        return this.winningtimestamp;
    }

    public void setWinningtimestamp(String winningtimestamp) {
        this.winningtimestamp = winningtimestamp;
    }

    public String getTokenid() {
        return this.tokenid;
    }

    public void setTokenid(String tokenid) {
        this.tokenid = tokenid;
    }

    public String getAuctionaccountid() {
        return this.auctionaccountid;
    }

    public void setAuctionaccountid(String auctionaccountid) {
        this.auctionaccountid = auctionaccountid;
    }

    public String getEndtimestamp() {
        return this.endtimestamp;
    }

    public void setEndtimestamp(String endtimestamp) {
        this.endtimestamp = endtimestamp;
    }

    public Long getReserve() {
        return this.reserve;
    }

    public void setReserve(Long reserve) {
        this.reserve = reserve;
    }

    public String getStatus() {
        return this.status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean getWinnerCanBid() {
        return this.winnercanbid;
    }

    public void setWinnercanbid(boolean winnercanbid) {
        this.winnercanbid = winnercanbid;
    }

    public String getWinningtxid() {
        return this.winningtxid;
    }

    public void setWinningtxid(String winningtxid) {
        this.winningtxid = winningtxid;
    }

    public String getWinningtxhash() {
        return this.winningtxhash;
    }

    public void setWinningtxhash(String winningtxhash) {
        this.winningtxhash = winningtxhash;
    }

    public String getTokenmetadata() {
        return this.tokenmetadata;
    }

    public void setTokenmetadata(String tokenmetadata) {
        this.tokenmetadata = tokenmetadata;
    }

    public Long getMinimumbid() {
        return this.minimumbid;
    }

    public void setMinimumbid(Long minimumbid) {
        this.minimumbid = minimumbid;
    }

    public String getStarttimestamp() {
        return this.starttimestamp;
    }

    public void setStarttimestamp(String starttimestamp) {
        this.starttimestamp = starttimestamp;
    }

    public String getTransfertxid() {
        return this.transfertxid;
    }

    public void setTransfertxid(String transfertxid) {
        this.transfertxid = transfertxid;
    }

    public String getTransfertxhash() {
        return this.transfertxhash;
    }

    public void setTransfertxhash(String transfertxhash) {
        this.transfertxhash = transfertxhash;
    }

    public String getTokenowneraccount() { return this.tokenowneraccount; }

    public void setTokenowneraccount(String tokenowneraccount) { this.tokenowneraccount = tokenowneraccount; }

    public String getTransfertimestamp() {
        return this.transfertimestamp;
    }

    public void setTransfertimestamp(String transfertimestamp) {
        this.transfertimestamp = transfertimestamp;
    }

    public String getTransferstatus() {
        return transferstatus;
    }

    public void setTransferstatus(String transferstatus) {
        this.transferstatus = transferstatus;
    }

    public String getTitle() {
        return this.title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Auctions (");

        sb.append(id);
        sb.append(", ").append(lastconsensustimestamp);
        sb.append(", ").append(winningbid);
        sb.append(", ").append(winningaccount);
        sb.append(", ").append(winningtimestamp);
        sb.append(", ").append(tokenid);
        sb.append(", ").append(auctionaccountid);
        sb.append(", ").append(endtimestamp);
        sb.append(", ").append(reserve);
        sb.append(", ").append(status);
        sb.append(", ").append(winnercanbid);
        sb.append(", ").append(winningtxid);
        sb.append(", ").append(winningtxhash);
        sb.append(", ").append(tokenmetadata);
        sb.append(", ").append(minimumbid);
        sb.append(", ").append(starttimestamp);
        sb.append(", ").append(transfertxid);
        sb.append(", ").append(transfertxhash);
        sb.append(", ").append(tokenowneraccount);
        sb.append(", ").append(transfertimestamp);
        sb.append(", ").append(getTransferstatus());
        sb.append(", ").append(getTitle());
        sb.append(", ").append(getDescription());

        sb.append(")");
        return sb.toString();
    }

    @Override
    public Auction fromJson(io.vertx.core.json.JsonObject json) {
        this.setId(json.getInteger("id"));
        this.setLastconsensustimestamp(json.getString("lastconsensustimestamp","0.0"));
        this.setWinningbid(json.getLong("winningbid", 0L));
        this.setWinningaccount(json.getString("winningaccount", ""));
        this.setWinningtimestamp(json.getString("winningtimestamp", ""));
        this.setTokenid(json.getString("tokenid"));
        this.setAuctionaccountid(json.getString("auctionaccountid"));
        this.setEndtimestamp(json.getString("endtimestamp", ""));
        this.setReserve(json.getLong("reserve", 0L));
        this.setStatus(json.getString("status", "PENDING"));
        this.setWinnercanbid(json.getBoolean("winnercanbid", /* def= */false));
        this.setWinningtxid(json.getString("winningtxid"));
        this.setWinningtxhash(json.getString("winningtxhash"));
        this.setTokenmetadata(json.getString("tokenmetadata"));
        this.setMinimumbid(json.getLong("minimumbid", 0L));
        this.setStarttimestamp(json.getString("starttimestamp", "0.0"));
        this.setTransfertxid(json.getString("transfertxid", ""));
        this.setTransfertxhash(json.getString("transfertxhash", ""));
        this.setTokenowneraccount(json.getString("tokenowneraccount", ""));
        this.setTransfertimestamp(json.getString("transfertimestamp", ""));
        this.setTransferstatus(json.getString("transferstatus", ""));
        this.setTitle(json.getString("title", ""));
        this.setDescription(json.getString("description", ""));
        return this;
    }


    @Override
    public JsonObject toJson() {
        JsonObject json = new io.vertx.core.json.JsonObject();
        json.put("id",getId());
        json.put("lastconsensustimestamp",getLastconsensustimestamp());
        json.put("winningbid",getWinningbid());
        json.put("winningaccount",getWinningaccount());
        json.put("winningtimestamp",getWinningtimestamp());
        json.put("tokenid",getTokenid());
        json.put("auctionaccountid",getAuctionaccountid());
        json.put("endtimestamp",getEndtimestamp());
        json.put("reserve", getReserve());
        json.put("status",getStatus());
        json.put("winnercanbid",getWinnerCanBid());
        json.put("winningtxid", getWinningtxid());
        json.put("winningtxhash", getWinningtxhash());
        json.put("tokenmetadata", getTokenmetadata());
        json.put("minimumbid", getMinimumbid());
        json.put("starttimestamp", getStarttimestamp());
        json.put("transfertxid", getTransfertxid());
        json.put("transfertxhash", getTransfertxhash());
        json.put("tokenowneraccount", getTokenowneraccount());
        json.put("transfertimestamp", getTransfertimestamp());
        json.put("transferstatus", getTransferstatus());
        json.put("title", getTitle());
        json.put("description", getDescription());

        return json;
    }
}
