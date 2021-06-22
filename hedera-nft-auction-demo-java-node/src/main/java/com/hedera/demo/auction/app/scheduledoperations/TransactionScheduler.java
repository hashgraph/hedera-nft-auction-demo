package com.hedera.demo.auction.app.scheduledoperations;

import com.google.errorprone.annotations.Var;
import com.hedera.demo.auction.app.HederaClient;
import com.hedera.demo.auction.app.domain.Bid;
import com.hedera.demo.auction.app.repository.BidsRepository;
import com.hedera.hashgraph.sdk.AccountId;
import com.hedera.hashgraph.sdk.Hbar;
import com.hedera.hashgraph.sdk.PrecheckStatusException;
import com.hedera.hashgraph.sdk.ReceiptStatusException;
import com.hedera.hashgraph.sdk.ScheduleCreateTransaction;
import com.hedera.hashgraph.sdk.ScheduleSignTransaction;
import com.hedera.hashgraph.sdk.Status;
import com.hedera.hashgraph.sdk.Transaction;
import com.hedera.hashgraph.sdk.TransactionId;
import com.hedera.hashgraph.sdk.TransactionReceipt;
import com.hedera.hashgraph.sdk.TransactionResponse;
import com.hedera.hashgraph.sdk.TransferTransaction;
import io.github.cdimascio.dotenv.Dotenv;
import lombok.extern.log4j.Log4j2;

import javax.annotation.Nullable;
import java.sql.SQLException;
import java.util.Objects;
import java.util.concurrent.TimeoutException;

@Log4j2
public class TransactionScheduler {
    private final AccountId auctionAccountId;
    @Nullable
    private TransactionId transactionId;
    @Nullable
    private Transaction transaction;
    private final Dotenv env = Dotenv.load();

    public TransactionScheduler(AccountId auctionAccountId, TransactionId transactionId, Transaction transaction) {
        this.auctionAccountId = auctionAccountId;
        this.transactionId = transactionId;
        this.transaction = transaction;
    }

    public TransactionScheduler(AccountId auctionAccountId) {
        this.auctionAccountId = auctionAccountId;
        this.transactionId = null;
        this.transaction = null;
    }

    public void issueScheduledTransactionForRefund(Bid bid, BidsRepository bidsRepository, String memo) {
        // Create a transfer transaction for the refund
        // check the status of the bid isn't already refunded or issued
        try {
            Bid testBid = bidsRepository.getBidForTimestamp(bid.getTimestamp());
            if ((testBid != null) && ! testBid.isRefunded() && ! testBid.isRefundIssued()) {
                this.transactionId = TransactionId.generate(AccountId.fromString(Objects.requireNonNull(env.get("OPERATOR_ID"))));
                this.transactionId.setScheduled(true);
                String shortTransactionId = this.transactionId.toString().replace("?scheduled", "");

                TransferTransaction transferTransaction = new TransferTransaction();
                transferTransaction.setTransactionMemo(memo);
                transferTransaction.addHbarTransfer(auctionAccountId, Hbar.fromTinybars(-bid.getBidamount()));
                transferTransaction.addHbarTransfer(AccountId.fromString(bid.getBidderaccountid()), Hbar.fromTinybars(bid.getBidamount()));

                this.transaction = transferTransaction;

                @Var TransactionSchedulerResult transactionSchedulerResult = null;
                try {
                    transactionSchedulerResult = issueScheduledTransaction("Scheduled Auction Refund");
                    if (transactionSchedulerResult.success || transactionSchedulerResult.status == Status.NO_NEW_VALID_SIGNATURES) {
                        log.info("Refund transaction successfully scheduled (id {})", shortTransactionId);
                        log.info("setting bid to refund in progress (timestamp = {})", bid.getTimestamp());
                        try {
                            bidsRepository.setRefundIssued(bid.getTimestamp(), shortTransactionId);
                        } catch (SQLException e) {
                            log.error("Failed to set bid refund in progress (bid timestamp {})",bid.getTimestamp());
                            log.error(e, e);
                        }
                    } else {
                        log.error("Error issuing refund to bid - timestamp = {}", bid.getTimestamp());
                        log.error(transactionSchedulerResult.status);
                    }
                } catch (Exception e) {
                    log.error(e, e);
                }
            }
        } catch (SQLException e) {
            log.error(e, e);
        }
    }

    public TransactionSchedulerResult issueScheduledTransaction(String scheduleMemo) throws Exception {

        if (transaction == null) {
            throw new Exception("transaction to schedule is null");
        }
        // Schedule the transaction
        ScheduleCreateTransaction scheduleCreateTransaction = transaction.schedule()
                .setPayerAccountId(auctionAccountId)
                .setTransactionId(transactionId)
                .setScheduleMemo(scheduleMemo);

        HederaClient hederaClient = new HederaClient();
        try {
            log.debug("Creating scheduled transaction for pub key {}", hederaClient.operatorPublicKey().toString());
            TransactionResponse response = scheduleCreateTransaction.execute(hederaClient.client());

            try {
                TransactionReceipt receipt = response.getReceipt(hederaClient.client());
                hederaClient.client().close();
                return handleResponse(hederaClient, receipt);
            } catch (TimeoutException e) {
                log.error("TimeoutException fetching receipt");
                log.error(e, e);
                hederaClient.client().close();
                throw e;
            } catch (ReceiptStatusException receiptStatusException) {
                hederaClient.client().close();
                return handleResponse(hederaClient, receiptStatusException.receipt);
            }
        } catch (PrecheckStatusException e) {
            hederaClient.client().close();
            return new TransactionSchedulerResult(/* success= */false, e.status);
        }
    }

    private static TransactionSchedulerResult scheduleSignTransaction(HederaClient hederaClient, TransactionReceipt existingReceipt) throws TimeoutException {
        // the same tx has already been submitted, submit just the signature
        // get the receipt for the transaction
        try {
            log.debug("Signing schedule id {}  with key for public key {}", existingReceipt.scheduleId, hederaClient.operatorPublicKey().toString());
            ScheduleSignTransaction scheduleSignTransaction = new ScheduleSignTransaction()
                    .setScheduleId(existingReceipt.scheduleId);

            TransactionResponse response = scheduleSignTransaction.execute(hederaClient.client());

            try {
                TransactionReceipt receipt = response.getReceipt(hederaClient.client());
                return handleResponse(hederaClient, receipt);
            } catch (ReceiptStatusException receiptStatusException) {
                return handleResponse(hederaClient, receiptStatusException.receipt);
            }

        } catch (TimeoutException e) {
            log.error("Exception fetching receipt");
            log.error(e, e);
            throw e;
        } catch (PrecheckStatusException precheckStatusException) {
            return new TransactionSchedulerResult(/* success= */false, precheckStatusException.status);
        }
    }

    private static TransactionSchedulerResult handleResponse(HederaClient hederaClient, TransactionReceipt receipt) throws TimeoutException {
        //INVALID_TRANSACTION_START
        //
        log.info(receipt.status);
        switch (receipt.status) {
            case SUCCESS:
            case SCHEDULE_ALREADY_EXECUTED:
                return new TransactionSchedulerResult(/* success= */true, Status.SUCCESS, receipt.scheduleId);
            case IDENTICAL_SCHEDULE_ALREADY_CREATED:
                return scheduleSignTransaction(hederaClient, receipt);
            default:
                return new TransactionSchedulerResult(/* success= */false, receipt.status);
        }
    }
}
