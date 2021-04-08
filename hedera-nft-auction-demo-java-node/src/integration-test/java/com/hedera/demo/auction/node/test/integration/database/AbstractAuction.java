package com.hedera.demo.auction.node.test.integration.database;

import com.hedera.demo.auction.node.app.domain.Auction;

import static org.junit.jupiter.api.Assertions.assertEquals;

public abstract class AbstractAuction extends AbstractDatabaseTest {
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

    boolean winnercanbid = true;

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
        auction.setWinnercanbid(winnercanbid);
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
        assertEquals(winnercanbid, getAuction.getWinnerCanBid());
        assertEquals(auction.getTokenimage(),getAuction.getTokenimage());
        assertEquals(auction.getWinningbid(),getAuction.getWinningbid());
        assertEquals(auction.getMinimumbid(),getAuction.getMinimumbid());
    }
}
