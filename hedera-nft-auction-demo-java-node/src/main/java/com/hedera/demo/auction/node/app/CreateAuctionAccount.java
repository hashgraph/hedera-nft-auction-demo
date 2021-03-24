package com.hedera.demo.auction.node.app;

import com.google.errorprone.annotations.Var;
import com.hedera.hashgraph.sdk.AccountCreateTransaction;
import com.hedera.hashgraph.sdk.AccountId;
import com.hedera.hashgraph.sdk.Client;
import com.hedera.hashgraph.sdk.Hbar;
import com.hedera.hashgraph.sdk.Key;
import com.hedera.hashgraph.sdk.KeyList;
import com.hedera.hashgraph.sdk.PublicKey;
import com.hedera.hashgraph.sdk.Status;
import com.hedera.hashgraph.sdk.TransactionReceipt;
import com.hedera.hashgraph.sdk.TransactionResponse;
import lombok.extern.log4j.Log4j2;

import java.util.Arrays;

@Log4j2
public class CreateAuctionAccount {

    private CreateAuctionAccount() {
    }

    /**
     * Creates an auction account with an optional set of keys
     * @param initialBalance the initial balance for the new account
     * @param minThreshold the threshold for the account's threshold key
     * @param keys an array of public keys, if none supplied, the operator's public key is used
     * @throws Exception in the event of an exception
     */
    public static AccountId create(long initialBalance, int minThreshold, @Var String[] keys) throws Exception {
        Client client = HederaClient.getClient();
        AccountCreateTransaction accountCreateTransaction = new AccountCreateTransaction();
        KeyList thresholdKey = KeyList.withThreshold(minThreshold);
        if (keys.length == 0) {
            log.info("No public key provided, defaulting to operator public key");
            keys = new String[]{client.getOperatorPublicKey().toString()};
        }
        for (String key : keys) {
            Key pubKey = PublicKey.fromString(key);
            thresholdKey.add(pubKey);
        }
        accountCreateTransaction.setKey(thresholdKey);
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
        if (args.length < 2) {
            log.error("Invalid number of arguments supplied - initial balance, minThreshold");
            return;
        } else {
            @Var String[] keys = {};
            if (args.length == 3) {
                keys = Arrays.copyOfRange(args, 2, args.length);
            } else {
                Client client = HederaClient.getClient();
                log.info("No public key provided, defaulting to operator public key");
                keys = new String[] { client.getOperatorPublicKey().toString() };
            }
            log.info("Creating account");

            create(Long.parseLong(args[0]), Integer.parseInt(args[1]), keys);
        }
    }
}
