package com.hedera.demo.auction;

import com.google.errorprone.annotations.Var;
import com.hedera.demo.auction.app.HederaClient;
import com.hedera.demo.auction.app.Utils;
import com.hedera.demo.auction.app.domain.Auction;
import com.hedera.demo.auction.app.mirrormapping.MirrorTokenTransfer;
import com.hedera.demo.auction.app.mirrormapping.MirrorTransaction;
import com.hedera.demo.auction.app.mirrormapping.MirrorTransactions;
import com.hedera.demo.auction.app.repository.AuctionsRepository;
import com.hedera.demo.auction.app.scheduledoperations.TransactionScheduler;
import com.hedera.demo.auction.app.scheduledoperations.TransactionSchedulerResult;
import com.hedera.hashgraph.sdk.AccountBalance;
import com.hedera.hashgraph.sdk.AccountBalanceQuery;
import com.hedera.hashgraph.sdk.AccountId;
import com.hedera.hashgraph.sdk.Client;
import com.hedera.hashgraph.sdk.Hbar;
import com.hedera.hashgraph.sdk.PrecheckStatusException;
import com.hedera.hashgraph.sdk.TokenId;
import com.hedera.hashgraph.sdk.TransactionId;
import com.hedera.hashgraph.sdk.TransferTransaction;
import io.vertx.core.json.JsonObject;
import lombok.extern.log4j.Log4j2;
import org.jooq.tools.StringUtils;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;

@Log4j2
/**
 * This class deals with the transferring of the auctioned token to the auction winner
 * or the original owner of the token in the event the auction's reserve was not met or
 * no winning bids were received.
 */
public class AuctionEndTransfer implements Runnable {

    private final AuctionsRepository auctionsRepository;
    private final HederaClient hederaClient;
    private boolean runThread = true;
    private final String operatorKey;
    private final int mirrorQueryFrequency;
    private final AccountId operatorId;

    public enum TransferResult {
        SUCCESS,
        FAILED,
        NOT_FOUND
    }

    public AuctionEndTransfer(HederaClient hederaClient, AuctionsRepository auctionsRepository, String operatorKey, int mirrorQueryFrequency) {
        this.auctionsRepository = auctionsRepository;
        this.hederaClient = hederaClient;
        this.operatorKey = operatorKey;
        this.mirrorQueryFrequency = mirrorQueryFrequency;
        this.operatorId = hederaClient.operatorId();
    }

    /**
     * Stops the thread cleanly
     */
    public void stop() {
        runThread = false;
    }

    /**
     * For each of the auctions:
     *
     * If the auction is closed and its transfer status is empty, look for the winning account,
     * if the winning account is empty, set the auction status to TRANSFER_PENDING,
     * if the winning account is not empty, check the token is associated to the winning account if so, set the auction's
     * status to TRANSFER_PENDING
     *
     * If the auction's status is TRANSFER_PENDING or TRANSFER_IN_PROGRESS, check if the transfer was successful.
     * if not successful or not found, attempt to transfer
     */
    @Override
    public void run() {
        while (runThread) {
            try {
                List<Auction> auctionsList = auctionsRepository.getAuctionsList();
                for (Auction auction: auctionsList) {
                    if (auction.isClosed() && StringUtils.isEmpty(auction.getTransferstatus()) && auction.getProcessrefunds()) {
                        log.debug("auction closed {}", auction.getAuctionaccountid());
                        // auction is closed, check association between token and winner

                        if (StringUtils.isEmpty(auction.getWinningaccount())) {
                            // we don't have a winning bid, we'll transfer to the original token owner
                            try {
                                log.debug("auction {} no winning bid, setting to {}", auction.getAuctionaccountid(), Auction.TRANSFER_STATUS_PENDING);

                                auctionsRepository.setTransferPending(auction.getTokenid());
                            } catch (Exception e) {
                                log.error("Failed to set auction to {} status", Auction.TRANSFER_STATUS_PENDING, e);
                            }
                        } else {
                            log.debug("auction {} has winning bid", auction.getAuctionaccountid());
                            log.info("Checking association between token {} and account {}", auction.getTokenid(), auction.getWinningaccount());
                            setTransferringIfAssociated(auction);
                        }
                    }
                    if (auction.isTransferPending() || auction.isTransferInProgress()) {
                        // has a scheduled TX completed already, if so, just update the DB with it
                        log.debug("calling auctionEndTransferInterface.checkTransferInProgress");
                        TransferResult result = checkTransferInProgress(auction);
                        // transfer the token
                        // transfer already occurred and the checkTransferInProgress should have updated the auction
                        // status accordingly
                        switch (result) {
                            case FAILED:
                            case NOT_FOUND:
                                log.debug("result FAILED, NOT_FOUND");
                                if (!StringUtils.isEmpty(this.operatorKey)) {
                                    log.debug("Transferring token");
                                    transferToken(auction);
                                    log.debug("Token transfer started");
                                }
                                break;
                            case SUCCESS:
                                log.debug("result SUCCESS");
                        }
                    }
                }
                Utils.sleep(this.mirrorQueryFrequency);
            } catch (SQLException e) {
                log.error("Failed to fetch auctions", e);
            }
        }
    }

