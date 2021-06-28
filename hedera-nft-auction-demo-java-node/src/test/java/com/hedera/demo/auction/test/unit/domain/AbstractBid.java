package com.hedera.demo.auction.test.unit.domain;

import com.hedera.demo.auction.app.domain.Bid;

import static org.junit.jupiter.api.Assertions.assertEquals;

public abstract class AbstractBid {
    String timestamp = "timestamp";
    int auctionid = 2;
    String bidderaccountid = "bidderaccountid";
    Long bidamount = 30L;
    String status = "status";
    String refundtxid = "refundtxid";
    String refundtxhash = "refundtxhash";
    String transactionid = "transactionid";
    String transactionhash = "transactionhash";
    String refundStatus = Bid.REFUND_ISSUED;

    Bid testBidObject() {
        Bid bid = new Bid();

        bid.setTimestamp(timestamp);
        bid.setAuctionid(auctionid);
        bid.setBidderaccountid(bidderaccountid);
        bid.setBidamount(bidamount);
        bid.setStatus(status);
        bid.setRefundtxid(refundtxid);
        bid.setRefundtxhash(refundtxhash);
        bid.setTransactionid(transactionid);
        bid.setTransactionhash(transactionhash);
        bid.setRefundstatus(refundStatus);

        return bid;
    }

    public void verifyBidContents(Bid bid) {
        assertEquals(timestamp, bid.getTimestamp());
        assertEquals(auctionid, bid.getAuctionid());
        assertEquals(bidderaccountid, bid.getBidderaccountid());
        assertEquals(bidamount, bid.getBidamount());
        assertEquals(status, bid.getStatus());
        assertEquals(refundtxid, bid.getRefundtxid());
        assertEquals(refundtxhash, bid.getRefundtxhash());
        assertEquals(transactionid, bid.getTransactionid());
        assertEquals(transactionhash, bid.getTransactionhash());
        assertEquals(refundStatus, bid.getRefundstatus());
    }
}
