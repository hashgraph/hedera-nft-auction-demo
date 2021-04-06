package com.hedera.demo.auction.node.test.unit.mirrormapping;

import com.hedera.demo.auction.node.mirrormapping.MirrorHbarTransfer;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class MirrorMappingHbarTransferTest {
    String account = "account";
    long amount = 10;

    @Test
    public void testMirrorMappingHbarTransferHedera() {

        JsonObject transfer = new JsonObject();
        transfer.put("account", account);
        transfer.put("amount",amount);

        MirrorHbarTransfer mirrorHbarTransfer = transfer.mapTo(MirrorHbarTransfer.class);

        assertEquals(account, mirrorHbarTransfer.getAccount());
        assertEquals(amount, mirrorHbarTransfer.getAmount());
    }

    @Test
    public void testMirrorMappingHbarTransferKabuto() {

        // TODO: Match Kabuto's format
        JsonObject transfer = new JsonObject();
        transfer.put("account", account);
        transfer.put("amount",amount);

        MirrorHbarTransfer mirrorHbarTransfer = transfer.mapTo(MirrorHbarTransfer.class);

        assertEquals(account, mirrorHbarTransfer.getAccount());
        assertEquals(amount, mirrorHbarTransfer.getAmount());
    }

    @Test
    public void testMirrorMappingHbarTransferDragonglass() {

        // TODO: Match Dragonglass's format
        JsonObject transfer = new JsonObject();
        transfer.put("account", account);
        transfer.put("amount",amount);

        MirrorHbarTransfer mirrorHbarTransfer = transfer.mapTo(MirrorHbarTransfer.class);

        assertEquals(account, mirrorHbarTransfer.getAccount());
        assertEquals(amount, mirrorHbarTransfer.getAmount());
    }

    @Test
    public void testMirrorMappingHbarTransferExtraData() {

        JsonObject transfer = new JsonObject();
        transfer.put("account", account);
        transfer.put("amount",amount);
        transfer.put("dummy", "dummy");

        assertDoesNotThrow(() -> transfer.mapTo(MirrorHbarTransfer.class));
    }
    
}
