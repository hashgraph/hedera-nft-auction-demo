package com.hedera.demo.auction.app.repository;

import com.google.errorprone.annotations.Var;
import com.hedera.demo.auction.app.SqlConnectionManager;
import com.hedera.demo.auction.app.domain.Auction;
import com.hedera.demo.auction.app.domain.Bid;
import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.NotNull;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Record1;
import org.jooq.Record2;
import org.jooq.Result;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;

import javax.annotation.Nullable;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.hedera.demo.auction.app.db.Tables.AUCTIONS;
import static com.hedera.demo.auction.app.db.Tables.BIDS;

/**
 * Repository to manage auctions in the database
 */
@Log4j2
public class AuctionsRepository {
    private final SqlConnectionManager connectionManager;
    private final Condition activeOrClosedStatusCondition = AUCTIONS.STATUS.eq(Auction.ACTIVE).or(AUCTIONS.STATUS.eq(Auction.CLOSED));
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
     * @return {@code Result<Record>} records of auctions
     * @throws SQLException in the event of an error
     */
    private Result<Record> getAuctions() throws SQLException {
        DSLContext cx = connectionManager.dsl();
        return cx.selectFrom(AUCTIONS).orderBy(AUCTIONS.ID).fetch();
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
        return cx.selectFrom(AUCTIONS)
                .where(AUCTIONS.ID.eq(auctionId))
                .fetchAny();
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
     * @return {@code List<Auction>} list of Auction objects
     * @throws SQLException in the event of an error
     */
    public List<Auction> getAuctionsList() throws SQLException {
        List<Auction> auctions = new ArrayList<>();
        Result<Record> auctionsData = getAuctions();
        for (Record record : auctionsData) {
            Auction auction = new Auction(record);
            auctions.add(auction);
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
     * Gets auctions below reserve
     *
     * @return List of Auction below reserve
     * @throws SQLException in the event of an error
     */
    @Nullable
    public List<Auction> getAuctionsBelowReserve() throws SQLException {
        DSLContext cx = connectionManager.dsl();
        // get auctions with no winning bid
        Result<Record> auctionRecords = cx.selectFrom(AUCTIONS)
                .where(AUCTIONS.WINNINGBID.eq(0L))
                .orderBy(AUCTIONS.ID)
                .fetch();

        List<Auction> auctions = new ArrayList<>();
        for (Record record : auctionRecords) {
            // look for bids against the auction that are below reserve
            int count = cx.select(DSL.count())
                    .from(BIDS)
                    .where(BIDS.AUCTIONID.eq(record.get(AUCTIONS.ID)))
                    .and(BIDS.STATUS.eq("Bid below reserve"))
                    .fetchOne(0, int.class);

            if (count > 0) {
                Auction auction = new Auction(record);
                auctions.add(auction);
            }
        }
        return auctions;
    }

    /**
     * Gets ENDED auctions with a bid above reserve
     *
     * @return List of ENDED Auction with a bid above reserve and winning bid > 0
     * @throws SQLException in the event of an error
     */
    @Nullable
    public List<Auction> getAuctionsSold() throws SQLException {
        DSLContext cx = connectionManager.dsl();
        Result<Record> auctionRecords = cx.selectFrom(AUCTIONS)
                .where(AUCTIONS.WINNINGBID.ge(AUCTIONS.RESERVE))
                .and(AUCTIONS.WINNINGBID.ne(0L))
                .and(AUCTIONS.STATUS.eq(Auction.ENDED))
                .orderBy(AUCTIONS.ID)
                .fetch();

        List<Auction> auctions = new ArrayList<>();
        for (Record record : auctionRecords) {
            Auction auction = new Auction(record);
            auctions.add(auction);
        }
        return auctions;
    }

    /**
     * Gets auctions for a given status
     *
     * @param status the status to search on
     * @return List of Auction that match the provided status
     * @throws SQLException in the event of an error
     */
    @Nullable
    public List<Auction> getByStatus(String status) throws SQLException {
        DSLContext cx = connectionManager.dsl();
        Result<Record> auctionRecords = cx.selectFrom(AUCTIONS)
                .where(AUCTIONS.STATUS.eq(status))
                .orderBy(AUCTIONS.ID)
                .fetch();

        List<Auction> auctions = new ArrayList<>();
        for (Record record : auctionRecords) {
            Auction auction = new Auction(record);
            auctions.add(auction);
        }
        return auctions;
    }

    /**
     * Sets an auction's status to ACTIVE if it's PENDING and
     * updates the tokenOwner and startTimestamp
     *
     * @param auction the Auction object being updated
     * @param tokenOwnerAccount the token owner's account id
     * @param timestamp the start time stamp of the auction
     * @throws Exception in the event of an error
     */
    public void setActive(Auction auction, String tokenOwnerAccount, String timestamp) throws Exception {
        DSLContext cx = connectionManager.dsl();

        int rows = cx.update(AUCTIONS)
                .set(AUCTIONS.STATUS, Auction.ACTIVE)
                .set(AUCTIONS.STARTTIMESTAMP, timestamp)
                .set(AUCTIONS.TOKENOWNER, tokenOwnerAccount)
                .where(AUCTIONS.AUCTIONACCOUNTID.eq(auction.getAuctionaccountid()))
                .and(AUCTIONS.STATUS.eq(Auction.PENDING))
                .execute();

        if (rows == 0) {
            String message = "auction cannot be set to ACTIVE, it's not PENDING";
            log.error(message);
            throw new Exception(message);
        }
    }

    /**
     * Sets an auction's transfer status to TRANSFER_PENDING
     *
     * @param tokenId the token id of the auction to update
     * @throws Exception in the event of an error
     */
    public void setTransferPending(String tokenId) throws Exception {
        DSLContext cx = connectionManager.dsl();
        log.debug("Setting auction transfer status to {} for token id {}", Auction.TRANSFER_STATUS_PENDING, tokenId);
        int rows = cx.update(AUCTIONS)
                .set(AUCTIONS.TRANSFERSTATUS, Auction.TRANSFER_STATUS_PENDING)
                .where(AUCTIONS.TOKENID.eq(tokenId))
                .and(AUCTIONS.TRANSFERSTATUS.eq(""))
                .execute();
        if (rows == 0) {
            String message = "cannot overwrite non empty auction transferStatus to PENDING";
            log.error(message);
            throw new Exception(message);
        }
    }

    /**
     * Sets an auction's transfer status to IN_PROGRESS
     *
     * @param tokenId the token id of the auction to update
     * @throws Exception in the event of an error
     */
    public void setTransferInProgress(String tokenId) throws Exception {
        DSLContext cx = connectionManager.dsl();
        log.debug("setTransferInProgress {} for token id {}", Auction.TRANSFER_STATUS_IN_PROGRESS, tokenId);

        int rows = cx.update(AUCTIONS)
                .set(AUCTIONS.TRANSFERSTATUS, Auction.TRANSFER_STATUS_IN_PROGRESS)
                .where(AUCTIONS.TOKENID.eq(tokenId))
                .and(AUCTIONS.TRANSFERSTATUS.eq(Auction.TRANSFER_STATUS_PENDING))
                .execute();

        if (rows == 0) {
            String message = "auction transfer status not PENDING";
            log.error(message);
            throw new Exception(message);
        }
    }

    /**
     * Sets the auction's transfer transaction id and hash given a token id
     *
     * @param tokenId the token id of the auction to update
     * @param transactionId the transaction id
     * @param transactionHash the transaction hash
     * @throws Exception in the event of an error
     */
    public void setTransferTransactionByTokenId(String tokenId, String transactionId, String transactionHash) throws Exception {
        DSLContext cx = connectionManager.dsl();
        log.debug("setTransferTransactionByTokenId {}, transactionId {}, hash {}", tokenId, transactionId, transactionHash);

        int rows = cx.update(AUCTIONS)
                .set(AUCTIONS.TRANSFERTXID, transactionId)
                .set(AUCTIONS.TRANSFERTXHASH, transactionHash)
                .set(AUCTIONS.TRANSFERSTATUS, Auction.TRANSFER_STATUS_COMPLETE)
                .set(AUCTIONS.STATUS, Auction.ENDED)
                .where(AUCTIONS.TOKENID.eq(tokenId))
                .and(AUCTIONS.TRANSFERSTATUS.eq(Auction.TRANSFER_STATUS_IN_PROGRESS))
                .execute();
        if (rows == 0) {
            String message = "unable to set transfer status to COMPLETE, it's not IN PROGRESS";
            log.error(message);
            throw new Exception(message);
        }
    }

    /**
     * Sets the auction's transfer transaction id and hash given an auction id if the auction is closed
     *
     * @param auctionId the auction id to update
     * @param transactionId the transaction id
     * @param transactionHash the transaction hash
     * @throws Exception in the event of an error
     */
    public void setTransferTransactionByAuctionId(int auctionId, String transactionId, String transactionHash) throws Exception {
        DSLContext cx = connectionManager.dsl();

        log.debug("setTransferTransactionByAuctionId {}, transactionId {}, hash {}",auctionId, transactionId, transactionHash);
        int rows = cx.update(AUCTIONS)
                .set(AUCTIONS.TRANSFERTXID, transactionId)
                .set(AUCTIONS.TRANSFERTXHASH, transactionHash)
                .set(AUCTIONS.TRANSFERSTATUS, Auction.TRANSFER_STATUS_COMPLETE)
                .set(AUCTIONS.STATUS, Auction.ENDED)
                .where(AUCTIONS.ID.eq(auctionId))
                .and(AUCTIONS.STATUS.eq(Auction.CLOSED))
                .execute();
        if (rows == 0) {
            String message = "auction is not CLOSED, cannot set transfer transaction";
            throw new Exception(message);
        }
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
            .and(activeOrClosedStatusCondition)
            .execute();
    }

    /**
     * Sets the auction's status to CLOSED given an auction id
     *
     * @param auctionId the auction id to update
     * @return boolean if the update was successful
     * @throws Exception in the event of an error
     */
    public boolean setClosed(int auctionId) throws Exception {
        DSLContext cx = connectionManager.dsl();
        int rows = cx.update(AUCTIONS)
                .set(AUCTIONS.STATUS, Auction.CLOSED)
                .where(AUCTIONS.ID.eq(auctionId))
                .and(AUCTIONS.STATUS.eq(Auction.ACTIVE))
                .execute();
        if (rows == 0) {
            String message = "unable to close inactive (not ACTIVE) auction";
            log.error(message);
            throw new Exception(message);
        }
        return true;
    }

    /**
     * Sets the auction's status to CLOSED given an auction object
     *
     * @param auction the auction object to update
     * @return Auction object
     * @throws SQLException in the event of an error
     */
    public Auction setClosed(Auction auction) throws Exception {
        setClosed(auction.getId());
        auction.setStatus(Auction.CLOSED);
        return auction;
    }

    /**
     * Updates the last consensus timestamp of an auction
     *
     * @param auctionId the id of the auction to update
     * @param lastConsensusTimestamp the consensus timestamp
     * @throws SQLException in the event of an error
     */
    public void setLastConsensusTimestamp(int auctionId, String lastConsensusTimestamp) throws SQLException {
        DSLContext cx = connectionManager.dsl();
        cx.update(AUCTIONS)
                .set(AUCTIONS.LASTCONSENSUSTIMESTAMP, lastConsensusTimestamp)
                .where(AUCTIONS.ID.eq(auctionId))
                .execute();
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
        try {
            DSLContext cx = connectionManager.dsl();
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
                    AUCTIONS.PROCESSREFUNDS,
                    AUCTIONS.CREATEAUCTIONTXID
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
                    auction.getProcessrefunds(),
                    auction.getCreateauctiontxid()
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
     * @return {@code Map<String, Integer>} hashmap of auctionId and EndTimeStamp
     * @throws SQLException in the event of an error
     */
    public Map<String, Integer> openAndPendingAuctions() throws SQLException {
        DSLContext cx = connectionManager.dsl();
        Map<String, Integer> result = new HashMap<>();

        @NotNull Result<Record2<String, Integer>> rows = cx.select(AUCTIONS.ENDTIMESTAMP, AUCTIONS.ID)
                .from(AUCTIONS)
                .where(AUCTIONS.STATUS.eq(Auction.ACTIVE))
                .or(AUCTIONS.STATUS.eq(Auction.PENDING))
                .fetch();

        for (Record2<String, Integer> row : rows) {
            result.put(row.value1(), row.value2());
        }

        return result;
    }

    /**
     * Creates a complete auction object in the database for the purpose of testing
     *
     * @param auction the auction object to add to the database
     * @return Auction object including the newly created auction's unique identifier
     * @throws SQLException in the event of an error
     */
    public Auction createComplete(Auction auction) throws SQLException{
        try {
            DSLContext cx = connectionManager.dsl();
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
                    AUCTIONS.PROCESSREFUNDS,
                    AUCTIONS.CREATEAUCTIONTXID
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
                    auction.getProcessrefunds(),
                    auction.getCreateauctiontxid()
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

        @NotNull
        Result<Record1<String>> rows = cx.select(BIDS.TIMESTAMP).from(BIDS).where(BIDS.TIMESTAMP.eq(priorBid.getTimestamp())).and(BIDS.REFUNDSTATUS.ne("")).fetch();

        @Var boolean shouldUpdate = true;
        if ((rows != null) && (rows.size() != 0)) {
            // this bid already exists in the database and its refund status is already set
            shouldUpdate = false;
        }

        boolean finalShouldUpdatePriorBid = updatePriorBid && shouldUpdate;
        cx.transaction(configuration -> {
            if (finalShouldUpdatePriorBid) {
                int updatedRows = DSL.using(configuration).update(BIDS)
                        .set(BIDS.STATUS, priorBid.getStatus())
                        .set(BIDS.REFUNDSTATUS, priorBid.getRefundstatus())
                        .where(BIDS.TIMESTAMP.eq(priorBid.getTimestamp()))
                        .and(BIDS.REFUNDSTATUS.eq("")) // don't overwrite refund status if already set
                        .execute();
                log.debug("Updated {} bids", updatedRows);

            }
            DSL.using(configuration).update(AUCTIONS)
                    .set(AUCTIONS.LASTCONSENSUSTIMESTAMP, auction.getLastconsensustimestamp())
                    .set(AUCTIONS.WINNINGACCOUNT, auction.getWinningaccount())
                    .set(AUCTIONS.WINNINGBID, auction.getWinningbid())
                    .set(AUCTIONS.WINNINGTIMESTAMP, auction.getWinningtimestamp())
                    .set(AUCTIONS.WINNINGTXID, auction.getWinningtxid())
                    .set(AUCTIONS.WINNINGTXHASH, auction.getWinningtxhash())
                    .where(AUCTIONS.AUCTIONACCOUNTID.eq(auction.getAuctionaccountid()))
                    .execute();

            if (bidAmount > 0) {
                // does the bid already exist ?
                @NotNull
                Result<Record1<String>> bid = DSL.using(configuration).select(BIDS.TIMESTAMP).from(BIDS).where(BIDS.TIMESTAMP.eq(newBid.getTimestamp())).fetch();

                if ((bid == null) || (bid.size() == 0)) {
                    // this bid is definitely new, add it
                    DSL.using(configuration).insertInto(BIDS,
                            BIDS.AUCTIONID,
                            BIDS.STATUS,
                            BIDS.TIMESTAMP,
                            BIDS.BIDAMOUNT,
                            BIDS.BIDDERACCOUNTID,
                            BIDS.TRANSACTIONID,
                            BIDS.TRANSACTIONHASH,
                            BIDS.REFUNDSTATUS
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
                } else {
                    log.debug("Bid already in database");
                }
            }
        });
    }
}
