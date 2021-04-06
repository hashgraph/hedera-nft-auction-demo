package com.hedera.demo.auction.node.app.refunder;

import com.google.errorprone.annotations.Var;
import com.hedera.demo.auction.node.app.HederaClient;
import com.hedera.demo.auction.node.app.repository.BidsRepository;
import com.hedera.hashgraph.sdk.AccountId;
import com.hedera.hashgraph.sdk.Client;
import com.hedera.hashgraph.sdk.Hbar;
import com.hedera.hashgraph.sdk.PrivateKey;
import com.hedera.hashgraph.sdk.Status;
import com.hedera.hashgraph.sdk.TransactionId;
import com.hedera.hashgraph.sdk.TransactionReceipt;
import com.hedera.hashgraph.sdk.TransactionResponse;
import com.hedera.hashgraph.sdk.TransferTransaction;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.codec.binary.Hex;

@Log4j2
public class Refunder implements Runnable {

    private final BidsRepository bidsRepository;
    private final long amount;
    private final String refundKey;
    private final String accountId;
    private final String consensusTimestamp;
    private final String auctionAccountId;
    private final String bidTransactionId;
    private final PrivateKey refundKeyPrivate;
    public Refunder(BidsRepository bidsRepository, String auctionAccountId, long amount, String accountId, String consensusTimestamp, String bidTransactionId, String refundKey) {
        this.bidsRepository = bidsRepository;
        this.amount = amount;
        this.refundKey = refundKey;
        this.accountId = accountId;
        this.consensusTimestamp = consensusTimestamp;
        this.auctionAccountId = auctionAccountId;
        this.bidTransactionId = bidTransactionId;
        if (this.refundKey.isBlank()) {
            // dummy key for the client
            this.refundKeyPrivate = PrivateKey.generate();
        } else {
            this.refundKeyPrivate = PrivateKey.fromString(refundKey);
        }
    }

    @SneakyThrows
    @Override
    public void run() {
        try {
            //TODO: Check a scheduled transaction has not already completed (success) for this bid
            // can only work with scheduled transactions

            // create a client for the auction's account
            Client client = HederaClient.getClient();

            client.setOperator(AccountId.fromString(this.auctionAccountId), refundKeyPrivate);
            log.info("Refunding " + this.amount + " from " + this.auctionAccountId + " to " + this.accountId);
            String memo = "Auction refund for tx " + bidTransactionId;
            // issue refund

//            //TODO: Scheduled transaction here
//            // create a deterministic transaction id from the consensus timestamp of the payment transaction
//            // note: this assumes the scheduled transaction occurs quickly after the payment
//            String deterministicTxId = this.auctionAccountId.concat("@").concat(this.consensusTimestamp);
//            TransactionId transactionId = TransactionId.fromString(deterministicTxId);
//            // Create a transfer transaction for the refund
//            TransferTransaction transferTransaction = new TransferTransaction();
//            //TODO: Fix list of node account ids
//            transferTransaction.setNodeAccountIds(List.of(AccountId.fromString("0.0.3")));
//            transferTransaction.setTransactionMemo(memo);
//            transferTransaction.setTransactionId(transactionId);
//            transferTransaction.addHbarTransfer(AccountId.fromString(this.auctionAccountId), Hbar.fromTinybars(-this.amount));
//            transferTransaction.addHbarTransfer(AccountId.fromString(this.accountId), Hbar.fromTinybars(this.amount));
//            transferTransaction.freezeWith(client);
//            transferTransaction.sign(this.refundKey);
//
//            // Schedule the transaction
//            ScheduleCreateTransaction scheduleCreateTransaction = transferTransaction.schedule();
//
//            TransactionResponse response = scheduleCreateTransaction.execute(client);
//            TransactionReceipt receipt = response.getReceipt(client);
//
//            byte[] transactionHash = response.transactionHash;
//            if (receipt.status == Status.SUCCESS) {
//                // update database
//                log.debug("Scheduling refund of " + this.amount + " to " + this.accountId);
//                bidsRepository.setRefundInProgress(this.consensusTimestamp, transactionId.toString(), Hex.encodeHexString(transactionHash));
//                log.debug("Successfully scheduled refund of " + this.amount + " to " + this.accountId);
//            } else {
//                log.error("Scheduling refund of " + this.amount + " to " + this.accountId + " failed with " + receipt.status);
//            }

            TransactionId transactionId = TransactionId.generate(AccountId.fromString(this.auctionAccountId));

            TransferTransaction transferTransaction = new TransferTransaction();
            transferTransaction.setTransactionMemo(memo);
            transferTransaction.setTransactionId(transactionId);
            transferTransaction.addHbarTransfer(AccountId.fromString(this.auctionAccountId), Hbar.fromTinybars(-this.amount));
            transferTransaction.addHbarTransfer(AccountId.fromString(this.accountId), Hbar.fromTinybars(this.amount));
            transferTransaction.freezeWith(client);

            transferTransaction.sign(refundKeyPrivate);

            @Var String transactionHash = "";
            if ( ! this.refundKey.isBlank()) {
                // only execute if we have a refund key
                TransactionResponse response = transferTransaction.execute(client);
                transactionHash = Hex.encodeHexString(response.transactionHash);
                // check for receipt
                TransactionReceipt receipt = response.getReceipt(client);
                if (receipt.status != Status.SUCCESS) {
                    log.error("Refunding " + this.amount + " to " + this.accountId + " failed with " + receipt.status);
                    return;
                }
            }
            // update database
            bidsRepository.setRefundInProgress(this.consensusTimestamp, transactionId.toString(), transactionHash);
        } catch (InterruptedException e) {
            log.error(e);
        }
    }
}
