package com.hedera.demo.auction.node.app.scheduledoperations;

import com.hedera.hashgraph.sdk.AccountId;
import com.hedera.hashgraph.sdk.Client;
import com.hedera.hashgraph.sdk.PrecheckStatusException;
import com.hedera.hashgraph.sdk.PrivateKey;
import com.hedera.hashgraph.sdk.ReceiptStatusException;
import com.hedera.hashgraph.sdk.ScheduleCreateTransaction;
import com.hedera.hashgraph.sdk.ScheduleId;
import com.hedera.hashgraph.sdk.ScheduleSignTransaction;
import com.hedera.hashgraph.sdk.Status;
import com.hedera.hashgraph.sdk.Transaction;
import com.hedera.hashgraph.sdk.TransactionId;
import com.hedera.hashgraph.sdk.TransactionReceipt;
import com.hedera.hashgraph.sdk.TransactionReceiptQuery;
import com.hedera.hashgraph.sdk.TransactionResponse;
import lombok.extern.log4j.Log4j2;

import java.util.List;
import java.util.concurrent.TimeoutException;

@Log4j2
public class TransactionScheduler {
    private final Client client;
    private final AccountId accountId;
    private final PrivateKey privateKey;
    private final TransactionId transactionId;
    private final Transaction transaction;

    public TransactionScheduler(Client client, AccountId accountId, PrivateKey refundKey, TransactionId transactionId, Transaction transaction) {
        this.client = client;
        this.accountId = accountId;
        this.privateKey = refundKey;
        this.transactionId = transactionId;
        this.transaction = transaction;
    }
    public TransactionSchedulerResult issueScheduledTransaction() throws TimeoutException {
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

            try {
                TransactionReceipt receipt = response.getReceipt(client);
                return handleResponse(receipt.status);
            } catch (TimeoutException timeoutException) {
                log.error("Exception fetching receipt");
                log.error(timeoutException);
                throw timeoutException;
            } catch (ReceiptStatusException receiptStatusException) {
                return handleResponse(receiptStatusException.receipt.status);
            }

        } catch (PrecheckStatusException e) {
            return handleResponse(e.status);
        }
    }

    private TransactionSchedulerResult scheduleSignTransaction() throws TimeoutException {
        // the same tx has already been submitted, submit just the signature
        // get the receipt for the transaction
        try {
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

            try {
                response.getReceipt(client);
                return handleResponse(receipt.status);
            } catch (ReceiptStatusException receiptStatusException) {
                return handleResponse(receiptStatusException.receipt.status);
            }

        } catch (TimeoutException timeoutException) {
            log.error("Exception fetching receipt");
            log.error(timeoutException);
            throw timeoutException;
        } catch (PrecheckStatusException precheckStatusException) {
            return handleResponse(precheckStatusException.status);
        }
    }

    private TransactionSchedulerResult handleResponse(Status status) throws TimeoutException {
        //INVALID_TRANSACTION_START
        //
        switch (status) {
            case SUCCESS:
            case SCHEDULE_ALREADY_EXECUTED:
                return new TransactionSchedulerResult(/* success= */true, Status.SUCCESS);
            case IDENTICAL_SCHEDULE_ALREADY_CREATED:
                return scheduleSignTransaction();
            default:
                return new TransactionSchedulerResult(/* success= */false, status);
        }
    }

}
