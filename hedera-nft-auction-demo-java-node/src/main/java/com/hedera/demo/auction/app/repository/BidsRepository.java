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
import org.jooq.tools.StringUtils;

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

    @Nullable
    public Bid getBidForTimestamp (String timestamp) throws SQLException {
        DSLContext cx = connectionManager.dsl();

        Result<Record> result = cx.selectFrom(Tables.BIDS)
                .where(Tables.BIDS.TIMESTAMP.eq(timestamp))
                .fetch();
        if (result.size() != 1) {
            return null;
        } else {
            return new Bid(result.get(0));
        }
    }

    /**
     * Gets the minimum timestamp for bids which have a status
     * which is neither empty, "REFUNDED" or "ERROR" for a given auction
     * @param auctionId the id of the auction
     * @return String lowest timestamp of all bids matching query
     * @throws SQLException in the event of an error
     */
    public String getFirstBidToRefund(int auctionId) throws SQLException {
        DSLContext cx = connectionManager.dsl();
        Result<Record1<String>> result = cx.select(min(Tables.BIDS.TIMESTAMP))
                .from(Tables.BIDS)
                .where(Tables.BIDS.AUCTIONID.eq(auctionId))
                .and(Tables.BIDS.REFUNDSTATUS.ne(""))
                .and(Tables.BIDS.REFUNDSTATUS.ne(Bid.REFUND_REFUNDED))
                .and(Tables.BIDS.REFUNDSTATUS.ne(Bid.REFUND_ERROR))
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

    public void setRefundIssued(String consensusTimestamp, String transactionId, String scheduleId) throws SQLException {
        DSLContext cx = connectionManager.dsl();

        if (StringUtils.isEmpty(transactionId)) {
            cx.update(Tables.BIDS)
                    .set(Tables.BIDS.REFUNDSTATUS, Bid.REFUND_ISSUED)
                    .set(Tables.BIDS.SCHEDULEID, scheduleId)
                    .where(Tables.BIDS.TIMESTAMP.eq(consensusTimestamp))
                    .and(Tables.BIDS.REFUNDSTATUS.eq(Bid.REFUND_ISSUING))
                    .execute();
        } else {
            cx.update(Tables.BIDS)
                    .set(Tables.BIDS.REFUNDSTATUS, Bid.REFUND_ISSUED)
                    .set(Tables.BIDS.SCHEDULEID, scheduleId)
                    .set(Tables.BIDS.REFUNDTXID, transactionId)
                    .where(Tables.BIDS.TIMESTAMP.eq(consensusTimestamp))
                    .and(Tables.BIDS.REFUNDSTATUS.eq(Bid.REFUND_ISSUING))
                    .execute();
        }
    }

    public boolean setRefundIssuing(String consensusTimestamp) throws SQLException {
        DSLContext cx = connectionManager.dsl();

        int rows = cx.update(Tables.BIDS)
                .set(Tables.BIDS.REFUNDSTATUS, Bid.REFUND_ISSUING)
                .set(Tables.BIDS.SCHEDULEID, "")
                .where(Tables.BIDS.TIMESTAMP.eq(consensusTimestamp))
                .and(Tables.BIDS.REFUNDSTATUS.eq(Bid.REFUND_PENDING))
                .execute();
        return (rows == 1);
    }

    public boolean setRefundPending(String bidTransactionId) throws SQLException {
        DSLContext cx = connectionManager.dsl();
        int rowsUpdated = cx.update(Tables.BIDS)
                .set(Tables.BIDS.REFUNDSTATUS, Bid.REFUND_PENDING)
                .set(Tables.BIDS.SCHEDULEID, "")
                .set(Tables.BIDS.REFUNDTXHASH, "")
                .set(Tables.BIDS.REFUNDTXID, "")
                .where(Tables.BIDS.TRANSACTIONID.eq(bidTransactionId))
                .execute();
        return (rowsUpdated != 0);
    }

    public boolean setRefundError(String bidTransactionId) throws SQLException {
        DSLContext cx = connectionManager.dsl();
        int rowsUpdated = cx.update(Tables.BIDS)
                .set(Tables.BIDS.REFUNDSTATUS, Bid.REFUND_ERROR)
                .set(Tables.BIDS.SCHEDULEID, "")
                .set(Tables.BIDS.REFUNDTXHASH, "")
                .set(Tables.BIDS.REFUNDTXID, "")
                .where(Tables.BIDS.TRANSACTIONID.eq(bidTransactionId))
                .execute();
        return (rowsUpdated != 0);
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

    public void add(Bid bid) throws SQLException {
        @Var DSLContext cx = null;
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
                    Tables.BIDS.SCHEDULEID
            ).values(
                    bid.getAuctionid(),
                    bid.getStatus(),
                    bid.getTimestamp(),
                    bid.getBidamount(),
                    bid.getBidderaccountid(),
                    bid.getTransactionid(),
                    bid.getTransactionhash(),
                    bid.getRefundstatus(),
                    bid.getScheduleId()
            ).execute();

        } catch (DataAccessException e) {
            log.info("Bid already in database");
        }
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
