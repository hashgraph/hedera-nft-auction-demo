package com.hedera.demo.auction.test.unit.domain;

import com.hedera.demo.auction.app.domain.Auction;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AuctionTest extends AbstractAuction {

    @Test
    public void testAuctionSettersGetters() {

        Auction auction = testAuctionObject();
        verifyAuctionContents(auction);
    }

    @Test
    public void testAuctionStatuses() {

        Auction auction = new Auction();
        // active
        auction.setStatus(Auction.ACTIVE);
        assertFalse(auction.isEnded());
        assertFalse(auction.isClosed());
        assertFalse(auction.isPending());
        assertTrue(auction.isActive());
        assertFalse(auction.isTransferPending());

        // closed
        auction.setStatus(Auction.CLOSED);
        assertFalse(auction.isEnded());
        assertTrue(auction.isClosed());
        assertFalse(auction.isPending());
        assertFalse(auction.isActive());
        assertFalse(auction.isTransferPending());

        // ended
        auction.setStatus(Auction.ENDED);
        assertTrue(auction.isEnded());
        assertFalse(auction.isClosed());
        assertFalse(auction.isPending());
        assertFalse(auction.isActive());
        assertFalse(auction.isTransferPending());

        // pending
        auction.setStatus(Auction.PENDING);
        assertFalse(auction.isEnded());
        assertFalse(auction.isClosed());
        assertTrue(auction.isPending());
        assertFalse(auction.isActive());

        // transfer
        auction.setTransferstatus(Auction.TRANSFER_STATUS_PENDING);
        assertTrue(auction.isTransferPending());
        assertFalse(auction.isTransferInProgress());
        assertFalse(auction.isTransferComplete());

        auction.setTransferstatus(Auction.TRANSFER_STATUS_IN_PROGRESS);
        assertFalse(auction.isTransferPending());
        assertTrue(auction.isTransferInProgress());
        assertFalse(auction.isTransferComplete());

        auction.setTransferstatus(Auction.TRANSFER_STATUS_COMPLETE);
        assertFalse(auction.isTransferPending());
        assertFalse(auction.isTransferInProgress());
        assertTrue(auction.isTransferComplete());
    }

    @Test
    public void testAuctionToString() {
        Auction auction = testAuctionObject();
        String auctionString = auction.toString();
        assertTrue(auctionString.contains(String.valueOf(id).concat(", ")));
        assertTrue(auctionString.contains(", ".concat(String.valueOf(winningBid))));
        assertTrue(auctionString.contains(", ".concat(winningAccount)));
        assertTrue(auctionString.contains(", ".concat(winningTimestamp)));
        assertTrue(auctionString.contains(", ".concat(tokenId)));
        assertTrue(auctionString.contains(", ".concat(auctionaccountid)));
        assertTrue(auctionString.contains(", ".concat(endtimestamp)));
        assertTrue(auctionString.contains(", ".concat(String.valueOf(reserve))));
        assertTrue(auctionString.contains(", ".concat(status)));
        assertTrue(auctionString.contains(", true"));
        assertTrue(auctionString.contains(", ".concat(winningtxid)));
        assertTrue(auctionString.contains(", ".concat(winningtxhash)));
        assertTrue(auctionString.contains(", ".concat(tokenmetadata)));
        assertTrue(auctionString.contains(", ".concat(String.valueOf(minimumbid))));
        assertTrue(auctionString.contains(", ".concat(starttimestamp)));
        assertTrue(auctionString.contains(", ".concat(transfertxid)));
        assertTrue(auctionString.contains(", ".concat(transfertxhash)));
        assertTrue(auctionString.contains(", ".concat(lastConsensusTimestamp)));
        assertTrue(auctionString.contains(", ".concat(transferStatus)));
        assertTrue(auctionString.contains(", ".concat(title)));
        assertTrue(auctionString.contains(", ".concat(description)));
    }

    @Test
    public void testAuctionToJson() {
        Auction auction = testAuctionObject();

        JsonObject auctionJson = auction.toJson();

        assertEquals(id, auctionJson.getInteger("id"));
        assertEquals(winningBid, auctionJson.getLong("winningbid"));
        assertEquals(winningAccount, auctionJson.getString("winningaccount"));
        assertEquals(winningTimestamp, auctionJson.getString("winningtimestamp"));
        assertEquals(tokenId, auctionJson.getString("tokenid"));
        assertEquals(auctionaccountid, auctionJson.getString("auctionaccountid"));
        assertEquals(endtimestamp, auctionJson.getString("endtimestamp"));
        assertEquals(reserve, auctionJson.getLong("reserve"));
        assertEquals(status, auctionJson.getString("status"));
        assertEquals(winnercanbid, auctionJson.getBoolean("winnercanbid"));
        assertEquals(winningtxid, auctionJson.getString("winningtxid"));
        assertEquals(winningtxhash, auctionJson.getString("winningtxhash"));
        assertEquals(tokenmetadata, auctionJson.getString("tokenmetadata"));
        assertEquals(minimumbid, auctionJson.getLong("minimumbid"));
        assertEquals(starttimestamp, auctionJson.getString("starttimestamp"));
        assertEquals(transfertxid, auctionJson.getString("transfertxid"));
        assertEquals(transfertxhash, auctionJson.getString("transfertxhash"));
        assertEquals(lastConsensusTimestamp, auctionJson.getString("lastconsensustimestamp"));
        assertEquals(transferStatus, auctionJson.getString("transferstatus"));
        assertEquals(title, auctionJson.getString("title"));
        assertEquals(description, auctionJson.getString("description"));
    }

    @Test
    public void testAuctionFromJson() {
        JsonObject auctionJson = testAuctionObject().toJson();
        Auction auction = new Auction(auctionJson);
        verifyAuctionContents(auction);
    }
}
