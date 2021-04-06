package com.hedera.demo.auction.node.test.unit.mirrormapping;

import com.hedera.demo.auction.node.mirrormapping.MirrorTransactions;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.apache.commons.codec.DecoderException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MirrorMappingTransactionsTest extends AbstractMirrorMapping {

    @Test
    public void testMirrorMappingTransactionsHedera() throws DecoderException {

        JsonObject transaction1 = hederaTransaction();
        JsonObject transaction2 = hederaTransaction();

        JsonArray transactions = new JsonArray();
        transactions.add(transaction1);
        transactions.add(transaction2);

        JsonObject transaction = new JsonObject();

        transaction.put("transactions", transactions);

        JsonObject link = new JsonObject();
        link.put("next", linkURL);
        transaction.put("links", link);

        MirrorTransactions mirrorTransactions = transaction.mapTo(MirrorTransactions.class);

        assertEquals(transactions.size(), mirrorTransactions.transactions.size());

        assertEquals(linkURL, mirrorTransactions.links.getNext());

    }

    @Test
    public void testMirrorMappingTransactionsKabuto() throws DecoderException {

        // TODO: Match Kabuto's format
        JsonObject transaction1 = hederaTransaction();
        JsonObject transaction2 = hederaTransaction();

        JsonArray transactions = new JsonArray();
        transactions.add(transaction1);
        transactions.add(transaction2);

        JsonObject transaction = new JsonObject();

        transaction.put("transactions", transactions);

//        JsonObject links = new JsonObject();
//        JsonObject link = new JsonObject();
//        link.put("next", linkURL);
//        links.put("links", link);
//        transaction.put("links", links);

        MirrorTransactions mirrorTransactions = transaction.mapTo(MirrorTransactions.class);

        assertEquals(transactions.size(), mirrorTransactions.transactions.size());

//        assertEquals(linkURL, mirrorTransactions.links.getNext());
    }

    @Test
    public void testMirrorMappingTransactionsDragonglass() throws DecoderException {

        // TODO: Match Dragonglass's format
        JsonObject transaction1 = hederaTransaction();
        JsonObject transaction2 = hederaTransaction();

        JsonArray transactions = new JsonArray();
        transactions.add(transaction1);
        transactions.add(transaction2);

        JsonObject transaction = new JsonObject();

        transaction.put("transactions", transactions);

//        JsonObject links = new JsonObject();
//        JsonObject link = new JsonObject();
//        link.put("next", linkURL);
//        links.put("links", link);
//        transaction.put("links", links);

        MirrorTransactions mirrorTransactions = transaction.mapTo(MirrorTransactions.class);

        assertEquals(transactions.size(), mirrorTransactions.transactions.size());

//        assertEquals(linkURL, mirrorTransactions.links.getNext());
    }
}
