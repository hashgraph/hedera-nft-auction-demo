package com.hedera.demo.auction.node.test.unit.domain;

import com.hedera.demo.auction.node.app.domain.Bid;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BidTest extends AbstractBid {
    @Test
    public void testBidSettersGetters() {

        Bid bid = testBidObject();
        verifyBidContents(bid);
    }

    @Test
    public void testBidToString() {
        Bid bid = testBidObject();

        String bidString = bid.toString();

        assertTrue(bidString.contains(timestamp.concat(", ")));
        assertTrue(bidString.contains(", ".concat(String.valueOf(auctionid))));
        assertTrue(bidString.contains(", ".concat(bidderaccountid)));
        assertTrue(bidString.contains(", ".concat(String.valueOf(bidamount))));
        assertTrue(bidString.contains(", ".concat(status)));
        assertTrue(bidString.contains(", false"));
        assertTrue(bidString.contains(", ".concat(refundtxid)));
        assertTrue(bidString.contains(", ".concat(refundtxhash)));
        assertTrue(bidString.contains(", ".concat(status)));
        assertTrue(bidString.contains(", ".concat(transactionid)));
        assertTrue(bidString.contains(", ".concat(transactionhash)));
    }

    @Test
    public void testBidToJson() {
        Bid bid = testBidObject();

        JsonObject bidJson = bid.toJson();

        assertEquals(timestamp, bidJson.getString("timestamp"));
        assertEquals(auctionid, bidJson.getLong("auctionid"));
        assertEquals(bidderaccountid, bidJson.getString("bidderaccountid"));
        assertEquals(bidamount, bidJson.getLong("bidamount"));
        assertEquals(status, bidJson.getString("status"));
        assertEquals(refunded, bidJson.getBoolean("refunded"));
        assertEquals(refundtxid, bidJson.getString("refundtxid"));
        assertEquals(refundtxhash, bidJson.getString("refundtxhash"));
        assertEquals(transactionid, bidJson.getString("transactionid"));
        assertEquals(transactionhash, bidJson.getString("transactionhash"));
    }

    @Test
    public void testBidFromJson() {
        JsonObject bidJson = testBidObject().toJson();
        Bid bid = new Bid(bidJson);
        verifyBidContents(bid);
    }
}