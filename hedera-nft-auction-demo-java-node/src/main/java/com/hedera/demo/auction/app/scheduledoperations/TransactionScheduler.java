package com.hedera.demo.auction.app.scheduledoperations;

import com.google.errorprone.annotations.Var;
import com.hedera.demo.auction.app.HederaClient;
import com.hedera.demo.auction.app.domain.Bid;
import com.hedera.demo.auction.app.repository.BidsRepository;
import com.hedera.hashgraph.sdk.AccountId;
import com.hedera.hashgraph.sdk.Hbar;
import com.hedera.hashgraph.sdk.PrecheckStatusException;
import com.hedera.hashgraph.sdk.PublicKey;
import com.hedera.hashgraph.sdk.ReceiptStatusException;
import com.hedera.hashgraph.sdk.ScheduleCreateTransaction;
import com.hedera.hashgraph.sdk.ScheduleId;
import com.hedera.hashgraph.sdk.ScheduleSignTransaction;
import com.hedera.hashgraph.sdk.Status;
import com.hedera.hashgraph.sdk.Transaction;
import com.hedera.hashgraph.sdk.TransactionId;
import com.hedera.hashgraph.sdk.TransactionReceipt;
import com.hedera.hashgraph.sdk.TransactionResponse;
import com.hedera.hashgraph.sdk.TransferTransaction;
import io.github.cdimascio.dotenv.Dotenv;
import lombok.extern.log4j.Log4j2;
import org.jooq.tools.StringUtils;

import javax.annotation.Nullable;
import java.sql.SQLException;
import java.util.Objects;
import java.util.concurrent.TimeoutException;

/**
 * Schedules a transaction for execution on Hedera, alternatively if the transaction is already scheduled, adds a signature to the schedule
 */
@Log4j2
public class TransactionScheduler {
    private final AccountId auctionAccountId;
    @Nullable
    private TransactionId transactionId;
    @Nullable
    private Transaction transaction;
    private final Dotenv env = Dotenv.configure().ignoreIfMissing().load();

    /**
     * Constructor
     *
     * @param auctionAccountId the auction account id
     * @param transactionId the transaction id
     * @param transaction the transaction to schedule
     */
    public TransactionScheduler(AccountId auctionAccountId, TransactionId transactionId, Transaction transaction) {
        this.auctionAccountId = auctionAccountId;
        this.transactionId = transactionId;
        this.transaction = transaction;
    }

    /**
     * Constructor
     *
     * @param auctionAccountId the auction account id
     */
    public TransactionScheduler(AccountId auctionAccountId) {
        this.auctionAccountId = auctionAccountId;
        this.transactionId = null;
        this.transaction = null;
    }

