package com.hedera.demo.auction.test.unit.mirrormapping;

import com.google.errorprone.annotations.Var;
import com.hedera.demo.auction.app.mirrormapping.MirrorTransaction;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class HederaMirrorMappingTransactionTest extends AbstractMirrorMapping {

    @Test
    public void testMirrorMappingTransactionHedera() throws IOException {

        JsonObject transaction = loadJsonFile("hedera-mirror/hedera-mirror-transaction.json");

        String consensusTimestamp = transaction.getString("consensus_timestamp");
        String result = transaction.getString("result");
        String transactionId = transaction.getString("transaction_id");

        String memo = transaction.getString("memo_base64");
        String transactionHash = transaction.getString("transaction_hash");
        String name = transaction.getString("name");

        MirrorTransaction mirrorTransaction = transaction.mapTo(MirrorTransaction.class);

        assertEquals(consensusTimestamp, mirrorTransaction.consensusTimestamp);
        assertEquals(memo, mirrorTransaction.memo);
        assertEquals(result, mirrorTransaction.result);
        assertEquals(transactionHash, mirrorTransaction.transactionHash);
        assertEquals(transactionId, mirrorTransaction.transactionId);
        assertEquals(name, mirrorTransaction.name);

        JsonArray hbarTransfers = transaction.getJsonArray("transfers");
        assertEquals(hbarTransfers.size(), mirrorTransaction.hbarTransfers.size());

        for (int i=0; i < hbarTransfers.size(); i++) {
            assertEquals(hbarTransfers.getJsonObject(i).getString("account"), mirrorTransaction.hbarTransfers.get(i).account);
            assertEquals((long)hbarTransfers.getJsonObject(i).getLong("amount"), mirrorTransaction.hbarTransfers.get(i).amount);
        }

        JsonArray tokenTransfers = transaction.getJsonArray("token_transfers");
        assertEquals(tokenTransfers.size(), mirrorTransaction.tokenTransfers.size());

        for (int i=0; i < tokenTransfers.size(); i++) {
            assertEquals(tokenTransfers.getJsonObject(i).getString("account"), mirrorTransaction.tokenTransfers.get(i).account);
            assertEquals((long)tokenTransfers.getJsonObject(i).getLong("amount"), mirrorTransaction.tokenTransfers.get(i).amount);
            assertEquals(tokenTransfers.getJsonObject(i).getString("token_id"), mirrorTransaction.tokenTransfers.get(i).tokenId);
        }

        assertEquals(transactionId.substring(0, transactionId.indexOf("-")), mirrorTransaction.payer());

        boolean success = result.equals("SUCCESS");
        assertEquals(success, mirrorTransaction.isSuccessful());
    }

    @Test
    public void testMirrorMappingTransactionResultHedera() throws IOException {

        JsonObject transaction = loadJsonFile("hedera-mirror/hedera-mirror-transaction.json");

        transaction.put("result", "ERROR");
        @Var MirrorTransaction mirrorTransaction = transaction.mapTo(MirrorTransaction.class);
        assertEquals("ERROR", mirrorTransaction.result);
        assertEquals(false, mirrorTransaction.isSuccessful());

        transaction.put("result", "SUCCESS");
        mirrorTransaction = transaction.mapTo(MirrorTransaction.class);
        assertEquals("SUCCESS", mirrorTransaction.result);
        assertEquals(true, mirrorTransaction.isSuccessful());

    }
}
