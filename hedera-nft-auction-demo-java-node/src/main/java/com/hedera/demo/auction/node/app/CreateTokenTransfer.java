package com.hedera.demo.auction.node.app;

import com.hedera.hashgraph.sdk.AccountId;
import com.hedera.hashgraph.sdk.Client;
import com.hedera.hashgraph.sdk.Hbar;
import com.hedera.hashgraph.sdk.Status;
import com.hedera.hashgraph.sdk.TokenId;
import com.hedera.hashgraph.sdk.TransactionReceipt;
import com.hedera.hashgraph.sdk.TransactionResponse;
import com.hedera.hashgraph.sdk.TransferTransaction;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class CreateTokenTransfer extends AbstractCreate {

    public CreateTokenTransfer() throws Exception {
        hederaClient = new HederaClient(env);
    }

    /**
     * Transfers the token to the account
     * @param tokenId the name of the token
     * @param accountId the symbol for the token
     * @throws Exception in the event of an exception
     */
    public void transfer(String tokenId, String accountId) throws Exception {
        Client client = hederaClient.client();
        client.setMaxTransactionFee(Hbar.from(100));

        TransferTransaction transferTransaction = new TransferTransaction();
        transferTransaction.setTransactionMemo("TransferToAuction");
        transferTransaction.addTokenTransfer(TokenId.fromString(tokenId), client.getOperatorAccountId(), -1);
        transferTransaction.addTokenTransfer(TokenId.fromString(tokenId), AccountId.fromString(accountId), 1);

        TransactionResponse response = transferTransaction.execute(client);

        TransactionReceipt receipt = response.getReceipt(client);
        if (receipt.status != Status.SUCCESS) {
            log.error("Token transfer failed " + receipt.status);
            throw new Exception("Token transfer failed " + receipt.status);
        } else {
            log.info("Token " + tokenId + " transferred to account " + accountId);
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            log.error("Invalid number of arguments supplied - tokenId and accountId are required");
        } else {
            log.info("Transferring token to account");
            CreateTokenTransfer createTokenTransfer = new CreateTokenTransfer();
            createTokenTransfer.transfer(args[0], args[1]);
        }
    }
}
