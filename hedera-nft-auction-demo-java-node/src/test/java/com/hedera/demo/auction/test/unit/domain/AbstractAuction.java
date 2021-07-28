package com.hedera.demo.auction.test.unit.domain;

import com.hedera.demo.auction.app.domain.Auction;

import static org.junit.jupiter.api.Assertions.assertEquals;

public abstract class AbstractAuction {
    int id = 1;
    long winningBid = 30L;
    String winningAccount = "winningAccount";
    String winningTimestamp = "winstamp";
    String tokenId = "tokenId";
    String auctionaccountid="auctionAccountId";
    String endtimestamp = "end";
    long reserve = 20L;
    String status = "status";
    boolean winnercanbid = true;
    String winningtxid = "winningtxid";
    String winningtxhash = "winningtxhash";
    String tokenmetadata= "tokenmetadata";
    long minimumbid = 10L;
    String starttimestamp = "starttimestamp";
    String transfertxid = "transfertxid";
    String transfertxhash = "transfertxhash";
    String lastConsensusTimestamp = "1.2";
    String transferStatus = "transferstatus";
    String title = "title";
    String description = "description";
    String auctionCreateTransaction = "auctionCreateTransaction";

    Auction testAuctionObject() {
        Auction auction = new Auction();

        auction.setId(id);
        auction.setWinningbid(winningBid);
        auction.setWinningaccount(winningAccount);
        auction.setWinningtimestamp(winningTimestamp);
        auction.setTokenid(tokenId);
        auction.setAuctionaccountid(auctionaccountid);
        auction.setEndtimestamp(endtimestamp);
        auction.setReserve(reserve);
        auction.setStatus(status);
        auction.setWinnercanbid(winnercanbid);
        auction.setWinningtxid(winningtxid);
        auction.setWinningtxhash(winningtxhash);
        auction.setTokenmetadata(tokenmetadata);
        auction.setMinimumbid(minimumbid);
        auction.setStarttimestamp(starttimestamp);
        auction.setTransfertxid(transfertxid);
        auction.setTransfertxhash(transfertxhash);
        auction.setLastconsensustimestamp(lastConsensusTimestamp);
        auction.setTransferstatus(transferStatus);
        auction.setTitle(title);
        auction.setDescription(description);
        auction.setCreateauctiontxid(auctionCreateTransaction);

        return auction;
    }

    public void verifyAuctionContents(Auction auction) {
        assertEquals(winningBid, auction.getWinningbid());
        assertEquals(id, auction.getId());
        assertEquals(winningBid, auction.getWinningbid());
        assertEquals(winningAccount, auction.getWinningaccount());
        assertEquals(winningTimestamp, auction.getWinningtimestamp());
        assertEquals(tokenId, auction.getTokenid());
        assertEquals(auctionaccountid, auction.getAuctionaccountid());
        assertEquals(endtimestamp, auction.getEndtimestamp());
        assertEquals(reserve, auction.getReserve());
        assertEquals(status, auction.getStatus());
        assertEquals(true, auction.getWinnerCanBid());
        assertEquals(winningtxid, auction.getWinningtxid());
        assertEquals(winningtxhash, auction.getWinningtxhash());
        assertEquals(tokenmetadata, auction.getTokenmetadata());
        assertEquals(minimumbid, auction.getMinimumbid());
        assertEquals(starttimestamp, auction.getStarttimestamp());
        assertEquals(transfertxid, auction.getTransfertxid());
        assertEquals(transfertxhash, auction.getTransfertxhash());
        assertEquals(lastConsensusTimestamp, auction.getLastconsensustimestamp());
        assertEquals(transferStatus, auction.getTransferstatus());
        assertEquals(title, auction.getTitle());
        assertEquals(description, auction.getDescription());
        assertEquals(auctionCreateTransaction, auction.getCreateauctiontxid());
    }
}
