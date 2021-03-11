package com.hedera.demo.auction.web.app.domain;

import io.vertx.sqlclient.Row;

import java.io.Serializable;

import static com.hedera.demo.auction.web.db.Tables.AUCTIONS;

public class Auction implements Serializable {

    private static final long serialVersionUID = -495816041;

    public Integer id;
    public Long winningbid;
    public String winningaccount;
    public String winningtimestamp;
    public String tokenid;
    public String auctionaccountid;
    public String endtimestamp;
    public Long reserve;
    public String status;
    public String winningtxid;
    public String winningtxhash;
    public String tokenimage;

    public Auction() {
    }

    public Auction (Row row) {
        this.id = row.getInteger(AUCTIONS.ID.getName());
        this.auctionaccountid = row.getString(AUCTIONS.AUCTIONACCOUNTID.getName());
        this.endtimestamp = row.getString(AUCTIONS.ENDTIMESTAMP.getName());
        this.winningbid = row.getLong(AUCTIONS.WINNINGBID.getName());
        this.winningaccount = row.getString(AUCTIONS.WINNINGACCOUNT.getName());
        this.winningtimestamp = row.getString(AUCTIONS.WINNINGTIMESTAMP.getName());
        this.tokenid = row.getString(AUCTIONS.TOKENID.getName());
        this.reserve = row.getLong(AUCTIONS.RESERVE.getName());
        this.status = row.getString(AUCTIONS.STATUS.getName());
        this.winningtxid = row.getString(AUCTIONS.WINNINGTXID.getName());
        this.winningtxhash = row.getString(AUCTIONS.WINNINGTXHASH.getName());
        this.tokenimage = row.getString(AUCTIONS.TOKENIMAGE.getName());
    }
}
