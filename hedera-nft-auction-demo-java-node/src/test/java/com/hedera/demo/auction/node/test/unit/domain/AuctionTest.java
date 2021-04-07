package com.hedera.demo.auction.node.test.unit.domain;

import com.hedera.demo.auction.node.app.domain.Auction;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AuctionTest {
    @Test
    public void testAuction() {

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
        String tokenimage= "image";
        long minimumbid = 10L;
        String starttimestamp = "starttimestamp";
        String transfertxid = "transfertxid";
        String transfertxhash = "transfertxhash";

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
        auction.setTokenimage(tokenimage);
        auction.setMinimumbid(minimumbid);
        auction.setStarttimestamp(starttimestamp);
        auction.setTransfertxid(transfertxid);
        auction.setTransfertxhash(transfertxhash);

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
        assertEquals(tokenimage, auction.getTokenimage());
        assertEquals(minimumbid, auction.getMinimumbid());
        assertEquals(starttimestamp, auction.getStarttimestamp());
        assertEquals(transfertxid, auction.getTransfertxid());
        assertEquals(transfertxhash, auction.getTransfertxhash());




    }
}
