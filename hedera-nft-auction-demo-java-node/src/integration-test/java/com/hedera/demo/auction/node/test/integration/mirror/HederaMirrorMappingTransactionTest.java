package com.hedera.demo.auction.node.test.integration.mirror;

import com.hedera.demo.auction.node.app.mirrormapping.MirrorTransaction;
import com.hedera.demo.auction.node.app.mirrormapping.MirrorTransactions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.codec.BodyCodec;
import io.vertx.junit5.Checkpoint;
import io.vertx.junit5.VertxTestContext;
import org.apache.commons.codec.binary.Hex;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class HederaMirrorMappingTransactionTest extends  AbstractMirrorTest {

    public HederaMirrorMappingTransactionTest() throws Exception {
    }

    @Test
    public void testMirrorMappingTransactionHedera(Vertx vertx, VertxTestContext testContext) throws IOException {
        @SuppressWarnings("unused")
        Checkpoint responsesReceived = testContext.checkpoint(1);

        var webQuery = webClient
            .get(mirrorURL, "/api/v1/transactions")
            .addQueryParam("transactiontype", "CRYPTOTRANSFER")
            .addQueryParam("order", "asc")
            .addQueryParam("limit", "2")
            .addQueryParam("timestamp", "gt:0");

        webQuery.as(BodyCodec.jsonObject())
            .send(testContext.succeeding(response -> testContext.verify(() -> {
                testResponse(response.body());
                testContext.completeNow();
            })
        ));
    }

    private static void testResponse(JsonObject body) {

        MirrorTransactions mirrorTransactions = body.mapTo(MirrorTransactions.class);

        JsonArray transactions = body.getJsonArray("transactions");
        assertEquals(transactions.size(), mirrorTransactions.transactions.size());
        assertEquals(body.getJsonObject("links").getString("next"), mirrorTransactions.links.next);

        JsonObject transaction = transactions.getJsonObject(0);

        String consensusTimestamp = transaction.getString("consensus_timestamp");
        String result = transaction.getString("result");
        boolean scheduled = transaction.getBoolean("scheduled");
        String transactionId = transaction.getString("transaction_id");

        byte[] transactionMemoBytes = Base64.getDecoder().decode(transaction.getString("memo_base64"));
        String memo = new String(transactionMemoBytes, StandardCharsets.UTF_8);

        byte[] txHashBytes = Base64.getDecoder().decode(transaction.getString("transaction_hash"));
        String transactionHash = Hex.encodeHexString(txHashBytes);

        MirrorTransaction mirrorTransaction = transaction.mapTo(MirrorTransaction.class);

        assertEquals(consensusTimestamp, mirrorTransaction.consensusTimestamp);
        assertEquals(memo, mirrorTransaction.getMemo());
        assertEquals(result, mirrorTransaction.result);
        assertEquals(scheduled, mirrorTransaction.scheduled);
        assertEquals(transactionHash, mirrorTransaction.getTransactionHash());
        assertEquals(transactionId, mirrorTransaction.transactionId);

        JsonArray hbarTransfers = transaction.getJsonArray("transfers");
        assertEquals(hbarTransfers.size(), mirrorTransaction.hbarTransfers.size());

        for (int i=0; i < hbarTransfers.size(); i++) {
            assertEquals(hbarTransfers.getJsonObject(i).getString("account"), mirrorTransaction.hbarTransfers.get(i).account);
            assertEquals((long)hbarTransfers.getJsonObject(i).getLong("amount"), mirrorTransaction.hbarTransfers.get(i).amount);
        }
        //TODO: Would be good to get a mirror response containing token_Transfers which is not guaranteed
        if (transaction.containsKey("token_transfers")) {
            JsonArray tokenTransfers = transaction.getJsonArray("token_transfers");
            assertEquals(tokenTransfers.size(), mirrorTransaction.tokenTransfers.size());

            for (int i = 0; i < tokenTransfers.size(); i++) {
                assertEquals(tokenTransfers.getJsonObject(i).getString("account"), mirrorTransaction.tokenTransfers.get(i).account);
                assertEquals((long) tokenTransfers.getJsonObject(i).getLong("amount"), mirrorTransaction.tokenTransfers.get(i).amount);
                assertEquals(tokenTransfers.getJsonObject(i).getString("token_id"), mirrorTransaction.tokenTransfers.get(i).tokenId);
            }
        }
        assertEquals(transactionId.substring(0, transactionId.indexOf("-")), mirrorTransaction.payer());

        boolean success = result.equals("SUCCESS");
        assertEquals(success, mirrorTransaction.isSuccessful());
    }
}