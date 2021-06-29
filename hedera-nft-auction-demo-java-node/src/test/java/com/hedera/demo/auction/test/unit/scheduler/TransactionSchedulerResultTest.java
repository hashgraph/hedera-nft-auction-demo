package com.hedera.demo.auction.test.unit.scheduler;

import com.google.errorprone.annotations.Var;
import com.hedera.demo.auction.app.scheduledoperations.TransactionSchedulerResult;
import com.hedera.hashgraph.sdk.ScheduleId;
import com.hedera.hashgraph.sdk.Status;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TransactionSchedulerResultTest {
    @Test
    public void testTransactionSchedulerResultConstructor() {

        @Var ScheduleId scheduleId = ScheduleId.fromString("0.0.22");
        @Var TransactionSchedulerResult transactionSchedulerResult = new TransactionSchedulerResult(/* success= */ true, Status.ACCOUNT_DELETED, scheduleId);
        assertTrue(transactionSchedulerResult.success);
        assertEquals(Status.ACCOUNT_DELETED, transactionSchedulerResult.status);
        assertEquals(scheduleId.toString(), transactionSchedulerResult.getScheduleId());

        scheduleId = ScheduleId.fromString("0.0.23");
        transactionSchedulerResult = new TransactionSchedulerResult(/* success= */ false, Status.ACCOUNT_FROZEN_FOR_TOKEN, scheduleId);
        assertFalse(transactionSchedulerResult.success);
        assertEquals(Status.ACCOUNT_FROZEN_FOR_TOKEN, transactionSchedulerResult.status);
        assertEquals(scheduleId.toString(), transactionSchedulerResult.getScheduleId());
    }

    @Test
    public void testTransactionSchedulerResultShortConstructor() {

        @Var TransactionSchedulerResult transactionSchedulerResult = new TransactionSchedulerResult(/* success= */ true, Status.ACCOUNT_DELETED);
        assertTrue(transactionSchedulerResult.success);
        assertEquals(Status.ACCOUNT_DELETED, transactionSchedulerResult.status);

        transactionSchedulerResult = new TransactionSchedulerResult(/* success= */ false, Status.ACCOUNT_FROZEN_FOR_TOKEN);
        assertFalse(transactionSchedulerResult.success);
        assertEquals(Status.ACCOUNT_FROZEN_FOR_TOKEN, transactionSchedulerResult.status);
    }
}
