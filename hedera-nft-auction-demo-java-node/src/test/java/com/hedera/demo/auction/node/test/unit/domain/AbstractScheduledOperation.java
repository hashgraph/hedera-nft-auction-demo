package com.hedera.demo.auction.node.test.unit.domain;

import com.hedera.demo.auction.node.app.domain.ScheduledOperation;

import static org.junit.jupiter.api.Assertions.assertEquals;

public abstract class AbstractScheduledOperation {
    protected final String transactiontype = "transactiontype";
    protected final String transactiontimestamp = "transactiontimestamp";
    protected final int auctionid = 10;
    protected final String transactionid = "transactionid";
    protected final String memo = "memo";
    protected final String result = "result";
    protected final String status = "status";


    ScheduledOperation testScheduledOperationObject() {
        ScheduledOperation scheduledOperation = new ScheduledOperation();

        scheduledOperation.setTransactiontype(transactiontype);
        scheduledOperation.setTransactiontimestamp(transactiontimestamp);
        scheduledOperation.setAuctionid(auctionid);
        scheduledOperation.setTransactionid(transactionid);
        scheduledOperation.setMemo(memo);
        scheduledOperation.setResult(result);
        scheduledOperation.setStatus(status);

        return scheduledOperation;
    }

    public void verifyscheduledOperationContents(ScheduledOperation scheduledOperation) {
        assertEquals(transactiontype, scheduledOperation.getTransactiontype());
        assertEquals(transactiontimestamp, scheduledOperation.getTransactiontimestamp());
        assertEquals(auctionid, scheduledOperation.getAuctionid());
        assertEquals(transactionid, scheduledOperation.getTransactionid());
        assertEquals(memo, scheduledOperation.getMemo());
        assertEquals(result, scheduledOperation.getResult());
        assertEquals(status, scheduledOperation.getStatus());
    }
}
