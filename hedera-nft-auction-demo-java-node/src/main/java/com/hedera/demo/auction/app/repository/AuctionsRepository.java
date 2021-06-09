package com.hedera.demo.auction.app.repository;

import com.google.errorprone.annotations.Var;
import com.hedera.demo.auction.app.SqlConnectionManager;
import com.hedera.demo.auction.app.domain.Auction;
import lombok.extern.log4j.Log4j2;
import org.jooq.Configuration;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.exception.DataAccessException;

import javax.annotation.Nullable;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.hedera.demo.auction.app.db.Tables.AUCTIONS;

@Log4j2
public class AuctionsRepository {
    public final SqlConnectionManager connectionManager;

    public AuctionsRepository(SqlConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    @Nullable
    private Result<Record> getAuctions () throws SQLException {
        DSLContext cx = connectionManager.dsl();
        Result<Record> rows = cx.selectFrom(AUCTIONS).orderBy(AUCTIONS.ID).fetch();
        cx.close();
        return rows;
    }

    @Nullable
    private Record getAuctionData (int auctionId) throws SQLException {
        DSLContext cx = connectionManager.dsl();
        Record auctionRecord = cx.selectFrom(AUCTIONS)
                .where(AUCTIONS.ID.eq(auctionId))
                .fetchAny();
        cx.close();

        return auctionRecord;
    }

    public void setEndTimestampForTesting(int auctionId) throws SQLException {
        // get last consensus timestamp from auction and use to set end timestamp

        DSLContext cx = connectionManager.dsl();
        cx.update(AUCTIONS)
                .set(AUCTIONS.ENDTIMESTAMP, AUCTIONS.LASTCONSENSUSTIMESTAMP)
                .where(AUCTIONS.ID.eq(auctionId))
                .execute();
        cx.close();

    }
    public void deleteAllAuctions() throws SQLException {
        DSLContext cx = connectionManager.dsl();
        cx.deleteFrom(AUCTIONS)
                .execute();
        cx.close();
    }

    public List<Auction> getAuctionsList() throws SQLException {
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

    @Nullable
    public Auction getAuction(String accountId) throws SQLException {
        DSLContext cx = connectionManager.dsl();
        Record auctionData = cx.selectFrom(AUCTIONS).where(AUCTIONS.AUCTIONACCOUNTID.eq(accountId)).fetchAny();
        cx.close();
        if ((auctionData == null) || (auctionData.size() == 0)) {
            return null;
        } else {
            return new Auction(auctionData);
        }
    }

    public Auction setActive(Auction auction, String tokenOwnerAccount, String timestamp) throws SQLException {
        DSLContext cx = connectionManager.dsl();

        cx.update(AUCTIONS)
                .set(AUCTIONS.STATUS, Auction.ACTIVE)
                .set(AUCTIONS.STARTTIMESTAMP, timestamp)
                .set(AUCTIONS.TOKENOWNER, tokenOwnerAccount)
                .where(AUCTIONS.AUCTIONACCOUNTID.eq(auction.getAuctionaccountid()))
                .execute();
        cx.close();
        auction.setStatus(Auction.ACTIVE);
        return auction;

    }

    public void setTransferPending(String tokenId) throws SQLException {
        DSLContext cx = connectionManager.dsl();
        log.debug("Setting auction transfer status to " + Auction.TRANSFER_STATUS_PENDING + " for token id " + tokenId);
        cx.update(AUCTIONS)
                .set(AUCTIONS.TRANSFERSTATUS, Auction.TRANSFER_STATUS_PENDING)
                .set(AUCTIONS.TRANSFERTIMESTAMP, AUCTIONS.ENDTIMESTAMP)
                .where(AUCTIONS.TOKENID.eq(tokenId))
                .execute();
        cx.close();
    }

    public void setTransferTimestamp(int auctionId, String timestamp) throws SQLException {
        DSLContext cx = connectionManager.dsl();
        log.debug("setTransferTimestamp " + auctionId + ", timestamp " + timestamp);

        cx.update(AUCTIONS)
                .set(AUCTIONS.TRANSFERTIMESTAMP, timestamp)
                .where(AUCTIONS.ID.eq(auctionId))
                .execute();
        cx.close();
    }

    public void setTransferInProgress(String tokenId) throws SQLException {
        DSLContext cx = connectionManager.dsl();
        log.debug("setTransferInProgress " + Auction.TRANSFER_STATUS_IN_PROGRESS + " for token id " + tokenId);

        cx.update(AUCTIONS)
                .set(AUCTIONS.TRANSFERSTATUS, Auction.TRANSFER_STATUS_IN_PROGRESS)
                .where(AUCTIONS.TOKENID.eq(tokenId))
                .execute();
        cx.close();
    }

    public void setTransferTransactionByTokenId(String tokenId, String transactionId, String transactionHash) throws SQLException {
        DSLContext cx = connectionManager.dsl();
        log.debug("setTransferTransactionByTokenId " + tokenId + ", transactionId " + transactionId + ", hash " + transactionHash);

        cx.update(AUCTIONS)
                .set(AUCTIONS.TRANSFERTXID, transactionId)
                .set(AUCTIONS.TRANSFERTXHASH, transactionHash)
                .set(AUCTIONS.TRANSFERSTATUS, Auction.TRANSFER_STATUS_COMPLETE)
                .set(AUCTIONS.STATUS, Auction.ENDED)
                .where(AUCTIONS.TOKENID.eq(tokenId))
                .execute();
        cx.close();
    }

    public void setTransferTransactionByAuctionId(int auctionId, String transactionId, String transactionHash) throws SQLException {
        DSLContext cx = connectionManager.dsl();

        log.debug("setTransferTransactionByAuctionId " + auctionId + ", transactionId " + transactionId + ", hash " + transactionHash);
        cx.update(AUCTIONS)
                .set(AUCTIONS.TRANSFERTXID, transactionId)
                .set(AUCTIONS.TRANSFERTXHASH, transactionHash)
                .set(AUCTIONS.TRANSFERSTATUS, Auction.TRANSFER_STATUS_COMPLETE)
                .set(AUCTIONS.STATUS, Auction.ENDED)
                .where(AUCTIONS.ID.eq(auctionId))
                .execute();
        cx.close();
    }

    public void setEnded(int auctionId) throws SQLException {
        DSLContext cx = connectionManager.dsl();

        cx.update(AUCTIONS)
                .set(AUCTIONS.STATUS, Auction.ENDED)
                .where(AUCTIONS.ID.eq(auctionId))
                .execute();
        cx.close();
    }

    public void setClosed(int auctionId) throws SQLException {
        DSLContext cx = connectionManager.dsl();
        cx.update(AUCTIONS)
                .set(AUCTIONS.STATUS, Auction.CLOSED)
                .where(AUCTIONS.ID.eq(auctionId))
                .execute();
        cx.close();
    }

    public Auction setClosed(Auction auction) throws SQLException {
        setClosed(auction.getId());

        updateStatus(auction.getAuctionaccountid(), Auction.CLOSED);
        auction.setStatus(Auction.CLOSED);
        return auction;
    }

    private void updateStatus(String auctionAccountId, String newStatus) throws SQLException {
        DSLContext cx = connectionManager.dsl();

        cx.update(AUCTIONS)
                .set(AUCTIONS.STATUS, newStatus)
                .where(AUCTIONS.AUCTIONACCOUNTID.eq(auctionAccountId))
                .execute();
        cx.close();
    }

    public void save(Auction auction) throws SQLException {
        DSLContext cx = connectionManager.dsl();
        cx.transaction( transaction -> {
            save(auction, transaction);
        });
        cx.close();
    }

    public void save(Auction auction, Configuration configuration) {
        configuration.dsl().update(AUCTIONS)
                .set(AUCTIONS.LASTCONSENSUSTIMESTAMP, auction.getLastconsensustimestamp())
                .set(AUCTIONS.WINNINGACCOUNT, auction.getWinningaccount())
                .set(AUCTIONS.WINNINGBID, auction.getWinningbid())
                .set(AUCTIONS.WINNINGTIMESTAMP, auction.getWinningtimestamp())
                .set(AUCTIONS.WINNINGTXID, auction.getWinningtxid())
                .set(AUCTIONS.WINNINGTXHASH, auction.getWinningtxhash())
                .where(AUCTIONS.AUCTIONACCOUNTID.eq(auction.getAuctionaccountid()))
                .execute();
    }

    public Auction add(Auction auction) throws SQLException {
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
                    AUCTIONS.TITLE,
                    AUCTIONS.DESCRIPTION
            ).values(auction.getTokenid(),
                    auction.getAuctionaccountid(),
                    auction.getEndtimestamp(),
                    auction.getReserve(),
                    "0.0",
                    auction.getWinnerCanBid(),
                    auction.getTokenimage(),
                    auction.getWinningbid(),
                    auction.getMinimumbid(),
                    auction.getTitle(),
                    auction.getDescription()
            ).returning(AUCTIONS.ID).execute();
            int id = cx.lastID().intValue();
            auction.setId(id);
        } catch (DataAccessException e) {
            log.info("Auction already in database");
            auction.setId(0);
        } finally {
            if (cx != null) {
                cx.close();
            }
        }
        return auction;
    }

