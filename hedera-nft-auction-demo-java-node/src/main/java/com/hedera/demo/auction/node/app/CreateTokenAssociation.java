package com.hedera.demo.auction.node.app;

import com.google.errorprone.annotations.Var;
import com.hedera.hashgraph.sdk.*;
import lombok.extern.log4j.Log4j2;

import java.util.ArrayList;
import java.util.List;

@Log4j2
public class CreateTokenAssociation {

    private CreateTokenAssociation() {
    }

    /**
     * Associates a token with an account and transfers the token to the account
     * @param tokenId the name of the token
     * @param accountId the symbol for the token
     * @throws Exception in the event of an exception
     */
    public static void associateAndTransfer(String tokenId, String accountId) throws Exception {
        Client client = HederaClient.getClient();
        client.setMaxTransactionFee(Hbar.from(100));
        TokenAssociateTransaction tokenAssociateTransaction = new TokenAssociateTransaction();
        List<TokenId> tokenIds = new ArrayList<>();
        tokenIds.add(TokenId.fromString(tokenId));
        tokenAssociateTransaction.setTokenIds(tokenIds);
        tokenAssociateTransaction.setTransactionMemo("Associate");
        tokenAssociateTransaction.setAccountId(AccountId.fromString(accountId));

        @Var TransactionResponse response = tokenAssociateTransaction.execute(client);

        @Var TransactionReceipt receipt = response.getReceipt(client);
        if (receipt.status != Status.SUCCESS) {
            log.error("Token association failed " + receipt.status);
            throw new Exception("Token association failed " + receipt.status);
        } else {
            log.info("Token " + tokenId + " associated with account " + accountId);
        }

        TransferTransaction transferTransaction = new TransferTransaction();
        transferTransaction.setTransactionMemo("TransferToAuction");
        transferTransaction.addTokenTransfer(TokenId.fromString(tokenId), client.getOperatorAccountId(), -1);
        transferTransaction.addTokenTransfer(TokenId.fromString(tokenId), AccountId.fromString(accountId), 1);

        response = transferTransaction.execute(client);

        receipt = response.getReceipt(client);
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
            log.info("Associating token to account and transferring");
            associateAndTransfer(args[0], args[1]);
        }
    }
}
