package com.hedera.demo.auction.test.unit.domain;

import com.hedera.demo.auction.app.domain.Bid;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BidTest extends AbstractBid {
    @Test
    public void testBidSettersGetters() {

        Bid bid = testBidObject();
        verifyBidContents(bid);
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
        assertEquals(refundtxid, bidJson.getString("refundtxid"));
        assertEquals(refundtxhash, bidJson.getString("refundtxhash"));
        assertEquals(transactionid, bidJson.getString("transactionid"));
        assertEquals(transactionhash, bidJson.getString("transactionhash"));
        assertEquals(refundStatus, bidJson.getString("refundstatus"));
    }

    @Test
    public void testBidFromJson() {
        JsonObject bidJson = testBidObject().toJson();
        Bid bid = new Bid(bidJson);
        verifyBidContents(bid);
    }

    @Test
    public void testBidRefundStatus() {
        Bid bid = new Bid();

        assertFalse(bid.isRefunded());
        assertFalse(bid.isRefundPending());
        assertFalse(bid.isRefundIssued());
        assertEquals("", bid.getRefundstatus());

        bid.setRefundstatus(Bid.REFUND_REFUNDED);
        assertTrue(bid.isRefunded());
        assertFalse(bid.isRefundPending());
        assertFalse(bid.isRefundIssued());

        bid.setRefundstatus(Bid.REFUND_ISSUED);
        assertFalse(bid.isRefunded());
        assertFalse(bid.isRefundPending());
        assertTrue(bid.isRefundIssued());

        bid.setRefundstatus(Bid.REFUND_PENDING);
        assertFalse(bid.isRefunded());
        assertTrue(bid.isRefundPending());
        assertFalse(bid.isRefundIssued());
    }
}
