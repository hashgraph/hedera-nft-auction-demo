package com.hedera.demo.auction.node.app;

import com.hedera.hashgraph.sdk.Status;

public class TransactionSchedulerResult {
    public boolean success = false;
    public Status status = Status.UNKNOWN;

    public TransactionSchedulerResult(boolean success, Status status) {
        this.success = success;
        this.status = status;
    }
}
