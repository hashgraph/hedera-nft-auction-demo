package com.hedera.demo.auction.test.integration.mirror;

import com.hedera.demo.auction.app.mirrormapping.MirrorTransaction;
import com.hedera.demo.auction.app.mirrormapping.MirrorTransactions;
import com.hedera.demo.auction.test.integration.AbstractMirrorIntegrationTest;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.codec.BodyCodec;
import io.vertx.junit5.Checkpoint;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class HederaMirrorMappingTransactionIntegrationTest extends AbstractMirrorIntegrationTest {

    public HederaMirrorMappingTransactionIntegrationTest() throws Exception {
        super("hedera");
    }

    @Test
    public void testMirrorMappingTransactionHedera(Vertx vertx, VertxTestContext testContext) throws IOException {
        @SuppressWarnings("unused")
        Checkpoint responsesReceived = testContext.checkpoint(1);

        var webQuery = webClient
            .get(mirrorURL, "/api/v1/transactions")
            .ssl(false)
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
        String transactionId = transaction.getString("transaction_id");
        String memo = transaction.getString("memo_base64");
        String transactionHash = transaction.getString("transaction_hash");

        MirrorTransaction mirrorTransaction = transaction.mapTo(MirrorTransaction.class);

        assertEquals(consensusTimestamp, mirrorTransaction.consensusTimestamp);
        assertEquals(memo, mirrorTransaction.memo);
        assertEquals(result, mirrorTransaction.result);
        assertEquals(transactionHash, mirrorTransaction.transactionHash);
        assertEquals(transactionId, mirrorTransaction.transactionId);

        JsonArray hbarTransfers = transaction.getJsonArray("transfers");
        assertEquals(hbarTransfers.size(), mirrorTransaction.hbarTransfers.size());

        for (int i=0; i < hbarTransfers.size(); i++) {
            assertEquals(hbarTransfers.getJsonObject(i).getString("account"), mirrorTransaction.hbarTransfers.get(i).account);
            assertEquals((long)hbarTransfers.getJsonObject(i).getLong("amount"), mirrorTransaction.hbarTransfers.get(i).amount);
        }
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
