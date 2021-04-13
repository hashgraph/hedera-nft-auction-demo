package com.hedera.demo.auction.node.test.integration;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public final class HederaJson {

    private HederaJson() {
        
    }
    public static JsonObject transaction(String account, String token, long amount) {
        JsonObject transaction = new JsonObject();
        transaction.put("charged_tx_fee", 84650);
        transaction.put("consensus_timestamp", "1617786661.662353000");
        transaction.put("max_fee", "100000000");
        transaction.put("memo_base64", "RGV2T3BzIFN5bnRoZXRpYyBUZXN0aW5n");
        transaction.put("name", "CRYPTOTRANSFER");
        transaction.put("node", "0.0.7");
        transaction.put("result", "SUCCESS");
        transaction.put("scheduled", false);
        transaction.put("transaction_hash", "KeUK9l64b1HShMmvFeQ+CCO2hBvzF5tUL8X2Bvxvsh+rcdNxxkQHEb3/nS6zsRwX");
        transaction.put("transaction_id", "0.0.90-1617786650-796134000");
        transaction.put("valid_duration_seconds", "120");
        transaction.put("valid_start_timestamp", "1617786650.796134000");

        JsonObject transfer = new JsonObject();
        transfer.put("amount", amount);
        transfer.put("token_id", token);
        transfer.put("account", account);

        JsonArray tokenTransfers = new JsonArray();
        tokenTransfers.add(transfer);

        transaction.put("token_transfers", tokenTransfers);

        return transaction;
    }
}
