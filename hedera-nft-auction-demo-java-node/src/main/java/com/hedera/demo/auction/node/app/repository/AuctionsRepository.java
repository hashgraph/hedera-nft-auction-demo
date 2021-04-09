package com.hedera.demo.auction.node.app.repository;

import com.google.errorprone.annotations.Var;
import com.hedera.demo.auction.node.app.SqlConnectionManager;
import com.hedera.demo.auction.node.app.domain.Auction;
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
            rows = cx.fetch("SELECT * FROM auctions ORDER BY id");
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

    @Nullable
    private Record getAuctionData (int auctionId) {
        @Var DSLContext cx = null;
        @Var Record auctionRecord = null;
        try {
            cx = connectionManager.dsl();
            auctionRecord = cx.selectFrom(AUCTIONS)
                    .where(AUCTIONS.ID.eq(auctionId))
                    .fetchAny();
        } catch (SQLException e) {
            log.error(e);
        } finally {
            if (cx != null) {
                cx.close();
            }
        }

        return auctionRecord;
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

//    public JsonArray getAuctionsJson() {
//        JsonArray auctions = new JsonArray();
//        Result<Record> auctionsData = getAuctions();
//        if (auctionsData != null) {
//            for (Record record : auctionsData) {
//                Auction auction = new Auction(record);
//                auctions.add(auction.toJson());
//            }
//        }
//        return auctions;
//    }
//
    public List<Auction> getAuctionsList() {
        List<Auction> auctions = new ArrayList<>();
        Result<Record> auctionsData = getAuctions();
        if (auctionsData != null) {
            for (Record record : auctionsData) {
                Auction auction = new Auction(record);
                auctions.add(auction);
            }
        }
        return auctions;
    }

    public Auction getAuction(int auctionId) throws Exception {
        Record auctionData = getAuctionData(auctionId);

        if (auctionData != null) {
            return new Auction(auctionData);
        } else {
            throw new Exception("No auction id " + auctionId);
        }
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

    public void setTransferring(String tokenId) throws SQLException {
        @Var DSLContext cx = null;
        try {
            cx = connectionManager.dsl();

            cx.update(AUCTIONS)
                    .set(AUCTIONS.STATUS, Auction.transfer())
                    .where(AUCTIONS.TOKENID.eq(tokenId))
                    .execute();
            cx.close();
        } catch (Exception e) {
            if (cx != null) {
                cx.close();
                throw e;
            }
        }
    }

    public void setTransferTransaction(int auctionId, String transactionId, String transactionHash) throws SQLException {
        @Var DSLContext cx = null;
        try {
            cx = connectionManager.dsl();

            cx.update(AUCTIONS)
                    .set(AUCTIONS.TRANSFERTXID, transactionId)
                    .set(AUCTIONS.TRANSFERTXHASH, transactionHash)
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

    public void setEnded(int auctionId, String transferTransactionHash) throws SQLException {
        @Var DSLContext cx = null;
        try {
            cx = connectionManager.dsl();

            cx.update(AUCTIONS)
                    .set(AUCTIONS.STATUS, Auction.ended())
                    .set(AUCTIONS.TRANSFERTXHASH, transferTransactionHash)
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

    public Map<String, Integer> openAndPendingAuctions() {
        @Var DSLContext cx = null;
        @Var Map<String, Integer> rows = new HashMap<>();
        try {
            cx = connectionManager.dsl();
            rows = cx.select(AUCTIONS.ID, AUCTIONS.ENDTIMESTAMP)
                    .from(AUCTIONS)
                    .where(AUCTIONS.STATUS.eq(Auction.active()))
                    .or(AUCTIONS.STATUS.eq(Auction.pending()))
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

    // for testing
    public Auction createComplete(Auction auction) {
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
                    AUCTIONS.MINIMUMBID,
                    AUCTIONS.WINNINGACCOUNT,
                    AUCTIONS.WINNINGTIMESTAMP,
                    AUCTIONS.STATUS,
                    AUCTIONS.WINNINGTXID,
                    AUCTIONS.WINNINGTXHASH,
                    AUCTIONS.STARTTIMESTAMP,
                    AUCTIONS.TRANSFERTXID,
                    AUCTIONS.TRANSFERTXHASH
            ).values(auction.getTokenid(),
                    auction.getAuctionaccountid(),
                    auction.getEndtimestamp(),
                    auction.getReserve(),
                    auction.getLastconsensustimestamp(),
                    auction.getWinnerCanBid(),
                    auction.getTokenimage(),
                    auction.getWinningbid(),
                    auction.getMinimumbid(),
                    auction.getWinningaccount(),
                    auction.getWinningtimestamp(),
                    auction.getStatus(),
                    auction.getWinningtxid(),
                    auction.getWinningtxhash(),
                    auction.getStarttimestamp(),
                    auction.getTransfertxid(),
                    auction.getTransfertxhash()
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
}
