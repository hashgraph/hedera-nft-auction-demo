package com.hedera.demo.auction.node.app.scheduledoperations;

import com.hedera.demo.auction.node.app.HederaClient;
import com.hedera.hashgraph.sdk.*;
import lombok.extern.log4j.Log4j2;

import java.util.List;
import java.util.concurrent.TimeoutException;

@Log4j2
public class TransactionScheduler {
    private final HederaClient hederaClient;
    private final AccountId auctionAccountId;
    private final PrivateKey refundKey;
    private final AccountId operatorId;
    private final PrivateKey operatorKey;
    private final TransactionId transactionId;
    private final Transaction transaction;

    public TransactionScheduler(HederaClient hederaClient, AccountId auctionAccountId, PrivateKey refundKey, TransactionId transactionId, Transaction transaction) {
        this.hederaClient = hederaClient;
        this.auctionAccountId = auctionAccountId;
        this.refundKey = refundKey;
        this.operatorKey = hederaClient.operatorPrivateKey();
        this.operatorId = hederaClient.operatorId();
        this.transactionId = transactionId;
        this.transaction = transaction;
    }
    public TransactionSchedulerResult issueScheduledTransaction() throws TimeoutException {
        hederaClient.setOperator(operatorId, operatorKey);

        // Schedule the transaction
        ScheduleCreateTransaction scheduleCreateTransaction = transaction.schedule()
                .setPayerAccountId(auctionAccountId)
                .setTransactionId(transactionId)
                //TODO: Fix list of node account ids
                .setNodeAccountIds(List.of(AccountId.fromString("0.0.3")))
                .freezeWith(hederaClient.client())
                .sign(refundKey)
                .sign(operatorKey);

        try {
            TransactionResponse response = scheduleCreateTransaction.execute(hederaClient.client());

            try {
                TransactionReceipt receipt = response.getReceipt(hederaClient.client());
                return handleResponse(receipt);
            } catch (TimeoutException timeoutException) {
                log.error("Exception fetching receipt");
                log.error(timeoutException);
                throw timeoutException;
            } catch (ReceiptStatusException receiptStatusException) {
                return handleResponse(receiptStatusException.receipt);
            }

        } catch (PrecheckStatusException e) {
            return new TransactionSchedulerResult(/* success= */false, e.status, null);
        }
    }

    private TransactionSchedulerResult scheduleSignTransaction(TransactionReceipt receipt) throws TimeoutException {
        // the same tx has already been submitted, submit just the signature
        // get the receipt for the transaction
        try {
            TransactionResponse response = new ScheduleSignTransaction()
                    .setScheduleId(receipt.scheduleId)
                    .setNodeAccountIds(List.of(AccountId.fromString("0.0.3")))
                    .freezeWith(hederaClient.client())
                    .sign(refundKey)
                    .execute(hederaClient.client());

            try {
                response.getReceipt(hederaClient.client());
                return handleResponse(receipt);
            } catch (ReceiptStatusException receiptStatusException) {
                return handleResponse(receipt);
            }

        } catch (TimeoutException timeoutException) {
            log.error("Exception fetching receipt");
            log.error(timeoutException);
            throw timeoutException;
        } catch (PrecheckStatusException precheckStatusException) {
            return new TransactionSchedulerResult(/* success= */false, precheckStatusException.status, null);
        }
    }

    private TransactionSchedulerResult handleResponse(TransactionReceipt receipt) throws TimeoutException {
        //INVALID_TRANSACTION_START
        //
        switch (receipt.status) {
            case SUCCESS:
            case SCHEDULE_ALREADY_EXECUTED:
                return new TransactionSchedulerResult(/* success= */true, Status.SUCCESS, receipt.scheduleId);
            case IDENTICAL_SCHEDULE_ALREADY_CREATED:
                return scheduleSignTransaction(receipt);
            default:
                return new TransactionSchedulerResult(/* success= */false, receipt.status, null);
        }
    }
}
