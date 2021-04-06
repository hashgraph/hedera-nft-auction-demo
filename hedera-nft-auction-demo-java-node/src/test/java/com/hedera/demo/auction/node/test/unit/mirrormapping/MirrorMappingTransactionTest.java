package com.hedera.demo.auction.node.test.unit.mirrormapping;

import com.hedera.demo.auction.node.mirrormapping.MirrorTransaction;
import io.vertx.core.json.JsonObject;
import org.apache.commons.codec.DecoderException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MirrorMappingTransactionTest extends AbstractMirrorMapping {

    @Test
    public void testMirrorMappingTransactionHedera() throws DecoderException {

        JsonObject transaction = hederaTransaction();

        MirrorTransaction mirrorTransaction = transaction.mapTo(MirrorTransaction.class);

        assertEquals(timestamp, mirrorTransaction.getConsensusTimestamp());
        assertEquals(memo, mirrorTransaction.getMemo());
        assertEquals(result, mirrorTransaction.getResult());
        assertEquals(scheduled, mirrorTransaction.getScheduled());
        assertEquals(transactionHash, mirrorTransaction.getTransactionHash());
        assertEquals(transactionId, mirrorTransaction.getTransactionId());

        assertEquals(hbarTransfers.size(), mirrorTransaction.hbarTransfers.size());
        assertEquals(transferAccount1, mirrorTransaction.hbarTransfers.get(0).getAccount());
        assertEquals(transferAmount1, mirrorTransaction.hbarTransfers.get(0).getAmount());
        assertEquals(transferAccount2, mirrorTransaction.hbarTransfers.get(1).getAccount());
        assertEquals(transferAmount2, mirrorTransaction.hbarTransfers.get(1).getAmount());

        assertEquals(tokenTransfers.size(), mirrorTransaction.tokenTransfers.size());
        assertEquals(transferAccount1, mirrorTransaction.tokenTransfers.get(0).account);
        assertEquals(transferAmount1, mirrorTransaction.tokenTransfers.get(0).amount);
        assertEquals(transferToken1, mirrorTransaction.tokenTransfers.get(0).tokenId);
        assertEquals(transferAccount2, mirrorTransaction.tokenTransfers.get(1).account);
        assertEquals(transferAmount2, mirrorTransaction.tokenTransfers.get(1).amount);
        assertEquals(transferToken2, mirrorTransaction.tokenTransfers.get(1).tokenId);

        assertEquals(payerAccount, mirrorTransaction.payer());
        assertEquals(successful, mirrorTransaction.isSuccessful());
    }

    @Test
    public void testMirrorMappingTransactionAlternativeHedera() throws DecoderException {

        JsonObject transaction = hederaTransaction();

        transaction.put("result", "ERROR");
        transaction.put("scheduled", false);

        MirrorTransaction mirrorTransaction = transaction.mapTo(MirrorTransaction.class);

        assertEquals("ERROR", mirrorTransaction.getResult());
        assertEquals(false, mirrorTransaction.getScheduled());
    }


    @Test
    public void testMirrorMappingTransactionKabuto() throws DecoderException {

        // TODO: Match Kabuto's format
        JsonObject transaction = kabutoTransaction();

        MirrorTransaction mirrorTransaction = transaction.mapTo(MirrorTransaction.class);

        assertEquals(timestamp, mirrorTransaction.getConsensusTimestamp());
        assertEquals(memo, mirrorTransaction.getMemo());
        assertEquals(result, mirrorTransaction.getResult());
        assertEquals(scheduled, mirrorTransaction.getScheduled());
        assertEquals(transactionHash, mirrorTransaction.getTransactionHash());
        assertEquals(transactionId, mirrorTransaction.getTransactionId());

//        assertEquals(hbarTransfers.size(), mirrorTransaction.hbarTransfers.size());
//        assertEquals(transferAccount1, mirrorTransaction.hbarTransfers.get(0).getAccount());
//        assertEquals(transferAmount1, mirrorTransaction.hbarTransfers.get(0).getAmount());
//        assertEquals(transferAccount2, mirrorTransaction.hbarTransfers.get(1).getAccount());
//        assertEquals(transferAmount2, mirrorTransaction.hbarTransfers.get(1).getAmount());
//
//        assertEquals(tokenTransfers.size(), mirrorTransaction.tokenTransfers.size());
//        assertEquals(transferAccount1, mirrorTransaction.tokenTransfers.get(0).account);
//        assertEquals(transferAmount1, mirrorTransaction.tokenTransfers.get(0).amount);
//        assertEquals(transferToken1, mirrorTransaction.tokenTransfers.get(0).tokenId);
//        assertEquals(transferAccount2, mirrorTransaction.tokenTransfers.get(1).account);
//        assertEquals(transferAmount2, mirrorTransaction.tokenTransfers.get(1).amount);
//        assertEquals(transferToken2, mirrorTransaction.tokenTransfers.get(1).tokenId);

        assertEquals(payerAccount, mirrorTransaction.payer());
        assertEquals(successful, mirrorTransaction.isSuccessful());

    }

    @Test
    public void testMirrorMappingTransactionDragonglass() throws DecoderException {

        // TODO: Match Dragonglass's format
        JsonObject transaction = dragonglassTransaction();

        MirrorTransaction mirrorTransaction = transaction.mapTo(MirrorTransaction.class);

        assertEquals(timestamp, mirrorTransaction.getConsensusTimestamp());
        assertEquals(memo, mirrorTransaction.getMemo());
        assertEquals(result, mirrorTransaction.getResult());
        assertEquals(scheduled, mirrorTransaction.getScheduled());
        assertEquals(transactionHash, mirrorTransaction.getTransactionHash());
        assertEquals(transactionId, mirrorTransaction.getTransactionId());

//        assertEquals(hbarTransfers.size(), mirrorTransaction.hbarTransfers.size());
//        assertEquals(transferAccount1, mirrorTransaction.hbarTransfers.get(0).getAccount());
//        assertEquals(transferAmount1, mirrorTransaction.hbarTransfers.get(0).getAmount());
//        assertEquals(transferAccount2, mirrorTransaction.hbarTransfers.get(1).getAccount());
//        assertEquals(transferAmount2, mirrorTransaction.hbarTransfers.get(1).getAmount());
//
//        assertEquals(tokenTransfers.size(), mirrorTransaction.tokenTransfers.size());
//        assertEquals(transferAccount1, mirrorTransaction.tokenTransfers.get(0).account);
//        assertEquals(transferAmount1, mirrorTransaction.tokenTransfers.get(0).amount);
//        assertEquals(transferToken1, mirrorTransaction.tokenTransfers.get(0).tokenId);
//        assertEquals(transferAccount2, mirrorTransaction.tokenTransfers.get(1).account);
//        assertEquals(transferAmount2, mirrorTransaction.tokenTransfers.get(1).amount);
//        assertEquals(transferToken2, mirrorTransaction.tokenTransfers.get(1).tokenId);

        assertEquals(payerAccount, mirrorTransaction.payer());
        assertEquals(successful, mirrorTransaction.isSuccessful());
    }
}
