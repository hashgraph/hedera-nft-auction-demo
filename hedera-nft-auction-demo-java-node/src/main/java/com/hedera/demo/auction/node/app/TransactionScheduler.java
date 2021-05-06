package com.hedera.demo.auction.node.app;

import com.hedera.hashgraph.sdk.*;

import java.util.List;
import java.util.concurrent.TimeoutException;

public class TransactionScheduler {
    public static void issueScheduledTransaction(Client client, AccountId accountId, PrivateKey privateKey, TransactionId transactionId, Transaction transaction) throws ReceiptStatusException, PrecheckStatusException, TimeoutException {
        String shortTransactionId = transactionId.toString().replace("?scheduled", "");

        client.setOperator(accountId, privateKey);

        // Schedule the transaction
        ScheduleCreateTransaction scheduleCreateTransaction = transaction.schedule()
                .setPayerAccountId(accountId)
                .setTransactionId(transactionId)
                //TODO: Fix list of node account ids
                .setNodeAccountIds(List.of(AccountId.fromString("0.0.3")))
                .freezeWith(client)
                .sign(privateKey);

        try {
            TransactionResponse response = scheduleCreateTransaction.execute(client);
            TransactionReceipt receipt = response.getReceipt(client);

            if (receipt.status != Status.SUCCESS) {
                if (receipt.status == Status.IDENTICAL_SCHEDULE_ALREADY_CREATED) {
                    scheduleSignTransaction(client, privateKey, transactionId, shortTransactionId);
                }
            }
        } catch (PrecheckStatusException e) {
            if (e.status == Status.IDENTICAL_SCHEDULE_ALREADY_CREATED) {
                scheduleSignTransaction(client, privateKey, transactionId, shortTransactionId);
            } else {
                throw e;
            }
        } catch (TimeoutException | ReceiptStatusException e) {
            throw e;
        }
    }

    private static void scheduleSignTransaction(Client client, PrivateKey privateKey, TransactionId transactionId, String shortTransactionId) throws TimeoutException, PrecheckStatusException, ReceiptStatusException {
        // the same tx has already been submitted, submit just the signature
        // get the receipt for the transaction
        TransactionReceipt receipt = new TransactionReceiptQuery()
                .setTransactionId(transactionId)
                .execute(client);
        ScheduleId scheduleId = receipt.scheduleId;

        TransactionResponse response = new ScheduleSignTransaction()
                .setScheduleId(scheduleId)
                .setNodeAccountIds(List.of(AccountId.fromString("0.0.3")))
                .freezeWith(client)
                .sign(privateKey)
                .execute(client);

        response.getReceipt(client);
    }
}
