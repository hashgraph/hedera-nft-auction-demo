package com.hedera.demo.auction.node.app;

import com.google.errorprone.annotations.Var;
import com.hedera.hashgraph.sdk.AccountCreateTransaction;
import com.hedera.hashgraph.sdk.AccountId;
import com.hedera.hashgraph.sdk.Client;
import com.hedera.hashgraph.sdk.Hbar;
import com.hedera.hashgraph.sdk.KeyList;
import com.hedera.hashgraph.sdk.PublicKey;
import com.hedera.hashgraph.sdk.Status;
import com.hedera.hashgraph.sdk.TransactionReceipt;
import com.hedera.hashgraph.sdk.TransactionResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import lombok.extern.log4j.Log4j2;
import org.jooq.tools.StringUtils;

@Log4j2
public class CreateAuctionAccount {

    private CreateAuctionAccount() {
    }

    /**
     * Creates an auction account with an optional set of keys
     * @param initialBalance the initial balance for the new account
     * @param keys an array of public keys, if none supplied, the operator's public key is used
     * @throws Exception in the event of an exception
     */
    public static AccountId create(long initialBalance, String keys) throws Exception {

        Client client = HederaClient.getClient();
        AccountCreateTransaction accountCreateTransaction = new AccountCreateTransaction();
        KeyList keyList = new KeyList();
        if (StringUtils.isEmpty(keys)) {
            log.info("No public key provided, defaulting to operator public key");
            keyList.add(client.getOperatorPublicKey());
        } else {
            JsonObject jsonKeys = new JsonObject(keys);
            JsonArray keysJson = jsonKeys.getJsonArray("keylist");
            if ((keysJson == null) || (keysJson.size() == 0)) {
                log.info("No public key provided, defaulting to operator public key");
                keyList.add(client.getOperatorPublicKey());
            } else {
                for (Object keyJsonItem : keysJson) {
                    JsonObject keyJson = JsonObject.mapFrom(keyJsonItem);
                    @Var KeyList thresholdKey = new KeyList();
                    if (keyJson.containsKey("threshold")) {
                        thresholdKey = KeyList.withThreshold(keyJson.getInteger("threshold"));
                    }
                    JsonArray keysInList = keyJson.getJsonArray("keys");
                    for (Object keyInList : keysInList) {
                        JsonObject oneKey = JsonObject.mapFrom(keyInList);
                        thresholdKey.add(PublicKey.fromString(oneKey.getString("key")));
                    }
                    keyList.add(thresholdKey);
                }
            }
        }
        log.info(keyList.toString());

        accountCreateTransaction.setKey(keyList);
        accountCreateTransaction.setInitialBalance(Hbar.from(initialBalance));
        TransactionResponse response = accountCreateTransaction.execute(client);

        TransactionReceipt receipt = response.getReceipt(client);
        if (receipt.status != Status.SUCCESS) {
            log.error("Account creation failed " + receipt.status);
            throw new Exception("Account creation failed " + receipt.status);
        } else {
            log.info("Account created " + receipt.accountId.toString());
        }
        return receipt.accountId;
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            log.error("Invalid number of arguments supplied - initial balance, keys");
            return;
        } else {
            @Var String keys = "";
            if (args.length == 2) {
                keys = args[1];
            }
            log.info("Creating account");

            create(Long.parseLong(args[0]), keys);
        }
    }
}