    /**
     * Issues a scheduled transaction in the context of a bid refund.
     *
     * If the creation of the scheduled transaction is successful, has no new valid signatures or is a duplicate, sets the Bid's refund status to REFUND ISSUED if a scheduled id is known
     * Otherwise, the bid's refund status is set to ERROR
     * If the creation of the scheduled transaction fails, the bid's refund status is set to ERROR
     *
     * @param bid the Bid object to refund
     * @param bidsRepository the repository for bids on the database
     * @param memo the memo to associate with the scheduled transaction
     */
    public void issueScheduledTransactionForRefund(Bid bid, BidsRepository bidsRepository, String memo) {
        // Create a transfer transaction for the refund
        // check the status of the bid isn't already refunded, issued or in error
        try {
            Bid testBid = bidsRepository.getBidForTimestamp(bid.getTimestamp());
            if ((testBid != null) && ! testBid.isRefunded() && ! testBid.isRefundIssued() && ! testBid.isRefundError()) {
                this.transactionId = TransactionId.generate(AccountId.fromString(Objects.requireNonNull(env.get("OPERATOR_ID"))));
                this.transactionId.setScheduled(true);
                String shortTransactionId = this.transactionId.toString().replace("?scheduled", "");

                TransferTransaction transferTransaction = new TransferTransaction();
                transferTransaction.setTransactionMemo(memo);
                transferTransaction.addHbarTransfer(this.auctionAccountId, Hbar.fromTinybars(-bid.getBidamount()));
                transferTransaction.addHbarTransfer(AccountId.fromString(bid.getBidderaccountid()), Hbar.fromTinybars(bid.getBidamount()));

                this.transaction = transferTransaction;

                @Var TransactionSchedulerResult transactionSchedulerResult = null;
                try {
                    transactionSchedulerResult = issueScheduledTransaction("Scheduled Auction Refund");
                    if (transactionSchedulerResult.success
                            || transactionSchedulerResult.status == Status.NO_NEW_VALID_SIGNATURES
                            || transactionSchedulerResult.status == Status.DUPLICATE_TRANSACTION) {
                        log.info("Refund transaction successfully scheduled (scheduleId {}, transactionId {})", transactionSchedulerResult.getScheduleId(), shortTransactionId);
                        log.info("setting bid to refund issued (timestamp = {})", bid.getTimestamp());
                        try {
                            if (StringUtils.isEmpty(transactionSchedulerResult.getScheduleId())) {
                                log.error("empty schedule id for bid transaction id {}", bid.getTransactionid());
                                bidsRepository.setRefundError(bid.getTransactionid());
                            } else {
                                bidsRepository.setRefundIssued(bid.getTimestamp(), shortTransactionId, transactionSchedulerResult.getScheduleId());
                            }
                        } catch (SQLException e) {
                            log.error("Failed to set bid refund issued (bid timestamp {})",bid.getTimestamp(), e);
                        }
                    } else {
                        log.error("Error issuing refund to bid - timestamp = {} status {}", bid.getTimestamp(), transactionSchedulerResult.status);
                        bidsRepository.setRefundError(bid.getTransactionid());
                    }
                } catch (Exception e) {
                    log.error(e, e);
                }
            }
        } catch (SQLException e) {
            log.error(e, e);
        }
    }

    /**
     * Issues a generic transaction to be scheduled
     *
     * @param scheduleMemo the memo of the scheduled transaction
     * @return TransactionSchedulerResult the result of the scheduling operation
     * @throws Exception in the event of an error
     */
    public TransactionSchedulerResult issueScheduledTransaction(String scheduleMemo) throws Exception {

        if (this.transaction == null) {
            throw new Exception("transaction to schedule is null");
        } else {
            // Schedule the transaction
            ScheduleCreateTransaction scheduleCreateTransaction = transaction.schedule()
                    .setPayerAccountId(this.auctionAccountId)
                    .setTransactionId(this.transactionId)
                    .setScheduleMemo(scheduleMemo);

            HederaClient hederaClient = new HederaClient();
            try {
                log.debug("Creating scheduled transaction for pub key {}", shortKey(hederaClient.operatorPublicKey()));
                TransactionResponse response = scheduleCreateTransaction.freezeWith(hederaClient.client()).execute(hederaClient.client());

                try {
                    TransactionReceipt receipt = response.getReceipt(hederaClient.client());
                    if (receipt.status == Status.SUCCESS) {
                        log.debug("created scheduled transaction - scheduleId {}", receipt.scheduleId.toString());
                    }
                    TransactionSchedulerResult transactionSchedulerResult = handleResponse(hederaClient, receipt);
                    hederaClient.client().close();
                    return transactionSchedulerResult;
                } catch (TimeoutException e) {
                    log.error("TimeoutException fetching receipt", e);
                    hederaClient.client().close();
                    throw e;
                } catch (ReceiptStatusException receiptStatusException) {
                    TransactionSchedulerResult transactionSchedulerResult = handleResponse(hederaClient, receiptStatusException.receipt);
                    hederaClient.client().close();
                    return transactionSchedulerResult;
                }
            } catch (PrecheckStatusException e) {
                hederaClient.client().close();
                return new TransactionSchedulerResult(/* success= */false, e.status);
            }
        }
    }