    /**
     * Checks if an auction's token is associated with the winning account, if the association exists, set the
     * auction's transfer status to "PENDING"
     * @param auction the auction for which to check the association
     */
    protected void setTransferringIfAssociated(Auction auction) {

        // Query the account balance
        // if token is in the list of balance.tokens, it's associated
        Client client = hederaClient.client();
        try {
            AccountBalance accountBalance = new AccountBalanceQuery()
                    .setAccountId(AccountId.fromString(auction.getWinningaccount()))
                    .execute(client);

            if (accountBalance.token.containsKey(TokenId.fromString(auction.getTokenid()))) {
                auctionsRepository.setTransferPending(auction.getTokenid());
            }
        } catch (TimeoutException e) {
            log.error("timeout exception querying for balance", e);
        } catch (PrecheckStatusException e) {
            log.error("precheckStatusException exception querying for balance", e);
        } catch (Exception e) {
            log.error("unable to set auction to transferPending", e);
        }
    }

    /**
     * Schedules a transaction to transfer the token to the winning account (or the original owner if the auction
     * failed to meet reserve or didn't receive qualifying bids
     *
     * @param auction the auction to transfer the token from
     */
    public void transferToken(Auction auction) {
        @Var boolean transferInProgress = false;

        TokenId tokenId = TokenId.fromString(auction.getTokenid());
        if (!StringUtils.isEmpty(auction.getTokenowneraccount())) {
            AccountId auctionAccountId = AccountId.fromString(auction.getAuctionaccountid());
            AccountId tokenOwnerAccount = AccountId.fromString(auction.getTokenowneraccount());
            @Var AccountId transferToAccountId = AccountId.fromString(auction.getTokenowneraccount());
            if (!StringUtils.isEmpty(auction.getWinningaccount())) {
                // we have a winning bid, transfer to the winning account
                transferToAccountId = AccountId.fromString(auction.getWinningaccount());
            }

            try {
                String memo = "Token transfer from auction";

                TransactionId transactionId = TransactionId.generate(operatorId);
                transactionId.setScheduled(true);
                String shortTransactionId = transactionId.toString().replace("?scheduled", "");

                TransferTransaction transferTransaction = new TransferTransaction();
                transferTransaction.setTransactionMemo(memo);
                transferTransaction.setTransactionId(transactionId);
                transferTransaction.addTokenTransfer(tokenId, auctionAccountId, -1L);
                transferTransaction.addTokenTransfer(tokenId, transferToAccountId, 1L);
                transferTransaction.addHbarTransfer(auctionAccountId, Hbar.fromTinybars(-auction.getWinningbid()));
                transferTransaction.addHbarTransfer(tokenOwnerAccount, Hbar.fromTinybars(auction.getWinningbid()));

                TransactionScheduler transactionScheduler = new TransactionScheduler(auctionAccountId, transactionId, transferTransaction);
                TransactionSchedulerResult transactionSchedulerResult = transactionScheduler.issueScheduledTransaction("Scheduled Auction End Transfer");

                if (transactionSchedulerResult.success) {
                    transferInProgress = true;
                    log.info("token transfer scheduled (id {})", shortTransactionId);
                } else {
                    log.error("error transferring token to winner auction: {} status {}", auction.getAuctionaccountid(), transactionSchedulerResult.status);
                }

            } catch (Exception e) {
                log.error("error scheduling transaction for auction {}, token {}, transfer {} to {}", auctionAccountId.toString(), tokenId.toString(), auction.getWinningbid(), transferToAccountId.toString(), e);
            }

            if (transferInProgress) {
                log.info("setting auction to transfer in progress (auction = {})",auction.getAuctionaccountid());
                try {
                    auctionsRepository.setTransferInProgress(auction.getTokenid());
                } catch (Exception e) {
                    log.error("unable to set auction to transfer in progress (auction = {}", auction.getAuctionaccountid(), e);
                }
            }
        } else {
            log.error("Token owner for auction id {} is not set.", auction.getAuctionaccountid());
            try {
                auctionsRepository.setTransferTransactionByAuctionId(auction.getId(), "Token not transferred by owner", "Token not transferred by owner");
            } catch (Exception e) {
                log.error(e.getMessage());
            }
        }
    }

