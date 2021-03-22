package com.hedera.demo.auction.node.app.repository;

import com.google.errorprone.annotations.Var;
import com.hedera.demo.auction.node.app.SqlConnectionManager;
import com.hedera.demo.auction.node.app.domain.Auction;
import io.vertx.core.json.JsonArray;
import lombok.extern.log4j.Log4j2;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.exception.DataAccessException;

import javax.annotation.Nullable;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.hedera.demo.auction.node.app.db.Tables.AUCTIONS;

@Log4j2
public class AuctionsRepository {
    private final SqlConnectionManager connectionManager;

    public AuctionsRepository(SqlConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    @Nullable
    private Result<Record> getAuctions () {
        @Var DSLContext cx = null;
        @Var Result<Record> rows = null;
        try {
            cx = connectionManager.dsl();
            rows = cx.fetch("SELECT * FROM auctions");
        } catch (Exception e) {
            if (cx != null) {
                cx.close();
            }
        } finally {
            if (cx != null) {
                cx.close();
            }
        }
        return rows;
    }

    public void deleteAllAuctions() {
        @Var DSLContext cx = null;
        try {
            cx = connectionManager.dsl();
            cx.deleteFrom(AUCTIONS)
                    .execute();
        } catch (SQLException e) {
            log.error(e);
        } finally {
            if (cx != null) {
                cx.close();
            }
        }
    }

    public JsonArray getAuctionsJson() {
        JsonArray auctions = new JsonArray();
        Result<Record> auctionsData = getAuctions();
        if (auctionsData != null) {
            for (Record record : auctionsData) {
                Auction auction = new Auction(record);
                auctions.add(auction.toJson());
            }
        }
        return auctions;
    }

    public List<Auction> getAuctionsList() {
        List<Auction> auctions = new ArrayList<>();
        Result<Record> auctionsData = getAuctions();
        if (auctionsData != null) {
            for (Record record : auctionsData) {
                Auction auction = new Auction(record);
                auction.setId(record.get(AUCTIONS.ID));
                auction.setLastconsensustimestamp(record.get(AUCTIONS.LASTCONSENSUSTIMESTAMP));
                auction.setWinningbid(record.get(AUCTIONS.WINNINGBID));
                auction.setWinningaccount(record.get(AUCTIONS.WINNINGACCOUNT));
                auction.setWinningtimestamp(record.get(AUCTIONS.WINNINGTIMESTAMP));
                auction.setTokenid(record.get(AUCTIONS.TOKENID));
                auction.setAuctionaccountid(record.get(AUCTIONS.AUCTIONACCOUNTID));
                auction.setEndtimestamp(record.get(AUCTIONS.ENDTIMESTAMP));
                auction.setReserve(record.get(AUCTIONS.RESERVE));
                auction.setStatus(record.get(AUCTIONS.STATUS));

                auctions.add(auction);
            }
        }
        return auctions;
    }

    public Auction setActive(Auction auction, String timestamp) throws SQLException {
        @Var DSLContext cx = null;
        try {
            cx = connectionManager.dsl();

            cx.update(AUCTIONS)
                    .set(AUCTIONS.STATUS, Auction.active())
                    .set(AUCTIONS.STARTTIMESTAMP, timestamp)
                    .where(AUCTIONS.AUCTIONACCOUNTID.eq(auction.getAuctionaccountid()))
                    .execute();
            cx.close();
        } catch (Exception e) {
            if (cx != null) {
                cx.close();
                throw e;
            }
        }
        auction.setStatus(Auction.active());
        return auction;

    }

    public Auction setClosed(Auction auction) throws SQLException {
        updateStatus(auction.getAuctionaccountid(), Auction.closed());
        auction.setStatus(Auction.closed());
        return auction;
    }

    public void setClosed(int auctionId) throws SQLException {

        @Var DSLContext cx = null;
        try {
            cx = connectionManager.dsl();

            cx.update(AUCTIONS)
                    .set(AUCTIONS.STATUS, Auction.closed())
                    .where(AUCTIONS.ID.eq(auctionId))
                    .execute();
            cx.close();
        } catch (Exception e) {
            if (cx != null) {
                cx.close();
                throw e;
            }
        }
    }

    private void updateStatus(String auctionAccountId, String newStatus) throws SQLException {
        @Var DSLContext cx = null;
        try {
            cx = connectionManager.dsl();

            cx.update(AUCTIONS)
                    .set(AUCTIONS.STATUS, newStatus)
                    .where(AUCTIONS.AUCTIONACCOUNTID.eq(auctionAccountId))
                    .execute();
            cx.close();
        } catch (Exception e) {
            if (cx != null) {
                cx.close();
                throw e;
            }
        }
    }

    public boolean save(Auction auction) {
        @Var DSLContext cx = null;
        @Var boolean result = false;
        try {
            cx = connectionManager.dsl();
            cx.update(AUCTIONS)
                    .set(AUCTIONS.LASTCONSENSUSTIMESTAMP, auction.getLastconsensustimestamp())
                    .set(AUCTIONS.WINNINGACCOUNT, auction.getWinningaccount())
                    .set(AUCTIONS.WINNINGBID, auction.getWinningbid())
                    .set(AUCTIONS.WINNINGTIMESTAMP, auction.getWinningtimestamp())
                    .set(AUCTIONS.WINNINGTXID, auction.getWinningtxid())
                    .set(AUCTIONS.WINNINGTXHASH, auction.getWinningtxhash())
                    .where(AUCTIONS.AUCTIONACCOUNTID.eq(auction.getAuctionaccountid()))
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

    public Auction add(Auction auction) {
        @Var DSLContext cx = null;
        try {
            cx = connectionManager.dsl();
            cx.insertInto(AUCTIONS,
                    AUCTIONS.TOKENID,
                    AUCTIONS.AUCTIONACCOUNTID,
                    AUCTIONS.ENDTIMESTAMP,
                    AUCTIONS.RESERVE,
                    AUCTIONS.LASTCONSENSUSTIMESTAMP,
                    AUCTIONS.WINNERCANBID,
                    AUCTIONS.TOKENIMAGE,
                    AUCTIONS.WINNINGBID,
                    AUCTIONS.MINIMUMBID
            ).values(auction.getTokenid(),
                    auction.getAuctionaccountid(),
                    auction.getEndtimestamp(),
                    auction.getReserve(),
                    "0.0",
                    auction.getWinnerCanBid(),
                    auction.getTokenimage(),
                    auction.getWinningbid(),
                    auction.getMinimumbid()
            ).returning(AUCTIONS.ID).execute();
            int id = cx.lastID().intValue();
            auction.setId(id);
        } catch (DataAccessException e) {
            log.info("Auction already in database");
        } catch (SQLException e) {
            log.error(e);
        } finally {
            if (cx != null) {
                cx.close();
            }
        }
        return auction;
    }

    public Map<String, Integer> openPendingAuctions() {
        @Var DSLContext cx = null;
        @Var Map<String, Integer> rows = new HashMap<>();
        try {
            cx = connectionManager.dsl();
            rows = cx.select(AUCTIONS.ID, AUCTIONS.ENDTIMESTAMP)
                    .from(AUCTIONS)
                    .where(AUCTIONS.STATUS.ne("CLOSED"))
                    .fetchMap(AUCTIONS.ENDTIMESTAMP, AUCTIONS.ID);
        } catch (SQLException e) {
            log.error(e);
        } finally {
            if (cx != null) {
                cx.close();
            }
        }

        return rows;
    }
}
