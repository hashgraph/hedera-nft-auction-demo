package com.hedera.demo.auction.node.app.repository;

import com.google.errorprone.annotations.Var;
import com.hedera.demo.auction.node.app.SqlConnectionManager;
import com.hedera.demo.auction.node.app.db.Tables;
import com.hedera.demo.auction.node.app.domain.Bid;
import lombok.extern.log4j.Log4j2;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.exception.DataAccessException;

import javax.annotation.Nullable;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.hedera.demo.auction.node.app.db.Tables.BIDS;

@Log4j2
public class BidsRepository {
    private final SqlConnectionManager connectionManager;

    public BidsRepository(SqlConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    @Nullable
    private Result<Record> getBids () throws SQLException {
        DSLContext cx = connectionManager.dsl();
        Result<Record> rows = cx.fetch("SELECT * FROM bids ORDER BY timestamp");
        cx.close();
        return rows;
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
                .where(BIDS.TIMESTAMP.eq(bid.getTimestamp()))
                .execute();
        return true;
    }

    public void setRefundInProgress(String consensusTimestamp, String transactionId, String transactionHash) throws SQLException {
        DSLContext cx = connectionManager.dsl();
        cx.update(Tables.BIDS)
                .set(BIDS.REFUNDTXID, transactionId)
                .set(BIDS.REFUNDTXHASH, transactionHash)
                .where(BIDS.TIMESTAMP.eq(consensusTimestamp))
                .execute();
        cx.close();
    }

    public void setRefunded(String consensusTimestamp, String transactionHash) throws SQLException {
        DSLContext cx = connectionManager.dsl();
        cx.update(Tables.BIDS)
                .set(BIDS.REFUNDED, true)
                .set(BIDS.TRANSACTIONHASH, transactionHash)
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
                    BIDS.TRANSACTIONHASH
            ).values(
                    bid.getAuctionid(),
                    bid.getStatus(),
                    bid.getTimestamp(),
                    bid.getBidamount(),
                    bid.getBidderaccountid(),
                    bid.getTransactionid(),
                    bid.getTransactionhash()
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

    public Map<String, String> bidsRefundToConfirm() throws SQLException {
        DSLContext cx = connectionManager.dsl();
        Map<String, String> rows = cx.select(BIDS.REFUNDTXID, BIDS.TIMESTAMP)
                .from(BIDS)
                .where(BIDS.REFUNDED.eq(false))
                .and(BIDS.REFUNDTXID.ne(""))
                .fetchMap(BIDS.TIMESTAMP, BIDS.REFUNDTXID);
        cx.close();

        return rows;
    }
}
