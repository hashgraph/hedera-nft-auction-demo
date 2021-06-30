package com.hedera.demo.auction.app.scheduledoperations;

import com.hedera.hashgraph.sdk.ScheduleId;
import com.hedera.hashgraph.sdk.Status;

import javax.annotation.Nullable;

/**
 * Holds the result of a transaction scheduling operation
 */
public class TransactionSchedulerResult {
    public boolean success;
    @Nullable
    private ScheduleId scheduleId;
    public Status status;

    /**
     * Constructor
     *
     * @param success boolean indicating success
     * @param status the Status of the operation
     * @param scheduleId the schedule id for the operation
     */
    public TransactionSchedulerResult(boolean success, Status status, ScheduleId scheduleId) {
        this.success = success;
        this.status = status;
        this.scheduleId = scheduleId;
    }

    /**
     * Constructor
     *
     * @param success boolean indicating success
     * @param status the Status of the operation
     */
    public TransactionSchedulerResult(boolean success, Status status) {
        this.success = success;
        this.status = status;
    }

    /**
     * Returns the schedule Id as a String
     *
     * @return String the schedule id
     */
    public String getScheduleId() {
        return (this.scheduleId == null) ? "" : this.scheduleId.toString();
    }
}
