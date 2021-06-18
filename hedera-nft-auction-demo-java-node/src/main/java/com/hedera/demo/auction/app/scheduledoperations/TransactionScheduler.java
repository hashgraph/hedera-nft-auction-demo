package com.hedera.demo.auction.app.scheduledoperations;

import com.hedera.demo.auction.app.HederaClient;
import com.hedera.demo.auction.app.domain.Bid;
import com.hedera.demo.auction.app.repository.BidsRepository;
import com.hedera.hashgraph.sdk.AccountId;
import com.hedera.hashgraph.sdk.PrecheckStatusException;
import com.hedera.hashgraph.sdk.PrivateKey;
import com.hedera.hashgraph.sdk.ReceiptStatusException;
import com.hedera.hashgraph.sdk.ScheduleCreateTransaction;
import com.hedera.hashgraph.sdk.ScheduleSignTransaction;
import com.hedera.hashgraph.sdk.Status;
import com.hedera.hashgraph.sdk.Transaction;
import com.hedera.hashgraph.sdk.TransactionId;
import com.hedera.hashgraph.sdk.TransactionReceipt;
import com.hedera.hashgraph.sdk.TransactionResponse;
import lombok.extern.log4j.Log4j2;

import java.sql.SQLException;
import java.util.concurrent.TimeoutException;

@Log4j2
public class TransactionScheduler {
    private final HederaClient hederaClient;
    private final AccountId auctionAccountId;
    private final AccountId operatorId;
    private final PrivateKey operatorKey;
    private final TransactionId transactionId;
    private final Transaction transaction;

    public TransactionScheduler(HederaClient hederaClient, AccountId auctionAccountId, TransactionId transactionId, Transaction transaction) {
        this.hederaClient = hederaClient;
        this.auctionAccountId = auctionAccountId;
        this.operatorKey = hederaClient.operatorPrivateKey();
        this.operatorId = hederaClient.operatorId();
        this.transactionId = transactionId;
        this.transaction = transaction;
    }
    public void issueScheduledTransactionForRefund(Bid bid, BidsRepository bidsRepository, String shortTransactionId) throws TimeoutException {
        TransactionSchedulerResult transactionSchedulerResult = issueScheduledTransaction();
        if (transactionSchedulerResult.success || transactionSchedulerResult.status == Status.NO_NEW_VALID_SIGNATURES) {
            log.info("Refund transaction successfully scheduled (id " + shortTransactionId + ")");
            log.info("setting bid to refund in progress (timestamp = " + bid.getTimestamp() + ")");
            try {
                bidsRepository.setRefundIssued(bid.getTimestamp(), shortTransactionId);
            } catch (SQLException sqlException) {
                log.error("Failed to set bid refund in progress (bid timestamp " + bid.getTimestamp() + ")");
                log.error(sqlException);
            }
        } else {
            log.error("Error issuing refund to bid - timestamp = " + bid.getTimestamp());
            log.error(transactionSchedulerResult.status);
        }
    }

    public TransactionSchedulerResult issueScheduledTransaction() throws TimeoutException {
        hederaClient.setOperator(operatorId, operatorKey);

        // Schedule the transaction
        ScheduleCreateTransaction scheduleCreateTransaction = transaction.schedule()
                .setPayerAccountId(auctionAccountId)
                .setTransactionId(transactionId)
                .setScheduleMemo("Scheduled Refund");

        try {
            log.debug("Creating scheduled transaction for pub key " + hederaClient.operatorPublicKey().toString());
            TransactionResponse response = scheduleCreateTransaction.execute(hederaClient.client());

            try {
                TransactionReceipt receipt = response.getReceipt(hederaClient.client());
                return handleResponse(receipt);
            } catch (TimeoutException timeoutException) {
                log.error("TimeoutException fetching receipt");
                log.error(timeoutException);
                throw timeoutException;
            } catch (ReceiptStatusException receiptStatusException) {
                return handleResponse(receiptStatusException.receipt);
            }

        } catch (PrecheckStatusException e) {
            return new TransactionSchedulerResult(/* success= */false, e.status);
        }
    }

    private TransactionSchedulerResult scheduleSignTransaction(TransactionReceipt existingReceipt) throws TimeoutException {
        // the same tx has already been submitted, submit just the signature
        // get the receipt for the transaction
        try {
            log.debug("Signing schedule id " + existingReceipt.scheduleId + " with key for public key " + hederaClient.operatorPublicKey().toString());
            ScheduleSignTransaction scheduleSignTransaction = new ScheduleSignTransaction()
                    .setScheduleId(existingReceipt.scheduleId);

            TransactionResponse response = scheduleSignTransaction.execute(hederaClient.client());

            try {
                TransactionReceipt receipt = response.getReceipt(hederaClient.client());
                return handleResponse(receipt);
            } catch (ReceiptStatusException receiptStatusException) {
                return handleResponse(receiptStatusException.receipt);
            }

        } catch (TimeoutException timeoutException) {
            log.error("Exception fetching receipt");
            log.error(timeoutException);
            throw timeoutException;
        } catch (PrecheckStatusException precheckStatusException) {
            return new TransactionSchedulerResult(/* success= */false, precheckStatusException.status);
        }
    }

    private TransactionSchedulerResult handleResponse(TransactionReceipt receipt) throws TimeoutException {
        //INVALID_TRANSACTION_START
        //
        log.info(receipt.status);
        switch (receipt.status) {
            case SUCCESS:
            case SCHEDULE_ALREADY_EXECUTED:
                return new TransactionSchedulerResult(/* success= */true, Status.SUCCESS, receipt.scheduleId);
            case IDENTICAL_SCHEDULE_ALREADY_CREATED:
                return scheduleSignTransaction(receipt);
            default:
                return new TransactionSchedulerResult(/* success= */false, receipt.status);
        }
    }
}
