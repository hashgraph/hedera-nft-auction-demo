package com.hedera.demo.auction.test.unit.mirrormapping;

import com.hedera.demo.auction.app.mirrormapping.MirrorSchedule;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class HederaMirrorMappingScheduleTest extends AbstractMirrorMapping {

    @Test
    public void testMirrorMappingSchedule() throws IOException {

        JsonObject schedule = loadJsonFile("hedera-mirror/hedera-mirror-schedule.json");

        String consensusTimestamp = schedule.getString("consensus_timestamp");
        String executedTimestamp = schedule.getString("executed_timestamp");
        JsonArray signaturesJson = schedule.getJsonArray("signatures");
        JsonObject signature1 = signaturesJson.getJsonObject(0);
        JsonObject signature2 = signaturesJson.getJsonObject(1);

        MirrorSchedule mirrorSchedule = schedule.mapTo(MirrorSchedule.class);

        assertEquals(consensusTimestamp,mirrorSchedule.consensusTimestamp);
        assertEquals(executedTimestamp, mirrorSchedule.executedTimestamp);
        assertEquals(2, mirrorSchedule.getSignatureCount());
        assertEquals(signature1.getString("consensus_timestamp"), mirrorSchedule.mirrorScheduleSignatures[0].consensusTimestamp);
        assertEquals(signature1.getString("public_key_prefix"), mirrorSchedule.mirrorScheduleSignatures[0].publicKeyPrefix);

        assertEquals(signature2.getString("consensus_timestamp"), mirrorSchedule.mirrorScheduleSignatures[1].consensusTimestamp);
        assertEquals(signature2.getString("public_key_prefix"), mirrorSchedule.mirrorScheduleSignatures[1].publicKeyPrefix);

        String prefix = mirrorSchedule.mirrorScheduleSignatures[0].getPublicKeyPrefix();
        assertNotNull(prefix);
    }
}
