package com.hedera.demo.auction.exerciser;

import com.hedera.demo.auction.app.HederaClient;
import com.hedera.hashgraph.sdk.AccountCreateTransaction;
import com.hedera.hashgraph.sdk.Client;
import com.hedera.hashgraph.sdk.Hbar;
import com.hedera.hashgraph.sdk.PrivateKey;
import com.hedera.hashgraph.sdk.Status;
import com.hedera.hashgraph.sdk.TransactionReceipt;
import com.hedera.hashgraph.sdk.TransactionResponse;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class CreateAccount {

    public CreateAccount() {
    }

    /**
     * Creates an account with an initial balance
     * @param initialBalance the initial balance for the new account
     * @throws Exception in the event of an exception
     */
    public void create(long initialBalance) throws Exception {

        HederaClient hederaClient = new HederaClient();
        Client client = hederaClient.client();

        PrivateKey privateKey = PrivateKey.generate();

        AccountCreateTransaction accountCreateTransaction = new AccountCreateTransaction();

        try {
            accountCreateTransaction.setKey(privateKey.getPublicKey());
            accountCreateTransaction.setInitialBalance(Hbar.from(initialBalance));
            TransactionResponse response = accountCreateTransaction.execute(client);

            TransactionReceipt receipt = response.getReceipt(client);
            if (receipt.status != Status.SUCCESS) {
                log.error("Account creation failed {}", receipt.status);
                throw new Exception("Account creation failed ".concat(receipt.status.toString()));
            } else {
                log.info("Account created {}", receipt.accountId.toString());
                log.info("Private key is {}", privateKey.toString());
                log.info("Public key is {}", privateKey.getPublicKey().toString());
            }
        } catch (Exception e) {
            log.error(e, e);
            throw e;
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            log.error("Invalid number of arguments supplied - initial balance");
            return;
        } else {
            log.info("Creating account");
            CreateAccount createAccount = new CreateAccount();
            createAccount.create(Long.parseLong(args[0]));
        }
    }
}
