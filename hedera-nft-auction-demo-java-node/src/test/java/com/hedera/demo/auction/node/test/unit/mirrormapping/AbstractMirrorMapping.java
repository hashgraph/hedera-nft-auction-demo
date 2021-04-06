package com.hedera.demo.auction.node.test.unit.mirrormapping;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public abstract class AbstractMirrorMapping {
    String timestamp = "1617709857.681394000";
    String memo = "test memo";
    String memoBase64 = Base64.getEncoder().encodeToString(memo.getBytes(StandardCharsets.UTF_8));
    String result = "SUCCESS";
    boolean successful = true;
    boolean scheduled = true;
    String transactionHash = "f34f1a7baeb8f1c7dd7daf1e6f8f786bd7dc7fce786dbef86deefbf1cd9ad5ce5e7fbdbae5e734ddad1de5bf7df7bedad74f76ef4f5af5bf5f6f96f6e1e7f97f9e3bf5ae9bd747f9";
    String payerAccount = "0.0.11093";
    String transactionId = payerAccount.concat("-1617709846-952854833");

    String transferAccount1 = "0.0.4";
    long transferAmount1 = 20936;
    String transferToken1 = "0.0.1234";
    String transferAccount2 = "0.0.98";
    long transferAmount2 = 276786;
    String transferToken2 = "0.0.2345";

    String linkURL = "link here";

    JsonArray hbarTransfers = new JsonArray();
    JsonArray tokenTransfers = new JsonArray();

    private JsonObject baseTransaction() throws DecoderException {
        byte[] transactionHashBytes = Hex.decodeHex(transactionHash);
        String transactionHashBase64 = Base64.getEncoder().encodeToString(transactionHashBytes);

        JsonObject transaction = new JsonObject();

        transaction.put("consensus_timestamp", timestamp);
        transaction.put("memo_base64", memoBase64);
        transaction.put("result", result);
        transaction.put("scheduled", scheduled);
        transaction.put("transaction_hash", transactionHashBase64);
        transaction.put("transaction_id", transactionId);

        return transaction;

    }
    public JsonObject hederaTransaction() throws DecoderException {
        JsonObject transaction = baseTransaction();

        hbarTransfers.add(hederaHbarTransfer(transferAccount1, transferAmount1));
        hbarTransfers.add(hederaHbarTransfer(transferAccount2, transferAmount2));
        transaction.put("transfers", hbarTransfers);

        tokenTransfers.add(hederaTokenTransfer(transferAccount1, transferAmount1, transferToken1));
        tokenTransfers.add(hederaTokenTransfer(transferAccount2, transferAmount2, transferToken2));
        transaction.put("token_transfers", tokenTransfers);


        return transaction;
    }

    public JsonObject hederaHbarTransfer(String account, long amount) {
        JsonObject transfer = new JsonObject();
        transfer.put("account", account);
        transfer.put("amount", amount);
        return transfer;

    }

    public JsonObject hederaTokenTransfer(String account, long amount, String tokenId) {
        JsonObject tokenTransfer = new JsonObject();
        tokenTransfer.put("account", account);
        tokenTransfer.put("amount", amount);
        tokenTransfer.put("token_id", tokenId);
        return tokenTransfer;
    }

    public JsonObject kabutoTransaction() throws DecoderException {
        JsonObject transaction = baseTransaction();

//        hbarTransfers.add(hederaHbarTransfer(transferAccount1, transferAmount1));
//        hbarTransfers.add(hederaHbarTransfer(transferAccount2, transferAmount2));
//        transaction.put("transfers", hbarTransfers);
//
//        tokenTransfers.add(hederaTokenTransfer(transferAccount1, transferAmount1, transferToken1));
//        tokenTransfers.add(hederaTokenTransfer(transferAccount2, transferAmount2, transferToken2));
//        transaction.put("token_transfers", tokenTransfers);

        return transaction;
    }

    public JsonObject dragonglassTransaction() throws DecoderException {
        JsonObject transaction = baseTransaction();

//        hbarTransfers.add(hederaHbarTransfer(transferAccount1, transferAmount1));
//        hbarTransfers.add(hederaHbarTransfer(transferAccount2, transferAmount2));
//        transaction.put("transfers", hbarTransfers);
//
//        tokenTransfers.add(hederaTokenTransfer(transferAccount1, transferAmount1, transferToken1));
//        tokenTransfers.add(hederaTokenTransfer(transferAccount2, transferAmount2, transferToken2));
//        transaction.put("token_transfers", tokenTransfers);

        return transaction;
    }
}
