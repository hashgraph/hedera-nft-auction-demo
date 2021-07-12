package com.hedera.demo.auction.app;

import com.google.errorprone.annotations.Var;
import com.hedera.hashgraph.sdk.AccountCreateTransaction;
import com.hedera.hashgraph.sdk.AccountId;
import com.hedera.hashgraph.sdk.Client;
import com.hedera.hashgraph.sdk.Hbar;
import com.hedera.hashgraph.sdk.Key;
import com.hedera.hashgraph.sdk.KeyList;
import com.hedera.hashgraph.sdk.PrivateKey;
import com.hedera.hashgraph.sdk.PublicKey;
import com.hedera.hashgraph.sdk.Status;
import com.hedera.hashgraph.sdk.TransactionReceipt;
import com.hedera.hashgraph.sdk.TransactionResponse;
import io.github.cdimascio.dotenv.Dotenv;
import io.vertx.core.json.JsonObject;
import lombok.extern.log4j.Log4j2;
import org.jooq.tools.StringUtils;

import java.util.Optional;

@Log4j2
public class CreateAuctionAccount extends AbstractCreate {

    public CreateAuctionAccount() throws Exception {
        super();
    }

    /**
     * Creates an auction account with an optional set of keys
     * @param initialBalance the initial balance for the new account
     * @param keys an array of public keys, if none supplied, the operator's public key is used
     * @return AccountId the created account id
     * @throws Exception in the event of an exception
     */
    public AccountId create(long initialBalance, String keys) throws Exception {

        Client client = hederaClient.client();
        AccountCreateTransaction accountCreateTransaction = new AccountCreateTransaction();
        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();

        @Var Key keyList;
        if (StringUtils.isEmpty(keys)) {
            log.info("No public key provided, defaulting to operator public key");
            keyList = client.getOperatorPublicKey();
        } else {
            JsonObject jsonObject = new JsonObject(keys);
            if (jsonObject.containsKey("keyList")) {
                AuctionKey auctionKey = jsonObject.mapTo(AuctionKey.class);
                if (auctionKey.auctionKeyList.auctionKeys.size() < auctionKey.auctionKeyList.threshold) {
                    throw new Exception("auction account threshold greater than number of keys");
                }
                if (auctionKey.isValid()) {
                    keyList = auctionKey.toKeyList();
                } else {
                    log.info("No public key provided, defaulting to operator public key");
                    keyList = client.getOperatorPublicKey();
                }
            } else {
                log.info("No public key provided, defaulting to operator public key");
                keyList = client.getOperatorPublicKey();
            }
            String masterKey = Optional.ofNullable(dotenv.get("MASTER_KEY")).orElse("");

            if ( ! StringUtils.isEmpty(masterKey)) {
                // check master key is not already in provided key list
                PublicKey masterPublicKey = PrivateKey.fromString(masterKey).getPublicKey();
                if ( ! keys.toUpperCase().contains(masterPublicKey.toString().toUpperCase())) {
                    // not supplied in the key list, add it here
                    Key fullKeyList = KeyList.of(keyList, masterPublicKey).setThreshold(1);
                    keyList = fullKeyList;
                }
            }
        }

        try {
            accountCreateTransaction.setKey(keyList);
            accountCreateTransaction.setInitialBalance(Hbar.from(initialBalance));
            TransactionResponse response = accountCreateTransaction.execute(client);

            TransactionReceipt receipt = response.getReceipt(client);
            if (receipt.status != Status.SUCCESS) {
                log.error("Account creation failed {}", receipt.status);
                throw new Exception("Account creation failed ".concat(receipt.status.toString()));
            } else {
                log.info("Account created {}", receipt.accountId.toString());
            }
            return receipt.accountId;
        } catch (Exception e) {
            log.error(e, e);
            throw e;
        }
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
            CreateAuctionAccount createAuctionAccount = new CreateAuctionAccount();
            createAuctionAccount.create(Long.parseLong(args[0]), keys);
        }
    }
}
