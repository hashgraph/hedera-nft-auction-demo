package com.hedera.demo.auction.node.test.unit.domain;

import com.hedera.demo.auction.node.app.domain.Bid;

import static org.junit.jupiter.api.Assertions.assertEquals;

public abstract class AbstractBid {
    String timestamp = "timestamp";
    int auctionid = 2;
    String bidderaccountid = "bidderaccountid";
    Long bidamount = 30L;
    String status = "status";
    boolean refunded = false;
    String refundtxid = "refundtxid";
    String refundtxhash = "refundtxhash";
    String transactionid = "transactionid";
    String transactionhash = "transactionhash";
    boolean refund = true;
    String timestampforrefund = "timestampforrefund";

    Bid testBidObject() {
        Bid bid = new Bid();

        bid.setTimestamp(timestamp);
        bid.setAuctionid(auctionid);
        bid.setBidderaccountid(bidderaccountid);
        bid.setBidamount(bidamount);
        bid.setStatus(status);
        bid.setRefunded(refunded);
        bid.setRefundtxid(refundtxid);
        bid.setRefundtxhash(refundtxhash);
        bid.setTransactionid(transactionid);
        bid.setTransactionhash(transactionhash);
        bid.setRefund(refund);
        bid.setTimestampforrefund(timestampforrefund);

        return bid;
    }

    public void verifyBidContents(Bid bid) {
        assertEquals(timestamp, bid.getTimestamp());
        assertEquals(auctionid, bid.getAuctionid());
        assertEquals(bidderaccountid, bid.getBidderaccountid());
        assertEquals(bidamount, bid.getBidamount());
        assertEquals(status, bid.getStatus());
        assertEquals(refunded, bid.getRefunded());
        assertEquals(refundtxid, bid.getRefundtxid());
        assertEquals(refundtxhash, bid.getRefundtxhash());
        assertEquals(transactionid, bid.getTransactionid());
        assertEquals(transactionhash, bid.getTransactionhash());
        assertEquals(refund, bid.getRefund());
        assertEquals(timestampforrefund, bid.getTimestampforrefund());
    }
}