    public Map<String, Integer> openAndPendingAuctions() throws SQLException {
        DSLContext cx = connectionManager.dsl();
        Map<String, Integer> rows = cx.select(AUCTIONS.ID, AUCTIONS.ENDTIMESTAMP)
                .from(AUCTIONS)
                .where(AUCTIONS.STATUS.eq(Auction.ACTIVE))
                .or(AUCTIONS.STATUS.eq(Auction.PENDING))
                .fetchMap(AUCTIONS.ENDTIMESTAMP, AUCTIONS.ID);
        cx.close();
        return rows;
    }

    // for testing
    public Auction createComplete(Auction auction) throws SQLException{
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
                    AUCTIONS.TRANSFERTXHASH,
                    AUCTIONS.TOKENOWNER,
                    AUCTIONS.TITLE,
                    AUCTIONS.DESCRIPTION,
                    AUCTIONS.TRANSFERSTATUS
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
                    auction.getTransfertxhash(),
                    auction.getTokenowneraccount(),
                    auction.getTitle(),
                    auction.getDescription(),
                    auction.getTransferstatus()
            ).returning(AUCTIONS.ID).execute();
            int id = cx.lastID().intValue();
            auction.setId(id);
        } catch (DataAccessException e) {
            log.info("Auction already in database");
        } finally {
            if (cx != null) {
                cx.close();
            }
        }
        return auction;
    }
}
