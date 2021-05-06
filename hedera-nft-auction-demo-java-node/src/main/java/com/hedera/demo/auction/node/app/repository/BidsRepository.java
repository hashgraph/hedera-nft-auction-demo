package com.hedera.demo.auction.node.app.repository;

import com.google.errorprone.annotations.Var;
import com.hedera.demo.auction.node.app.SqlConnectionManager;
import com.hedera.demo.auction.node.app.db.Tables;
import com.hedera.demo.auction.node.app.domain.Bid;
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

import static com.hedera.demo.auction.node.app.db.Tables.BIDS;
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

        Result<Record> rows = cx.selectFrom(BIDS)
                .orderBy(BIDS.TIMESTAMP)
                .fetch();
        cx.close();
        return rows;
    }

    @Nullable
    public Bid getBid (int auctionId, String accountId, long bidAmount) throws SQLException {
        DSLContext cx = connectionManager.dsl();

        Result<Record> result = cx.selectFrom(BIDS)
                .where(BIDS.AUCTIONID.eq(auctionId))
                .and(BIDS.BIDAMOUNT.eq(bidAmount))
                .and(BIDS.BIDDERACCOUNTID.eq(accountId))
                .fetch();
        cx.close();
        if (result.size() != 1) {
            return null;
        } else {
            return new Bid(result.get(0));
        }
    }

    public String getFirstBidToRefund(int auctionId) throws SQLException {
        DSLContext cx = connectionManager.dsl();
        Result<Record1<String>> result = cx.select(min(BIDS.TIMESTAMP))
                .from(BIDS)
                .where(BIDS.AUCTIONID.eq(auctionId))
                .and(BIDS.REFUNDSTATUS.ne(""))
                .and(BIDS.REFUNDSTATUS.ne(Bid.REFUND_REFUNDED))
                .fetch();
        cx.close();
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
        cx.deleteFrom(BIDS)
            .execute();
        cx.close();
    }

    public boolean setStatus(Bid bid) throws SQLException {
        DSLContext cx = connectionManager.dsl();
        cx.update(BIDS)
                .set(BIDS.STATUS, bid.getStatus())
                .set(BIDS.REFUNDSTATUS, bid.getRefundstatus())
                .where(BIDS.TIMESTAMP.eq(bid.getTimestamp()))
                .execute();
        return true;
    }

    public void setRefundIssued(String consensusTimestamp) throws SQLException {
        DSLContext cx = connectionManager.dsl();
        cx.update(Tables.BIDS)
                .set(BIDS.REFUNDSTATUS, Bid.REFUND_ISSUED)
                .where(BIDS.TIMESTAMP.eq(consensusTimestamp))
                .execute();
        cx.close();
    }

    public void setRefundPending(String bidTransactionId) throws SQLException {
        DSLContext cx = connectionManager.dsl();
        cx.update(Tables.BIDS)
                .set(BIDS.REFUNDSTATUS, Bid.REFUND_PENDING)
                .set(BIDS.REFUNDTXHASH, "")
                .set(BIDS.REFUNDTXID, "")
                .where(BIDS.TRANSACTIONID.eq(bidTransactionId))
                .execute();
        cx.close();
    }

    public void setRefunded(String bidTransactionId, String refundTransactionId, String refundTransactionHash) throws SQLException {
        DSLContext cx = connectionManager.dsl();
        cx.update(Tables.BIDS)
                .set(BIDS.REFUNDSTATUS, Bid.REFUND_REFUNDED)
                .set(BIDS.REFUNDTXHASH, refundTransactionHash)
                .set(BIDS.REFUNDTXID, refundTransactionId)
                .where(BIDS.TRANSACTIONID.eq(bidTransactionId))
                .execute();
        cx.close();
   }

    public void setBidRefundTimestamp(String consensusTimestamp, String bidRefundTimestamp) throws SQLException {
        DSLContext cx = connectionManager.dsl();
        cx.update(Tables.BIDS)
                .set(BIDS.TIMESTAMPFORREFUND, bidRefundTimestamp)
                .where(BIDS.TIMESTAMP.eq(consensusTimestamp))
                .execute();
        cx.close();
    }

    public boolean add(Bid bid) throws SQLException {
        @Var DSLContext cx = null;
        @Var boolean result = false;
        try {
            cx = connectionManager.dsl();
            cx.insertInto(BIDS,
                    BIDS.AUCTIONID,
                    BIDS.STATUS,
                    BIDS.TIMESTAMP,
                    BIDS.BIDAMOUNT,
                    BIDS.BIDDERACCOUNTID,
                    BIDS.TRANSACTIONID,
                    BIDS.TRANSACTIONHASH,
                    BIDS.REFUNDSTATUS,
                    BIDS.TIMESTAMPFORREFUND
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
            result = true;
        } catch (DataAccessException e) {
            log.info("Bid already in database");
        } finally {
            if (cx != null) {
                cx.close();
            }
        }
        return result;
    }

    public List<Bid> bidsToRefund(int auctionId) throws SQLException {

        List<Bid> bids = new ArrayList<>();
        DSLContext cx = connectionManager.dsl();

        Result<Record> result = cx.selectFrom(BIDS)
                .where(BIDS.AUCTIONID.eq(auctionId))
                .and(BIDS.REFUNDSTATUS.eq(Bid.REFUND_PENDING))
                .fetch();
        cx.close();

        if (result != null) {
            for (Record record : result) {
                Bid bid = new Bid(record);
                bids.add(bid);
            }
        }
        return bids;
    }
}