    /**
     * Checks a mirror node for a token transfer, when a successful transfer is found, updates the auction
     * with the transfer transaction id and transaction hash
     * @param mirrorTransactions a list of transactions from the mirror node
     * @param tokenId the token id to look for a transaction for
     * @return a TransferResult indicating success, failure or not found.
     */
    private  TransferResult transferOccurredAlready(MirrorTransactions mirrorTransactions, String tokenId) {
        @Var TransferResult result = TransferResult.NOT_FOUND;
        for (MirrorTransaction mirrorTransaction : mirrorTransactions.transactions) {
            for (MirrorTokenTransfer mirrorTokenTransfer : mirrorTransaction.tokenTransfers) {
                if (mirrorTokenTransfer.tokenId.equals(tokenId)) {
                    if (mirrorTransaction.isSuccessful()) {
                        // transaction complete
                        try {
                            auctionsRepository.setTransferTransactionByTokenId(tokenId, mirrorTransaction.transactionId, mirrorTransaction.getTransactionHashString());
                            return AuctionEndTransfer.TransferResult.SUCCESS;
                        } catch (Exception e) {
                            log.error("unable to set transaction to transfer complete", e);
                        }
                    } else {
                        // note: we keep going through the transactions just in case one is successful later
                        result = AuctionEndTransfer.TransferResult.FAILED;
                    }
                }
            }
        }
        return result;
    }

    /**
     * Checks mirror node for token transfer transactions, for each response from mirror node
     * check if any of the transactions contained within the response are successful or failed transactions
     * If the response contains data, keep querying mirror node for later transactions.
     * @param auction the auction to check transfers for
     * @return a TransferResult
     */
    private TransferResult checkTransferInProgress(Auction auction) {
        String uri = "/api/v1/transactions";

        log.debug("checkTransferInProgress");

        @Var TransferResult result = TransferResult.NOT_FOUND;
        @Var String nextTimestamp = auction.getEndtimestamp();
        ExecutorService executor = Executors.newFixedThreadPool(1);
        while (!StringUtils.isEmpty(nextTimestamp)) {
            Map<String, String> queryParameters = new HashMap<>();
            if (StringUtils.isEmpty(auction.getWinningaccount())) {
                queryParameters.put("account.id", auction.getTokenowneraccount());
            } else {
                queryParameters.put("account.id", auction.getWinningaccount());
            }
            queryParameters.put("transactiontype", "CRYPTOTRANSFER");
            queryParameters.put("order", "asc");
            queryParameters.put("timestamp", "gt:".concat(nextTimestamp));

            log.debug("querying mirror for successful transaction for account {} , timestamp:gt:{}", queryParameters.get("account.id"), nextTimestamp);
            Future<JsonObject> future = executor.submit(Utils.queryMirror(hederaClient, uri, queryParameters));
            try {
                JsonObject response = future.get();
                if (response != null) {
                    MirrorTransactions mirrorTransactions = response.mapTo(MirrorTransactions.class);
                    result = transferOccurredAlready(mirrorTransactions, auction.getTokenid());
                    log.info(result);
                    nextTimestamp = Utils.getTimestampFromMirrorLink(mirrorTransactions.links.next);
                }
            } catch (InterruptedException e) {
                log.error(e, e);
                Thread.currentThread().interrupt();
            } catch (ExecutionException e) {
                log.error(e, e);
            }

        }
        executor.shutdown();
        return result;
    }
}
