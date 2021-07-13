package com.hedera.demo.auction.app;

import com.google.errorprone.annotations.Var;
import com.hedera.demo.auction.app.api.RequestCreateAuctionAccount;
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
     * @param requestCreateAuctionAccount the object containing the auction to create
     * @return AccountId the created account id
     * @throws Exception in the event of an exception
     */
    public AccountId create(RequestCreateAuctionAccount requestCreateAuctionAccount) throws Exception {

        Client client = hederaClient.client();
        AccountCreateTransaction accountCreateTransaction = new AccountCreateTransaction();
        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();

        @Var Key keyList = requestCreateAuctionAccount.keylist.toKeyList();
        String masterKey = Optional.ofNullable(dotenv.get("MASTER_KEY")).orElse("");

        if ( ! StringUtils.isEmpty(masterKey)) {
            // check master key is not already in provided key list
            PublicKey masterPublicKey = PrivateKey.fromString(masterKey).getPublicKey();
            if (! requestCreateAuctionAccount.keylist.containsKey(masterPublicKey.toString())) {
                // not supplied in the key list, add it here
                Key fullKeyList = KeyList.of(keyList, masterPublicKey).setThreshold(1);
                keyList = fullKeyList;
            }
        }

        try {
            accountCreateTransaction.setKey(keyList);
            accountCreateTransaction.setInitialBalance(Hbar.from(requestCreateAuctionAccount.initialBalance));
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
}
