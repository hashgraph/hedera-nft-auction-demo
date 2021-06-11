package com.hedera.demo.auction.app.repository;

import com.google.errorprone.annotations.Var;
import com.hedera.demo.auction.app.SqlConnectionManager;
import com.hedera.demo.auction.app.db.Tables;
import com.hedera.demo.auction.app.domain.Bid;
import lombok.extern.log4j.Log4j2;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Record1;
import org.jooq.Result;
import org.jooq.exception.DataAccessException;

import javax.annotation.Nullable;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static org.jooq.impl.DSL.min;

@Log4j2
public class BidsRepository {
    private final SqlConnectionManager connectionManager;

    public BidsRepository(SqlConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    @Nullable
    private Result<Record> getBids () throws SQLException {
        DSLContext cx = connectionManager.dsl();

        Result<Record> rows = cx.selectFrom(Tables.BIDS)
                .orderBy(Tables.BIDS.TIMESTAMP)
                .fetch();
        return rows;
    }

    @Nullable
    public Bid getBid (int auctionId, String accountId, long bidAmount) throws SQLException {
        DSLContext cx = connectionManager.dsl();

        Result<Record> result = cx.selectFrom(Tables.BIDS)
                .where(Tables.BIDS.AUCTIONID.eq(auctionId))
                .and(Tables.BIDS.BIDAMOUNT.eq(bidAmount))
                .and(Tables.BIDS.BIDDERACCOUNTID.eq(accountId))
                .fetch();
        if (result.size() != 1) {
            return null;
        } else {
            return new Bid(result.get(0));
        }
    }

    public String getFirstBidToRefund(int auctionId) throws SQLException {
        DSLContext cx = connectionManager.dsl();
        Result<Record1<String>> result = cx.select(min(Tables.BIDS.TIMESTAMP))
                .from(Tables.BIDS)
                .where(Tables.BIDS.AUCTIONID.eq(auctionId))
                .and(Tables.BIDS.REFUNDSTATUS.ne(""))
                .and(Tables.BIDS.REFUNDSTATUS.ne(Bid.REFUND_REFUNDED))
                .fetch();
        if (result.size() != 1) {
            return "";
        } else {
            if (result.get(0).value1() == null) {
                return "";
            } else {
                return result.get(0).value1();
            }
        }
    }

    public List<Bid> getBidsList() throws SQLException {
        List<Bid> bids = new ArrayList<>();
        Result<Record> bidsData = getBids();
        if (bidsData != null) {
            for (Record record : bidsData) {
                Bid bid = new Bid(record);
                bids.add(bid);
            }
        }
        return bids;
    }

    public void deleteAllBids() throws SQLException {
        DSLContext cx = connectionManager.dsl();
        cx.deleteFrom(Tables.BIDS)
            .execute();
    }

    public void setStatus(Bid bid) throws SQLException {
        DSLContext cx = connectionManager.dsl();
        cx.update(Tables.BIDS)
                .set(Tables.BIDS.STATUS, bid.getStatus())
                .set(Tables.BIDS.REFUNDSTATUS, bid.getRefundstatus())
                .set(Tables.BIDS.TIMESTAMPFORREFUND, bid.getTimestampforrefund())
                .where(Tables.BIDS.TIMESTAMP.eq(bid.getTimestamp()))
                .execute();

    }

    public void setRefundIssued(String consensusTimestamp) throws SQLException {
        DSLContext cx = connectionManager.dsl();
        cx.update(Tables.BIDS)
                .set(Tables.BIDS.REFUNDSTATUS, Bid.REFUND_ISSUED)
                .where(Tables.BIDS.TIMESTAMP.eq(consensusTimestamp))
                .execute();
    }

    public void setRefundPending(String bidTransactionId) throws SQLException {
        DSLContext cx = connectionManager.dsl();
        cx.update(Tables.BIDS)
                .set(Tables.BIDS.REFUNDSTATUS, Bid.REFUND_PENDING)
                .set(Tables.BIDS.REFUNDTXHASH, "")
                .set(Tables.BIDS.REFUNDTXID, "")
                .where(Tables.BIDS.TRANSACTIONID.eq(bidTransactionId))
                .execute();
    }

    public boolean setRefunded(String bidTransactionId, String refundTransactionId, String refundTransactionHash) throws SQLException {
        DSLContext cx = connectionManager.dsl();
        int rowsUpdated = cx.update(Tables.BIDS)
                .set(Tables.BIDS.REFUNDSTATUS, Bid.REFUND_REFUNDED)
                .set(Tables.BIDS.REFUNDTXHASH, refundTransactionHash)
                .set(Tables.BIDS.REFUNDTXID, refundTransactionId)
                .where(Tables.BIDS.TRANSACTIONID.eq(bidTransactionId))
                .and(Tables.BIDS.REFUNDSTATUS.ne(Bid.REFUND_REFUNDED))
                .execute();

        return (rowsUpdated != 0);
   }

    public void setBidRefundTimestamp(String consensusTimestamp, String bidRefundTimestamp) throws SQLException {
        DSLContext cx = connectionManager.dsl();
        cx.update(Tables.BIDS)
                .set(Tables.BIDS.TIMESTAMPFORREFUND, bidRefundTimestamp)
                .where(Tables.BIDS.TIMESTAMP.eq(consensusTimestamp))
                .execute();
    }

    public boolean add(Bid bid) throws SQLException {
        @Var DSLContext cx = null;
        @Var boolean result = false;
        try {
            cx = connectionManager.dsl();

            cx.insertInto(Tables.BIDS,
                    Tables.BIDS.AUCTIONID,
                    Tables.BIDS.STATUS,
                    Tables.BIDS.TIMESTAMP,
                    Tables.BIDS.BIDAMOUNT,
                    Tables.BIDS.BIDDERACCOUNTID,
                    Tables.BIDS.TRANSACTIONID,
                    Tables.BIDS.TRANSACTIONHASH,
                    Tables.BIDS.REFUNDSTATUS,
                    Tables.BIDS.TIMESTAMPFORREFUND
            ).values(
                    bid.getAuctionid(),
                    bid.getStatus(),
                    bid.getTimestamp(),
                    bid.getBidamount(),
                    bid.getBidderaccountid(),
                    bid.getTransactionid(),
                    bid.getTransactionhash(),
                    bid.getRefundstatus(),
                    bid.getTimestamp()
            ).execute();

        } catch (DataAccessException e) {
            log.info("Bid already in database");
        }
        return result;
    }

    public List<Bid> bidsToRefund(int auctionId) throws SQLException {

        List<Bid> bids = new ArrayList<>();
        DSLContext cx = connectionManager.dsl();

        Result<Record> result = cx.selectFrom(Tables.BIDS)
                .where(Tables.BIDS.AUCTIONID.eq(auctionId))
                .and(Tables.BIDS.REFUNDSTATUS.eq(Bid.REFUND_PENDING))
                .fetch();

        if (result != null) {
            for (Record record : result) {
                Bid bid = new Bid(record);
                bids.add(bid);
            }
        }
        return bids;
    }
}
