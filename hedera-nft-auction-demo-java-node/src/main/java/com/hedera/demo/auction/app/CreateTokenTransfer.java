package com.hedera.demo.auction.app;

import com.hedera.demo.auction.app.api.RequestTokenTransfer;
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
     * @param requestTokenTransfer the token and account to use in the transfer
     * @throws Exception in the event of an exception
     */
    public void transfer(RequestTokenTransfer requestTokenTransfer) throws Exception {
        transfer(requestTokenTransfer, hederaClient.operatorId(), hederaClient.operatorPrivateKey());
    }
    /**
     * Transfers the token to the account
     * @param requestTokenTransfer the token and account to use in the transfer
     * @param tokenOwnerAccount the account id of the token owner
     * @param tokenOwnerKey the private key of the token owner
     * @throws Exception in the event of an exception
     */
    public void transfer(RequestTokenTransfer requestTokenTransfer, AccountId tokenOwnerAccount, PrivateKey tokenOwnerKey) throws Exception {

        Client client = hederaClient.client();
        client.setOperator(tokenOwnerAccount, tokenOwnerKey);
        client.setMaxTransactionFee(Hbar.from(100));

        TransferTransaction transferTransaction = new TransferTransaction();
        transferTransaction.setTransactionMemo("TransferToAuction");
        transferTransaction.addTokenTransfer(TokenId.fromString(requestTokenTransfer.tokenid), client.getOperatorAccountId(), -1);
        transferTransaction.addTokenTransfer(TokenId.fromString(requestTokenTransfer.tokenid), AccountId.fromString(requestTokenTransfer.auctionaccountid), 1);

        try {
            TransactionResponse response = transferTransaction.execute(client);

            TransactionReceipt receipt = response.getReceipt(client);
            if (receipt.status != Status.SUCCESS) {
                log.error("Token transfer failed {}", receipt.status);
                throw new Exception("Token transfer failed ".concat(receipt.status.toString()));
            } else {
                log.info("Token {} transferred to account {}", requestTokenTransfer.tokenid, requestTokenTransfer.auctionaccountid);
            }
        } catch (Exception e) {
            log.error(e, e);
            throw e;
        }
    }
}
