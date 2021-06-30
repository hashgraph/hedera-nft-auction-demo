package com.hedera.demo.auction.app.repository;

import com.google.errorprone.annotations.Var;
import com.hedera.demo.auction.app.SqlConnectionManager;
import com.hedera.demo.auction.app.db.Tables;
import com.hedera.demo.auction.app.domain.Auction;
import com.hedera.demo.auction.app.domain.Bid;
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

import static com.hedera.demo.auction.app.db.Tables.AUCTIONS;

/**
 * Repository to manage auctions in the database
 */
@Log4j2
public class AuctionsRepository {
    private final SqlConnectionManager connectionManager;

    /**
     * Constructor
     * @param connectionManager connection manager to the database
     */
    public AuctionsRepository(SqlConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    /**
     * Returns all auctions ordered by auction id
     *
     * @return Result<Record> records of auctions
     * @throws SQLException in the event of an error
     */
    @Nullable
    private Result<Record> getAuctions () throws SQLException {
        DSLContext cx = connectionManager.dsl();
        Result<Record> rows = cx.selectFrom(AUCTIONS).orderBy(AUCTIONS.ID).fetch();

        return rows;
    }

    /**
     * Returns a complete auction given an auction id
     *
     * @param auctionId the id of the auction
     * @return Record containing the auction
     * @throws SQLException in the event of an error
     */
    @Nullable
    private Record getAuctionData (int auctionId) throws SQLException {
        DSLContext cx = connectionManager.dsl();
        Record auctionRecord = cx.selectFrom(AUCTIONS)
                .where(AUCTIONS.ID.eq(auctionId))
                .fetchAny();

        return auctionRecord;
    }

    /**
     * For testing purposes, enables the end timestamp of the auction to be set for an auction id
     *
     * @param auctionId the auction id to set the end timestamp for
     * @throws SQLException in the event of an error
     */
    public void setEndTimestampForTesting(int auctionId) throws SQLException {
        // get last consensus timestamp from auction and use to set end timestamp

        DSLContext cx = connectionManager.dsl();
        cx.update(AUCTIONS)
                .set(AUCTIONS.ENDTIMESTAMP, AUCTIONS.LASTCONSENSUSTIMESTAMP)
                .where(AUCTIONS.ID.eq(auctionId))
                .execute();
    }

    /**
     * Deletes all the auctions from the database
     *
     * @throws SQLException in the event of an error
     */
    public void deleteAllAuctions() throws SQLException {
        DSLContext cx = connectionManager.dsl();
        cx.deleteFrom(AUCTIONS)
                .execute();
    }

    /**
     * Gets all the auctions in a List
     *
     * @return List<Auction> list of Auction objects
     * @throws SQLException in the event of an error
     */
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

    /**
     * Gets an Auction for a given auction id
     *
     * @param auctionId the id of the auction to get
     * @return Auction object matching the auction id
     * @throws Exception in the event of an error
     */
    public Auction getAuction(int auctionId) throws Exception {
        Record auctionData = getAuctionData(auctionId);

        if (auctionData != null) {
            return new Auction(auctionData);
        } else {
            throw new Exception("No auction id " + auctionId);
        }
    }

    /**
     * Gets an auction given an auction account id as a string
     *
     * @param accountId the account id in string format
     * @return Auction object matching the account id
     * @throws SQLException in the event of an error
     */
    @Nullable
    public Auction getAuction(String accountId) throws SQLException {
        DSLContext cx = connectionManager.dsl();
        Record auctionData = cx.selectFrom(AUCTIONS).where(AUCTIONS.AUCTIONACCOUNTID.eq(accountId)).fetchAny();

        if ((auctionData == null) || (auctionData.size() == 0)) {
            return null;
        } else {
            return new Auction(auctionData);
        }
    }

    /**
     * Sets an auction's status to ACTIVE, updates the tokenOwner and startTimestamp
     *
     * @param auction the Auction object being updated
     * @param tokenOwnerAccount the token owner's account id
     * @param timestamp the start time stamp of the auction
     * @return an updated Auction object
     * @throws SQLException in the event of an error
     */
    public Auction setActive(Auction auction, String tokenOwnerAccount, String timestamp) throws SQLException {
        DSLContext cx = connectionManager.dsl();

        cx.update(AUCTIONS)
                .set(AUCTIONS.STATUS, Auction.ACTIVE)
                .set(AUCTIONS.STARTTIMESTAMP, timestamp)
                .set(AUCTIONS.TOKENOWNER, tokenOwnerAccount)
                .where(AUCTIONS.AUCTIONACCOUNTID.eq(auction.getAuctionaccountid()))
                .execute();

        auction.setStatus(Auction.ACTIVE);
        auction.setStarttimestamp(timestamp);
        auction.setTokenowneraccount(tokenOwnerAccount);
        return auction;
    }

    /**
     * Sets an auction's transfer status to TRANSFER_PENDING
     *
     * @param tokenId the token id of the auction to update
     * @throws SQLException in the event of an error
     */
    public void setTransferPending(String tokenId) throws SQLException {
        DSLContext cx = connectionManager.dsl();
        log.debug("Setting auction transfer status to {} for token id {}", Auction.TRANSFER_STATUS_PENDING, tokenId);
        cx.update(AUCTIONS)
                .set(AUCTIONS.TRANSFERSTATUS, Auction.TRANSFER_STATUS_PENDING)
                .where(AUCTIONS.TOKENID.eq(tokenId))
                .execute();
    }

    /**
     * Sets an auction's transfer status to IN_PROGRESS
     *
     * @param tokenId the token id of the auction to update
     * @throws SQLException in the event of an error
     */
    public void setTransferInProgress(String tokenId) throws SQLException {
        DSLContext cx = connectionManager.dsl();
        log.debug("setTransferInProgress {} for token id {}", Auction.TRANSFER_STATUS_IN_PROGRESS, tokenId);

        cx.update(AUCTIONS)
                .set(AUCTIONS.TRANSFERSTATUS, Auction.TRANSFER_STATUS_IN_PROGRESS)
                .where(AUCTIONS.TOKENID.eq(tokenId))
                .execute();
    }

    /**
     * Sets the auction's transfer transaction id and hash given a token id
     *
     * @param tokenId the token id of the auction to update
     * @param transactionId the transaction id
     * @param transactionHash the transaction hash
     * @throws SQLException in the event of an error
     */
    public void setTransferTransactionByTokenId(String tokenId, String transactionId, String transactionHash) throws SQLException {
        DSLContext cx = connectionManager.dsl();
        log.debug("setTransferTransactionByTokenId {}, transactionId {}, hash {}", tokenId, transactionId, transactionHash);

        cx.update(AUCTIONS)
                .set(AUCTIONS.TRANSFERTXID, transactionId)
                .set(AUCTIONS.TRANSFERTXHASH, transactionHash)
                .set(AUCTIONS.TRANSFERSTATUS, Auction.TRANSFER_STATUS_COMPLETE)
                .set(AUCTIONS.STATUS, Auction.ENDED)
                .where(AUCTIONS.TOKENID.eq(tokenId))
                .execute();
    }

    /**
     * Sets the auction's transfer transaction id and hash given an auction id
     *
     * @param auctionId the auction id to update
     * @param transactionId the transaction id
     * @param transactionHash the transaction hash
     * @throws SQLException in the event of an error
     */
    public void setTransferTransactionByAuctionId(int auctionId, String transactionId, String transactionHash) throws SQLException {
        DSLContext cx = connectionManager.dsl();

        log.debug("setTransferTransactionByAuctionId {}, transactionId {}, hash {}",auctionId, transactionId, transactionHash);
        cx.update(AUCTIONS)
                .set(AUCTIONS.TRANSFERTXID, transactionId)
                .set(AUCTIONS.TRANSFERTXHASH, transactionHash)
                .set(AUCTIONS.TRANSFERSTATUS, Auction.TRANSFER_STATUS_COMPLETE)
                .set(AUCTIONS.STATUS, Auction.ENDED)
                .where(AUCTIONS.ID.eq(auctionId))
                .execute();
    }

    /**
     * Sets the auction's status to ENDED
     *
     * @param auctionId the auction id to update
     * @throws SQLException in the event of an error
     */
    public void setEnded(int auctionId) throws SQLException {
        DSLContext cx = connectionManager.dsl();

        cx.update(AUCTIONS)
                .set(AUCTIONS.STATUS, Auction.ENDED)
                .where(AUCTIONS.ID.eq(auctionId))
                .execute();
    }

    /**
     * Sets the auction's status to CLOSED given an auction id
     *
     * @param auctionId the auction id to update
     * @throws SQLException in the event of an error
     */
    public void setClosed(int auctionId) throws SQLException {
        DSLContext cx = connectionManager.dsl();
        cx.update(AUCTIONS)
                .set(AUCTIONS.STATUS, Auction.CLOSED)
                .where(AUCTIONS.ID.eq(auctionId))
                .execute();
    }

    /**
     * Sets the auction's status to CLOSED given an auction object
     *
     * @param auction the auction object to update
     * @return Auction object
     * @throws SQLException in the event of an error
     */
    public Auction setClosed(Auction auction) throws SQLException {
        setClosed(auction.getId());
        auction.setStatus(Auction.CLOSED);
        return auction;
    }

    /**
     * Partially updates an auction object in the database
     *
     * @param auction the auction object
     * @throws SQLException in the event of an error
     */
    public void save(Auction auction) throws SQLException {
        DSLContext cx = connectionManager.dsl();
        cx.update(AUCTIONS)
                .set(AUCTIONS.LASTCONSENSUSTIMESTAMP, auction.getLastconsensustimestamp())
                .set(AUCTIONS.WINNINGACCOUNT, auction.getWinningaccount())
                .set(AUCTIONS.WINNINGBID, auction.getWinningbid())
                .set(AUCTIONS.WINNINGTIMESTAMP, auction.getWinningtimestamp())
                .set(AUCTIONS.WINNINGTXID, auction.getWinningtxid())
                .set(AUCTIONS.WINNINGTXHASH, auction.getWinningtxhash())
                .where(AUCTIONS.AUCTIONACCOUNTID.eq(auction.getAuctionaccountid()))
                .execute();
    }

    /**
     * Adds a partial auction to the database
     *
     * @param auction the auction object to add to the database
     * @return Auction object including the newly created auction's unique identifier
     * @throws SQLException in the event of an error
     */
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
                    AUCTIONS.TOKENMETADATA,
                    AUCTIONS.WINNINGBID,
                    AUCTIONS.MINIMUMBID,
                    AUCTIONS.TITLE,
                    AUCTIONS.DESCRIPTION,
                    AUCTIONS.PROCESSREFUNDS
            ).values(auction.getTokenid(),
                    auction.getAuctionaccountid(),
                    auction.getEndtimestamp(),
                    auction.getReserve(),
                    "0.0",
                    auction.getWinnerCanBid(),
                    auction.getTokenmetadata(),
                    auction.getWinningbid(),
                    auction.getMinimumbid(),
                    auction.getTitle(),
                    auction.getDescription(),
                    auction.getProcessrefunds()
            ).returning(AUCTIONS.ID).execute();
            int id = cx.lastID().intValue();
            auction.setId(id);
        } catch (DataAccessException e) {
            log.info("Auction already in database");
            auction.setId(0);
        }
        return auction;
    }

    /**
     * Gets all open and pending auctions from the database
     *
     * @return Map<String, Integer> hashmap of auctionId and EndTimeStamp
     * @throws SQLException in the event of an error
     */
    public Map<String, Integer> openAndPendingAuctions() throws SQLException {
        DSLContext cx = connectionManager.dsl();
        Map<String, Integer> rows = cx.select(AUCTIONS.ID, AUCTIONS.ENDTIMESTAMP)
                .from(AUCTIONS)
                .where(AUCTIONS.STATUS.eq(Auction.ACTIVE))
                .or(AUCTIONS.STATUS.eq(Auction.PENDING))
                .fetchMap(AUCTIONS.ENDTIMESTAMP, AUCTIONS.ID);

        return rows;
    }

    /**
     * Creates a complete auction object in the database for the purpose of testing
     *
     * @param auction the auction object to add to the database
     * @return Auction object including the newly created auction's unique identifier
     * @throws SQLException in the event of an error
     */
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
                    AUCTIONS.TOKENMETADATA,
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
                    AUCTIONS.TRANSFERSTATUS,
                    AUCTIONS.PROCESSREFUNDS
            ).values(auction.getTokenid(),
                    auction.getAuctionaccountid(),
                    auction.getEndtimestamp(),
                    auction.getReserve(),
                    auction.getLastconsensustimestamp(),
                    auction.getWinnerCanBid(),
                    auction.getTokenmetadata(),
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
                    auction.getTransferstatus(),
                    auction.getProcessrefunds()
            ).returning(AUCTIONS.ID).execute();
            int id = cx.lastID().intValue();
            auction.setId(id);
        } catch (DataAccessException e) {
            log.debug("Auction already in database");
        }
        return auction;
    }

    /**
     * Updates an auction and one or two bids depending on supplied parameters
     *
     * @param updatePriorBid true if the prior bid should be updated in the database
     * @param bidAmount the bid amount
     * @param priorBid the prior bid object
     * @param auction the auction to update
     * @param newBid the new bid object
     * @throws SQLException in the event of an error
     */
    public void commitBidAndAuction(boolean updatePriorBid, long bidAmount, Bid priorBid, Auction auction, Bid newBid) throws SQLException {
        DSLContext cx = connectionManager.dsl();

        //TODO: This should be transactional
        if (updatePriorBid) {
            int rows = cx.update(Tables.BIDS)
                    .set(Tables.BIDS.STATUS, priorBid.getStatus())
                    .set(Tables.BIDS.REFUNDSTATUS, priorBid.getRefundstatus())
                    .where(Tables.BIDS.TIMESTAMP.eq(priorBid.getTimestamp()))
                    .and(Tables.BIDS.REFUNDSTATUS.eq("")) // don't overwrite refund status if already set
                    .execute();
            log.debug("Updated {} bids", rows);

            cx.update(AUCTIONS)
                .set(AUCTIONS.LASTCONSENSUSTIMESTAMP, auction.getLastconsensustimestamp())
                .set(AUCTIONS.WINNINGACCOUNT, auction.getWinningaccount())
                .set(AUCTIONS.WINNINGBID, auction.getWinningbid())
                .set(AUCTIONS.WINNINGTIMESTAMP, auction.getWinningtimestamp())
                .set(AUCTIONS.WINNINGTXID, auction.getWinningtxid())
                .set(AUCTIONS.WINNINGTXHASH, auction.getWinningtxhash())
                .where(AUCTIONS.AUCTIONACCOUNTID.eq(auction.getAuctionaccountid()))
                .execute();
        }
        if (bidAmount> 0) {
            try {
                cx.insertInto(Tables.BIDS,
                        Tables.BIDS.AUCTIONID,
                        Tables.BIDS.STATUS,
                        Tables.BIDS.TIMESTAMP,
                        Tables.BIDS.BIDAMOUNT,
                        Tables.BIDS.BIDDERACCOUNTID,
                        Tables.BIDS.TRANSACTIONID,
                        Tables.BIDS.TRANSACTIONHASH,
                        Tables.BIDS.REFUNDSTATUS
                ).values(
                        newBid.getAuctionid(),
                        newBid.getStatus(),
                        newBid.getTimestamp(),
                        newBid.getBidamount(),
                        newBid.getBidderaccountid(),
                        newBid.getTransactionid(),
                        newBid.getTransactionhash(),
                        newBid.getRefundstatus()
                ).execute();
            } catch (DataAccessException e) {
                log.debug("Bid already in database");
            }
        }
    }
}
