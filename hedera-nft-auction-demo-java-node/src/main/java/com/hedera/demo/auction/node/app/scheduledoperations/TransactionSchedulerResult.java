package com.hedera.demo.auction.node.app.scheduledoperations;

import com.hedera.hashgraph.sdk.Status;

public class TransactionSchedulerResult {
    public boolean success;
    public Status status;

    public TransactionSchedulerResult(boolean success, Status status) {
        this.success = success;
        this.status = status;
    }
}