    /**
     * Adds a signature to an existing schedule id
     *
     * @param hederaClient the HederaClient to use
     * @param existingReceipt the receipt of the scheduleCreate transaction
     * @return TransactionSchedulerResult the result of the operation
     * @throws TimeoutException in the event of an error
     */
    private TransactionSchedulerResult scheduleSignTransaction(HederaClient hederaClient, TransactionReceipt existingReceipt) throws TimeoutException {
        // the same tx has already been submitted, submit just the signature
        // get the receipt for the transaction
        try {
            this.transactionId = TransactionId.generate(AccountId.fromString(Objects.requireNonNull(env.get("OPERATOR_ID"))));
            log.debug("Signing schedule id {}, transaction id {} with key for public key {}", existingReceipt.scheduleId, this.transactionId.toString(), shortKey(hederaClient.operatorPublicKey()));
            ScheduleSignTransaction scheduleSignTransaction = new ScheduleSignTransaction()
                    .setTransactionId(this.transactionId)
                    .setScheduleId(existingReceipt.scheduleId);

            TransactionResponse response = scheduleSignTransaction.freezeWith(hederaClient.client()).execute(hederaClient.client());

            try {
                TransactionReceipt receipt = response.getReceipt(hederaClient.client());
                return handleResponse(hederaClient, receipt, existingReceipt.scheduleId);
            } catch (ReceiptStatusException receiptStatusException) {
                return handleResponse(hederaClient, receiptStatusException.receipt, existingReceipt.scheduleId);
            }

        } catch (TimeoutException e) {
            log.error("Exception fetching receipt", e);
            throw e;
        } catch (PrecheckStatusException precheckStatusException) {
            log.debug(precheckStatusException.status);
            switch (precheckStatusException.status) {
            case SCHEDULE_ALREADY_EXECUTED:
                return new TransactionSchedulerResult(/* success= */true, Status.SUCCESS, existingReceipt.scheduleId);
            case DUPLICATE_TRANSACTION:
                return new TransactionSchedulerResult(/* success= */false, precheckStatusException.status);
            default:
                return new TransactionSchedulerResult(/* success= */false, precheckStatusException.status);
            }
        }
    }

    /**
     * Processes the scheduling or signing transaction result
     * If the receipt status is SUCCESS, ALREADY EXECUTED or NO NEW VALID SIGNATURES, return a positive response
     * If the receipt status is SCHEDULE ALREADY CREATED, send a transaction to add a new signature to the schedule
     * If the receipt status is DUPLICATE TRANSACTION, return a negative response
     * In all other cases, return a negative response
     *
     * @param hederaClient the hederaClient to use
     * @param receipt the receipt of the operation to process
     * @param scheduleId the schedule id of the operation to process
     * @return TransactionSchedulerResult containing the result of the processing
     * @throws TimeoutException in the event of an error
     */

    private TransactionSchedulerResult handleResponse(HederaClient hederaClient, TransactionReceipt receipt, ScheduleId scheduleId) throws TimeoutException {
        log.debug(receipt.status);
        switch (receipt.status) {
            case SUCCESS:
            case SCHEDULE_ALREADY_EXECUTED:
            case NO_NEW_VALID_SIGNATURES:
                return new TransactionSchedulerResult(/* success= */true, Status.SUCCESS, scheduleId);
            case IDENTICAL_SCHEDULE_ALREADY_CREATED:
                return scheduleSignTransaction(hederaClient, receipt);
            case DUPLICATE_TRANSACTION:
                return new TransactionSchedulerResult(/* success= */false, receipt.status);
            default:
                return new TransactionSchedulerResult(/* success= */false, receipt.status);
        }
    }

    /**
     * Handles a response given a receipt
     * @param hederaClient the HederaClient to use
     * @param receipt the receipt to process
     * @return TransactionSchedulerResult containing the result of the receipt processing
     * @throws TimeoutException in the event of an error
     */
    private TransactionSchedulerResult handleResponse(HederaClient hederaClient, TransactionReceipt receipt) throws TimeoutException {
        return handleResponse(hederaClient, receipt, receipt.scheduleId);
    }

    /**
     * Shortens a public key to the last 4 characters for logging purposes
     *
     * @param publicKey the public key to shorten
     * @return String containing the last 4 characters of the public key
     */
    private static String shortKey(PublicKey publicKey) {
        return publicKey.toString().substring(publicKey.toString().length() - 4);
    }
}
