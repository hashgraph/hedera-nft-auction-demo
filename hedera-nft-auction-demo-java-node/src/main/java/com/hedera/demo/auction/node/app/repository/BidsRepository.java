package com.hedera.demo.auction.node.app.repository;

import com.google.errorprone.annotations.Var;
import com.hedera.demo.auction.node.app.SqlConnectionManager;
import com.hedera.demo.auction.node.app.db.Tables;
import com.hedera.demo.auction.node.app.domain.Bid;
import lombok.extern.log4j.Log4j2;
import org.jooq.DSLContext;
import org.jooq.exception.DataAccessException;

import java.sql.SQLException;

import static com.hedera.demo.auction.node.app.db.Tables.BIDS;

@Log4j2
public class BidsRepository {
    private final SqlConnectionManager connectionManager;

    public BidsRepository(SqlConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    public void deleteAllBids() {
        @Var DSLContext cx = null;
        try {
            cx = connectionManager.dsl();
            cx.deleteFrom(BIDS)
                .execute();
        } catch (SQLException e) {
            log.error(e);
        } finally {
            if (cx != null) {
                cx.close();
            }
        }
    }

    public boolean setStatus(Bid bid) {
        @Var DSLContext cx = null;
        @Var boolean result = false;
        try {
            cx = connectionManager.dsl();
            cx.update(BIDS)
                    .set(BIDS.STATUS, bid.getStatus())
                    .where(BIDS.TIMESTAMP.eq(bid.getTimestamp()))
                    .execute();
            result = true;
        } catch (SQLException e) {
            log.error(e);
        } finally {
            if (cx != null) {
                cx.close();
            }
        }
        return result;
    }

    public boolean setRefunded(Bid bid) {
        @Var DSLContext cx = null;
        @Var boolean result = false;
        try {
            cx = connectionManager.dsl();
            cx.update(Tables.BIDS)
                    .set(BIDS.REFUNDTXID, bid.getRefundtxid())
                    .where(BIDS.TIMESTAMP.eq(bid.getTimestamp()))
                    .execute();
            result = true;
        } catch (SQLException e) {
            log.error(e);
        } finally {
            if (cx != null) {
                cx.close();
            }
        }
        return result;
    }

    public boolean add(Bid bid) {
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
                    BIDS.TRANSACTIONID
            ).values(bid.getAuctionid(), bid.getStatus(), bid.getTimestamp(), bid.getBidamount(), bid.getBidderaccountid(), bid.getTransactionid()).execute();
            result = true;
        } catch (DataAccessException e) {
            log.info("Bid already in database");
        } catch (SQLException e) {
            log.error(e);
        } finally {
            if (cx != null) {
                cx.close();
            }
        }
        return result;
    }
}
