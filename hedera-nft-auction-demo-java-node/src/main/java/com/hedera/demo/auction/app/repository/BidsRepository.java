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

/**
 * Repository to manage bids in the database
 */
@Log4j2
public class BidsRepository {
    private final SqlConnectionManager connectionManager;

    /**
     * Constructor
     *
     * @param connectionManager the SqlConnectionManager to the database
     */
    public BidsRepository(SqlConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    /**
     * Gets all the bids from the database ordered by timestamp
     *
     * @return Result<Record> resultset containing bid records
     * @throws SQLException in the event of an error
     */
    @Nullable
    private Result<Record> getBids () throws SQLException {
        DSLContext cx = connectionManager.dsl();

        Result<Record> rows = cx.selectFrom(Tables.BIDS)
                .orderBy(Tables.BIDS.TIMESTAMP)
                .fetch();
        return rows;
    }

    /**
     * Gets a bid from the database (used for testing)
     * Note: Technically this would fail to return bids if several bids for the same amount, bidding account and auction exist.
     * In the context of the usage of this method in integration testing, this is not an issue
     *
     * @param auctionId the auction id the bid pertains to
     * @param accountId the bid account id
     * @param bidAmount the bid amount
     * @return Bid object matching the query parameters
     *
     * @throws SQLException in the event of an error
     */
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

    /**
     * Gets a bid given a timestamp
     *
     * @param timestamp the timestamp to get the bid for
     * @return Bid object matching the query parameters
     * @throws SQLException in the event of an error
     */
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
     *
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

    /**
     * Gets a list of bids
     *
     * @return List<Bid> list of bids
     * @throws SQLException in the event of an error
     */
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

    /**
     * Deletes all bids from the database
     *
     * @throws SQLException in the event of an error
     */
    public void deleteAllBids() throws SQLException {
        DSLContext cx = connectionManager.dsl();
        cx.deleteFrom(Tables.BIDS)
            .execute();
    }

    /**
     * Sets the refund status of a bid to REFUND ISSUED
     *
     * @param consensusTimestamp the timestamp of the bid to update
     * @param transactionId the refund transaction id
     * @param scheduleId the refund schedule id
     * @throws SQLException in the event of an error
     */
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

    /**
     * Sets the bid's refund status to ISSUING
     *
     * @param consensusTimestamp the timestamp of the bid to update
     * @return boolean indicating if a bid was updated or not
     * @throws SQLException in the event of an error
     */
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

    /**
     * Sets the bid's refund status to REFUND PENDING
     *
     * @param bidTransactionId the transaction id of the bid to update
     * @return boolean indicating if a bid was updated or not
     * @throws SQLException in the event of an error
     */
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

    /**
     * Sets the bid's refund status to REFUND ERROR
     *
     * @param bidTransactionId the transaction id of the bid to update
     * @return boolean to indicate if a bid was updated or not
     * @throws SQLException in the event of an error
     */
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

    /**
     * Sets the bid's refund status to REFUNDED, along with the refund transaction id and hash
     *
     * @param bidTransactionId the transaction is of the bid to update
     * @param refundTransactionId the refund transaction id
     * @param refundTransactionHash the refund transaction hash
     * @return boolean indicating if a bid was updated
     * @throws SQLException in the event of an error
     */
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

    /**
     * Adds a bid to the database
     *
     * @param bid Bid object to add to the database
     * @throws SQLException in the event of an error
     */
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

    /**
     * Gets list of bids that are awaiting a refund (REFUND PENDING)
     *
     * @param auctionId the auction id to query bids for
     * @return List<Bid> list of Bid objects
     * @throws SQLException in the event of an error
     */
    public List<Bid> getBidsToRefund(int auctionId) throws SQLException {

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

    /**
     * Gets the list of bids that are either refund ISSUING, ISSUED or ERROR
     *
     * @return List of Bid objects
     * @throws SQLException in the event of an error
     */
    public List<Bid> getOustandingRefunds() throws SQLException {
        List<Bid> bids = new ArrayList<>();
        DSLContext cx = connectionManager.dsl();

        Result<Record> result = cx.selectFrom(Tables.BIDS)
                .where(Tables.BIDS.REFUNDSTATUS.eq(Bid.REFUND_ISSUED))
                .or(Tables.BIDS.REFUNDSTATUS.eq(Bid.REFUND_ISSUING))
                .or(Tables.BIDS.REFUNDSTATUS.eq(Bid.REFUND_ERROR))
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
