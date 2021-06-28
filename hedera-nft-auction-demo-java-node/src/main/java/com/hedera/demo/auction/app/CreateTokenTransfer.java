package com.hedera.demo.auction.app;

import com.hedera.hashgraph.sdk.AccountId;
import com.hedera.hashgraph.sdk.Client;
import com.hedera.hashgraph.sdk.Hbar;
import com.hedera.hashgraph.sdk.PrivateKey;
import com.hedera.hashgraph.sdk.Status;
import com.hedera.hashgraph.sdk.TokenId;
import com.hedera.hashgraph.sdk.TransactionReceipt;
import com.hedera.hashgraph.sdk.TransactionResponse;
import com.hedera.hashgraph.sdk.TransferTransaction;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class CreateTokenTransfer extends AbstractCreate {

    public CreateTokenTransfer() throws Exception {
        super();
    }

    /**
     * Transfers the token to the account using the default client account and key
     * @param tokenId the name of the token
     * @param accountId the symbol for the token
     * @throws Exception in the event of an exception
     */
    public void transfer(String tokenId, String accountId) throws Exception {
        transfer(tokenId, accountId, hederaClient.operatorId(), hederaClient.operatorPrivateKey());
    }
    /**
     * Transfers the token to the account
     * @param tokenId the name of the token
     * @param accountId the symbol for the token
     * @param tokenOwnerAccount the account id of the token owner
     * @param tokenOwnerKey the private key of the token owner
     * @throws Exception in the event of an exception
     */
    public void transfer(String tokenId, String accountId, AccountId tokenOwnerAccount, PrivateKey tokenOwnerKey) throws Exception {

        Client client = hederaClient.client();
        client.setOperator(tokenOwnerAccount, tokenOwnerKey);
        client.setMaxTransactionFee(Hbar.from(100));

        TransferTransaction transferTransaction = new TransferTransaction();
        transferTransaction.setTransactionMemo("TransferToAuction");
        transferTransaction.addTokenTransfer(TokenId.fromString(tokenId), client.getOperatorAccountId(), -1);
        transferTransaction.addTokenTransfer(TokenId.fromString(tokenId), AccountId.fromString(accountId), 1);

        try {
            TransactionResponse response = transferTransaction.execute(client);

            TransactionReceipt receipt = response.getReceipt(client);
            if (receipt.status != Status.SUCCESS) {
                log.error("Token transfer failed {}", receipt.status);
                throw new Exception("Token transfer failed ".concat(receipt.status.toString()));
            } else {
                log.info("Token {} transferred to account {}", tokenId, accountId);
            }
        } catch (Exception e) {
            log.error(e, e);
            throw e;
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
