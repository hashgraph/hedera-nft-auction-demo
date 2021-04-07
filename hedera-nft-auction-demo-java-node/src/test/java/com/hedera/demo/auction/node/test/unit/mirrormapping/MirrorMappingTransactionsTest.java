package com.hedera.demo.auction.node.test.unit.mirrormapping;

import com.hedera.demo.auction.node.app.mirrormapping.MirrorTransactions;
import io.vertx.core.json.JsonObject;
import org.apache.commons.codec.DecoderException;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MirrorMappingTransactionsTest extends AbstractMirrorMapping {

    @Test
    public void testMirrorMappingTransactionsHedera() throws IOException {
        JsonObject transactions = loadJsonFile("hedera-mirror/hedera-mirror-transactions.json");
        MirrorTransactions mirrorTransactions = transactions.mapTo(MirrorTransactions.class);
        assertEquals(transactions.getJsonArray("transactions").size(), mirrorTransactions.transactions.size());
        assertEquals(transactions.getJsonObject("links").getString("next"), mirrorTransactions.links.getNext());
    }

    @Test
    public void testMirrorMappingTransactionsKabuto() throws DecoderException {

        // TODO: Match Kabuto's format
        assertEquals(1, 1);
    }

    @Test
    public void testMirrorMappingTransactionsDragonglass() throws DecoderException {

        // TODO: Match Dragonglass's format
        assertEquals(1, 1);
    }
}
