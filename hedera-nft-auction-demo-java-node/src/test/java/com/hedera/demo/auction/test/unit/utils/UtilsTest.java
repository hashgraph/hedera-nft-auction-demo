package com.hedera.demo.auction.test.unit.utils;

import com.google.errorprone.annotations.Var;
import com.hedera.demo.auction.app.Utils;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class UtilsTest {
    @Test
    public void testHederaMirrorTransactionId() {

        String transactionId = "0.0.123@123123.123132";
        String converted = Utils.hederaMirrorTransactionId(transactionId);
        assertEquals("0.0.123-123123-123132", converted);

    }
    @Test
    public void addToTimestampTest() {
        String timestamp = "123.001";
        long secondsToAdd = 4;

        String newTimeStamp = Utils.addToTimestamp(timestamp, secondsToAdd);
        assertEquals("127.001", newTimeStamp);
    }

    @Test
    public void timestampToDateTest() {
        @Var Instant now = Instant.now();
        String timestamp = now.getEpochSecond() + "." + now.getNano();

        now = now.minusNanos(now.getNano());

        String testInstant = Utils.timestampToDate(timestamp);

        assertEquals(now.toString().concat(" (UTC)"), testInstant);

    }

    @Test
    public void timestampFromMirrorLinkTest() {
        String linkgt = "api/v1/transactions?transactiontype=CRYPTOTRANSFER&order=asc&timestamp=gt:1598576703.187899009";
        @Var String linkTest = Utils.getTimestampFromMirrorLink(linkgt);
        assertEquals("1598576703.187899009", linkTest);

        String linklt = "api/v1/transactions?transactiontype=CRYPTOTRANSFER&order=asc&timestamp=lt:1598576703.187899009";
        linkTest = Utils.getTimestampFromMirrorLink(linklt);
        assertEquals("1598576703.187899009", linkTest);
    }
}
