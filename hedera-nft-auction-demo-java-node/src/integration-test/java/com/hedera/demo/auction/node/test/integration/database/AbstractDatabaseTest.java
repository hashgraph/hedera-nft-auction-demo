package com.hedera.demo.auction.node.test.integration.database;

import com.hedera.demo.auction.node.app.domain.Auction;
import com.hedera.demo.auction.node.app.domain.Bid;
import org.flywaydb.core.Flyway;
import org.testcontainers.containers.PostgreSQLContainer;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AbstractDatabaseTest {

    private int index;

    private String stringPlusIndex(String string) {
        return string.concat(String.valueOf(this.index));
    }

    long winningBid() {
        return 30L + this.index;
    }
    String winningAccount() {
        return stringPlusIndex("winningAccount");
    }
    String winningTimestamp() {
        return stringPlusIndex("winningTimestamp");
    }
    String endtimestamp() {
        return stringPlusIndex("endtimestamp");
    }
    long reserve() {
        return 20L + this.index;
    }
    String status() {
        return stringPlusIndex("endtimestamp");
    }
    boolean winnercanbid() {
        return (this.index == 1);
    }
    String winningtxid() {
        return stringPlusIndex("endtimestamp");
    }
    String winningtxhash() {
        return stringPlusIndex("endtimestamp");
    }
    String tokenimage() {
        return stringPlusIndex("endtimestamp");
    }
    long minimumbid() {
        return 10L + this.index;
    }
    String starttimestamp() {
        return stringPlusIndex("endtimestamp");
    }
    String transfertxid() {
        return stringPlusIndex("endtimestamp");
    }
    String transfertxhash() {
        return stringPlusIndex("endtimestamp");
    }
    String lastConsensusTimestamp() {
        return stringPlusIndex("endtimestamp");
    }
    String auctionAccountId() {
        return stringPlusIndex("endtimestamp");
    }
    String tokenId() {
        return stringPlusIndex("endtimestamp");
    }

    public String timestamp() {
        return stringPlusIndex("timestamp");
    }
    String bidderaccountid() {
        return stringPlusIndex("bidderaccountid");
    }
    Long bidamount() {
        return 30L + this.index;
    }
    boolean refunded() {
        return (this.index == 1);
    }
    String refundtxid() {
        return stringPlusIndex("refundtxid");
    }
    String refundtxhash() {
        return stringPlusIndex("refundtxhash");
    }
    String transactionid() {
        return stringPlusIndex("transactionid");
    }
    String transactionhash() {
        return stringPlusIndex("transactionhash");
    }

    Auction testAuctionObject(int index) {
        this.index = index;
        Auction auction = new Auction();

        auction.setWinningbid(winningBid());
        auction.setWinningaccount(winningAccount());
        auction.setWinningtimestamp(winningTimestamp());
        auction.setTokenid(tokenId());
        auction.setAuctionaccountid(auctionAccountId());
        auction.setEndtimestamp(endtimestamp());
        auction.setReserve(reserve());
        auction.setStatus(status());
        auction.setWinnercanbid(winnercanbid());
        auction.setWinningtxid(winningtxid());
        auction.setWinningtxhash(winningtxhash());
        auction.setTokenimage(tokenimage());
        auction.setMinimumbid(minimumbid());
        auction.setStarttimestamp(starttimestamp());
        auction.setTransfertxid(transfertxid());
        auction.setTransfertxhash(transfertxhash());
        auction.setLastconsensustimestamp(lastConsensusTimestamp());

        return auction;
    }

    public void testNewAuction(Auction auction, Auction getAuction) {
        assertEquals(auction.getTokenid(),getAuction.getTokenid());
        assertEquals("0.0",getAuction.getLastconsensustimestamp());
        assertEquals(auction.getAuctionaccountid(),getAuction.getAuctionaccountid());
        assertEquals(auction.getEndtimestamp(),getAuction.getEndtimestamp());
        assertEquals(auction.getReserve(),getAuction.getReserve());
        assertEquals(winnercanbid(), getAuction.getWinnerCanBid());
        assertEquals(auction.getTokenimage(),getAuction.getTokenimage());
        assertEquals(auction.getWinningbid(),getAuction.getWinningbid());
        assertEquals(auction.getMinimumbid(),getAuction.getMinimumbid());
    }

    Bid testBidObject(int index, int auctionId) {
        this.index = index;
        Bid bid = new Bid();

        bid.setTimestamp(timestamp());
        bid.setAuctionid(auctionId);
        bid.setBidderaccountid(bidderaccountid());
        bid.setBidamount(bidamount());
        bid.setStatus(status());
        bid.setRefunded(refunded());
        bid.setRefundtxid(refundtxid());
        bid.setRefundtxhash(refundtxhash());
        bid.setTransactionid(transactionid());
        bid.setTransactionhash(transactionhash());

        return bid;
    }


    public void testNewBid(Bid bid, Bid newBid) {
        assertEquals(bid.getAuctionid(),newBid.getAuctionid());
        assertEquals(bid.getStatus(),newBid.getStatus());
        assertEquals(bid.getTimestamp(),newBid.getTimestamp());
        assertEquals(bid.getBidamount(),newBid.getBidamount());
        assertEquals(bid.getBidderaccountid(),newBid.getBidderaccountid());
        assertEquals(bid.getTransactionid(),newBid.getTransactionid());
        assertEquals(bid.getTransactionhash(),newBid.getTransactionhash());
    }

    public void verifyBidContents(Bid bid, int auctionId) {
        assertEquals(timestamp(), bid.getTimestamp());
        assertEquals(auctionId, bid.getAuctionid());
        assertEquals(bidderaccountid(), bid.getBidderaccountid());
        assertEquals(bidamount(), bid.getBidamount());
        assertEquals(status(), bid.getStatus());
        assertEquals(refunded(), bid.getRefunded());
        assertEquals(refundtxid(), bid.getRefundtxid());
        assertEquals(refundtxhash(), bid.getRefundtxhash());
        assertEquals(transactionid(), bid.getTransactionid());
        assertEquals(transactionhash(), bid.getTransactionhash());
    }

    void migrate(PostgreSQLContainer postgres) {
        String postgresUrl = postgres.getJdbcUrl();
        String postgresUser = postgres.getUsername();
        String postgresPassword = postgres.getPassword();
        Flyway flyway = Flyway
                .configure()
                .dataSource(postgresUrl, postgresUser, postgresPassword)
                .locations("filesystem:./src/main/resources/migrations")
                .load();
        flyway.migrate();

    }
}
