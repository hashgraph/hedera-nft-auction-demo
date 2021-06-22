package com.hedera.demo.auction.app.scheduledoperations;

import com.hedera.demo.auction.app.Utils;
import com.hedera.demo.auction.app.domain.Auction;
import com.hedera.demo.auction.app.domain.ScheduledOperation;
import com.hedera.demo.auction.app.repository.AuctionsRepository;
import com.hedera.demo.auction.app.repository.ScheduledOperationsRepository;
import com.hedera.hashgraph.sdk.AccountId;
import com.hedera.hashgraph.sdk.Hbar;
import com.hedera.hashgraph.sdk.TokenAssociateTransaction;
import com.hedera.hashgraph.sdk.TokenId;
import com.hedera.hashgraph.sdk.TransactionId;
import lombok.extern.log4j.Log4j2;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Log4j2
public class ScheduleExecutor implements Runnable {
    private final AuctionsRepository auctionsRepository;
    private final ScheduledOperationsRepository scheduledOperationsRepository;
    private final int mirrorQueryFrequency;

    private boolean runThread = true;

    public ScheduleExecutor(AuctionsRepository auctionsRepository, ScheduledOperationsRepository scheduledOperationsRepository, int mirrorQueryFrequency) {
        this.auctionsRepository = auctionsRepository;
        this.scheduledOperationsRepository = scheduledOperationsRepository;
        this.mirrorQueryFrequency = mirrorQueryFrequency;
    }

    @Override
    public void run() {
        while (runThread) {
            try {
                List<ScheduledOperation> pendingOperations = scheduledOperationsRepository.getPendingOperationsList();
                if (pendingOperations != null) {
                    for (ScheduledOperation scheduledOperation : pendingOperations) {
                        try {
                            Auction auction = auctionsRepository.getAuction(scheduledOperation.getAuctionid());
                            AccountId auctionAccountId = AccountId.fromString(auction.getAuctionaccountid());

                            if (scheduledOperation.getTransactiontype().equals(ScheduledOperation.TYPE_TOKENASSOCIATE)) {
                                // create a token association transaction
                                TokenAssociateTransaction tokenAssociateTransaction = new TokenAssociateTransaction();
                                List<TokenId> tokenIds = new ArrayList<>();
                                tokenIds.add(TokenId.fromString(auction.getTokenid()));
                                tokenAssociateTransaction.setTokenIds(tokenIds);
                                tokenAssociateTransaction.setTransactionMemo(scheduledOperation.getMemo());
                                tokenAssociateTransaction.setAccountId(auctionAccountId);
                                tokenAssociateTransaction.setMaxTransactionFee(Hbar.from(100));

                                TransactionId transactionId = TransactionId.generate(auctionAccountId);

                                TransactionScheduler transactionScheduler = new TransactionScheduler(auctionAccountId, transactionId, tokenAssociateTransaction);
                                TransactionSchedulerResult transactionSchedulerResult = transactionScheduler.issueScheduledTransaction("");
                                if (transactionSchedulerResult.success) {
                                    scheduledOperation.setStatus(ScheduledOperation.EXECUTING);
                                    log.info("token associate transaction successfully scheduled (id {})", transactionId.toString());
                                    scheduledOperationsRepository.setStatus(scheduledOperation.getTransactiontimestamp(), ScheduledOperation.EXECUTING, "");
                                } else {
                                    log.error("error scheduling token associate transaction (timestamp {})", scheduledOperation.getTransactiontimestamp());
                                    scheduledOperationsRepository.setStatus(scheduledOperation.getTransactiontimestamp(), ScheduledOperation.PENDING, transactionSchedulerResult.status.toString());
                                    log.error(transactionSchedulerResult.status);
                                }
                            }
                        } catch (SQLException e) {
                            log.error("error fetching auction for pending operations");
                            log.error(e, e);
                        } catch (Exception e) {
                            log.error(e, e);
                        }
                    }
                }
            } catch (SQLException e) {
                log.error("error fetching list of pending operations");
                log.error(e, e);
            }

            Utils.sleep(this.mirrorQueryFrequency);
        }
    }

    public void stop() {
        runThread = false;
    }
}
