package com.hedera.demo.auction.node.app;

import com.hedera.hashgraph.sdk.AccountId;
import com.hedera.hashgraph.sdk.Client;
import com.hedera.hashgraph.sdk.Hbar;
import com.hedera.hashgraph.sdk.Status;
import com.hedera.hashgraph.sdk.TokenAssociateTransaction;
import com.hedera.hashgraph.sdk.TokenId;
import com.hedera.hashgraph.sdk.TransactionReceipt;
import com.hedera.hashgraph.sdk.TransactionResponse;
import lombok.extern.log4j.Log4j2;

import java.util.ArrayList;
import java.util.List;

@Log4j2
public class CreateTokenAssociation {

    private CreateTokenAssociation() {
    }

    /**
     * Associates a token with an account
     * @param tokenId the name of the token
     * @param accountId the symbol for the token
     * @throws Exception in the event of an exception
     */
    public static void associate(String tokenId, String accountId) throws Exception {
        Client client = HederaClient.getClient();
        client.setMaxTransactionFee(Hbar.from(100));
        TokenAssociateTransaction tokenAssociateTransaction = new TokenAssociateTransaction();
        List<TokenId> tokenIds = new ArrayList<>();
        tokenIds.add(TokenId.fromString(tokenId));
        tokenAssociateTransaction.setTokenIds(tokenIds);
        tokenAssociateTransaction.setTransactionMemo("Associate");
        tokenAssociateTransaction.setAccountId(AccountId.fromString(accountId));

        TransactionResponse response = tokenAssociateTransaction.execute(client);

        TransactionReceipt receipt = response.getReceipt(client);
        if (receipt.status != Status.SUCCESS) {
            log.error("Token association failed " + receipt.status);
            throw new Exception("Token association failed " + receipt.status);
        } else {
            log.info("Token " + tokenId + " associated with account " + accountId);
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            log.error("Invalid number of arguments supplied - tokenId and accountId are required");
        } else {
            log.info("Associating token to account");
            associate(args[0], args[1]);
        }
    }
}
