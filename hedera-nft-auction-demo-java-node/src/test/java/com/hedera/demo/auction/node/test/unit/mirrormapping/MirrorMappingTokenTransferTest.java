package com.hedera.demo.auction.node.test.unit.mirrormapping;

import com.hedera.demo.auction.node.mirrormapping.MirrorTokenTransfer;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class MirrorMappingTokenTransferTest {

    String account = "account";
    long amount = 10;
    String tokenId = "tokenId";

    @Test
    public void testMirrorMappingTokenTransferHedera() {

        JsonObject transfer = new JsonObject();
        transfer.put("account", account);
        transfer.put("amount", amount);
        transfer.put("token_id", tokenId);

        MirrorTokenTransfer mirrorTokenTransfer = transfer.mapTo(MirrorTokenTransfer.class);

        assertEquals(account, mirrorTokenTransfer.account);
        assertEquals(amount, mirrorTokenTransfer.amount);
        assertEquals(tokenId, mirrorTokenTransfer.tokenId);
    }

    @Test
    public void testMirrorMappingTokenTransferKabuto() {

        // TODO: Match Kabuto's format
        JsonObject transfer = new JsonObject();
        transfer.put("account", account);
        transfer.put("amount", amount);
        transfer.put("token_id", tokenId);

        MirrorTokenTransfer mirrorTokenTransfer = transfer.mapTo(MirrorTokenTransfer.class);

        assertEquals(account, mirrorTokenTransfer.account);
        assertEquals(amount, mirrorTokenTransfer.amount);
        assertEquals(tokenId, mirrorTokenTransfer.tokenId);
    }

    @Test
    public void testMirrorMappingTokenTransferDragonglass() {

        // TODO: Match Dragonglass's format
        JsonObject transfer = new JsonObject();
        transfer.put("account", account);
        transfer.put("amount", amount);
        transfer.put("token_id", tokenId);

        MirrorTokenTransfer mirrorTokenTransfer = transfer.mapTo(MirrorTokenTransfer.class);

        assertEquals(account, mirrorTokenTransfer.account);
        assertEquals(amount, mirrorTokenTransfer.amount);
        assertEquals(tokenId, mirrorTokenTransfer.tokenId);
    }

    @Test
    public void testMirrorMappingTokenTransferExtraData() {

        JsonObject transfer = new JsonObject();
        transfer.put("account", account);
        transfer.put("amount", amount);
        transfer.put("token_id", tokenId);
        transfer.put("dummy", "dummy");

        assertDoesNotThrow(() -> transfer.mapTo(MirrorTokenTransfer.class));
    }
}
