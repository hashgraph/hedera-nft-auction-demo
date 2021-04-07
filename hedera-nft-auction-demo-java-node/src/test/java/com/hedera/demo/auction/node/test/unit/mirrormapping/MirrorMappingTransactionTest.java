package com.hedera.demo.auction.node.test.unit.mirrormapping;

import com.google.errorprone.annotations.Var;
import com.hedera.demo.auction.node.app.mirrormapping.MirrorTransaction;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MirrorMappingTransactionTest extends AbstractMirrorMapping {

    @Test
    public void testMirrorMappingTransactionHedera() throws IOException {

        JsonObject transaction = loadJsonFile("hedera-mirror/hedera-mirror-transaction.json");

        String consensusTimestamp = transaction.getString("consensus_timestamp");
        String result = transaction.getString("result");
        boolean scheduled = transaction.getBoolean("scheduled");
        String transactionId = transaction.getString("transaction_id");

        byte[] transactionMemoBytes = Base64.getDecoder().decode(transaction.getString("memo_base64"));
        String memo = new String(transactionMemoBytes, StandardCharsets.UTF_8);

        byte[] txHashBytes = Base64.getDecoder().decode(transaction.getString("transaction_hash"));
        String transactionHash = Hex.encodeHexString(txHashBytes);

        MirrorTransaction mirrorTransaction = transaction.mapTo(MirrorTransaction.class);

        assertEquals(consensusTimestamp, mirrorTransaction.getConsensusTimestamp());
        assertEquals(memo, mirrorTransaction.getMemo());
        assertEquals(result, mirrorTransaction.getResult());
        assertEquals(scheduled, mirrorTransaction.getScheduled());
        assertEquals(transactionHash, mirrorTransaction.getTransactionHash());
        assertEquals(transactionId, mirrorTransaction.getTransactionId());

        JsonArray hbarTransfers = transaction.getJsonArray("transfers");
        assertEquals(hbarTransfers.size(), mirrorTransaction.hbarTransfers.size());

        for (int i=0; i < hbarTransfers.size(); i++) {
            assertEquals(hbarTransfers.getJsonObject(i).getString("account"), mirrorTransaction.hbarTransfers.get(i).getAccount());
            assertEquals((long)hbarTransfers.getJsonObject(i).getLong("amount"), mirrorTransaction.hbarTransfers.get(i).getAmount());
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
    public void testMirrorMappingTransactionScheduledHedera() throws IOException {

        JsonObject transaction = loadJsonFile("hedera-mirror/hedera-mirror-transaction.json");

        transaction.put("scheduled", false);
        @Var MirrorTransaction mirrorTransaction = transaction.mapTo(MirrorTransaction.class);
        assertEquals(false, mirrorTransaction.getScheduled());

        transaction.put("scheduled", true);
        mirrorTransaction = transaction.mapTo(MirrorTransaction.class);
        assertEquals(true, mirrorTransaction.getScheduled());
    }

    @Test
    public void testMirrorMappingTransactionResultHedera() throws IOException {

        JsonObject transaction = loadJsonFile("hedera-mirror/hedera-mirror-transaction.json");

        transaction.put("result", "ERROR");
        @Var MirrorTransaction mirrorTransaction = transaction.mapTo(MirrorTransaction.class);
        assertEquals("ERROR", mirrorTransaction.getResult());
        assertEquals(false, mirrorTransaction.isSuccessful());

        transaction.put("result", "SUCCESS");
        mirrorTransaction = transaction.mapTo(MirrorTransaction.class);
        assertEquals("SUCCESS", mirrorTransaction.getResult());
        assertEquals(true, mirrorTransaction.isSuccessful());

    }

    @Test
    public void testMirrorMappingTransactionKabuto() throws DecoderException {

        // TODO: Match Kabuto's format
        assertEquals(1, 1);
    }

    @Test
    public void testMirrorMappingTransactionDragonglass() throws DecoderException {

        // TODO: Match Dragonglass's format
        assertEquals(1, 1);
    }
}
