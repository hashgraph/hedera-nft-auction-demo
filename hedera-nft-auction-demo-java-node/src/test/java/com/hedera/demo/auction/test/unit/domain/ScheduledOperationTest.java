package com.hedera.demo.auction.test.unit.domain;

import com.hedera.demo.auction.app.domain.ScheduledOperation;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ScheduledOperationTest extends AbstractScheduledOperation {
    @Test
    public void testScheduledOperationSettersGetters() {

        ScheduledOperation scheduledOperation = testScheduledOperationObject();
        verifyscheduledOperationContents(scheduledOperation);
    }

    @Test
    public void testScheduledOperationToJson() {
        ScheduledOperation scheduledOperation = testScheduledOperationObject();
        JsonObject json = scheduledOperation.toJson();

        assertEquals(transactiontype, json.getString("transactiontype"));
        assertEquals(transactiontimestamp, json.getString("transactiontimestamp"));
        assertEquals(auctionid, json.getInteger("auctionid"));
        assertEquals(transactionid, json.getString("transactionid"));
        assertEquals(memo, json.getString("memo"));
        assertEquals(result, json.getString("result"));
        assertEquals(status, json.getString("status"));
    }

    @Test
    public void testScheduledOperationFromJson() {
        JsonObject json = testScheduledOperationObject().toJson();
        ScheduledOperation scheduledOperation = new ScheduledOperation(json);
        verifyscheduledOperationContents(scheduledOperation);
    }

    @Test
    public void testScheduledOperationStatus() {
        ScheduledOperation scheduledOperation = testScheduledOperationObject();

        scheduledOperation.setStatus("");
        assertFalse(scheduledOperation.isPending());
        assertFalse(scheduledOperation.isExecuting());
        assertFalse(scheduledOperation.isSuccessful());
        assertEquals("", scheduledOperation.getStatus());

        scheduledOperation.setStatus(ScheduledOperation.PENDING);
        assertTrue(scheduledOperation.isPending());
        assertFalse(scheduledOperation.isExecuting());
        assertFalse(scheduledOperation.isSuccessful());

        scheduledOperation.setStatus(ScheduledOperation.EXECUTING);
        assertFalse(scheduledOperation.isPending());
        assertTrue(scheduledOperation.isExecuting());
        assertFalse(scheduledOperation.isSuccessful());

        scheduledOperation.setStatus(ScheduledOperation.SUCCESSFUL);
        assertFalse(scheduledOperation.isPending());
        assertFalse(scheduledOperation.isExecuting());
        assertTrue(scheduledOperation.isSuccessful());
    }
}
