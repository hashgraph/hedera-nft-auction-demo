package com.hedera.demo.auction.app.scheduledoperations;

import com.hedera.hashgraph.sdk.ScheduleId;
import com.hedera.hashgraph.sdk.Status;

import javax.annotation.Nullable;

public class TransactionSchedulerResult {
    public boolean success;
    @Nullable
    private ScheduleId scheduleId;
    public Status status;

    public TransactionSchedulerResult(boolean success, Status status, ScheduleId scheduleId) {
        this.success = success;
        this.status = status;
        this.scheduleId = scheduleId;
    }
    public TransactionSchedulerResult(boolean success, Status status) {
        this.success = success;
        this.status = status;
    }
    public String getScheduleId() {
        return (this.scheduleId == null) ? "" : this.scheduleId.toString();
    }
}
