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
import com.hedera.hashgraph.sdk.PrivateKey;
import com.hedera.hashgraph.sdk.TokenId;
import com.hedera.hashgraph.sdk.TransactionId;
import com.hedera.hashgraph.sdk.TransferTransaction;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
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
public class AuctionEndTransfer implements Runnable {

    private final WebClient webClient;
    private final AuctionsRepository auctionsRepository;
    private final HederaClient hederaClient;
    private boolean runThread = true;
    private boolean testing = false;
    private final String operatorKey;
    private final int mirrorQueryFrequency;
    private final AccountId operatorId;

    public enum TransferResult {
        SUCCESS,
        FAILED,
        NOT_FOUND
    }

    public AuctionEndTransfer(HederaClient hederaClient, WebClient webClient, AuctionsRepository auctionsRepository, String operatorKey, int mirrorQueryFrequency) {
        this.webClient = webClient;
        this.auctionsRepository = auctionsRepository;
        this.hederaClient = hederaClient;
        this.operatorKey = operatorKey;
        this.mirrorQueryFrequency = mirrorQueryFrequency;
        this.operatorId = hederaClient.operatorId();
    }

    public void setTesting() {
        this.testing = true;
    }
    public void stop() {
        runThread = false;
    }

    @Override
    public void run() {
        while (runThread) {
            try {
                List<Auction> auctionsList = auctionsRepository.getAuctionsList();
                for (Auction auction: auctionsList) {
                    if (auction.isClosed() && StringUtils.isEmpty(auction.getTransferstatus())) {
                        log.debug("auction closed " + auction.getAuctionaccountid());
                        // auction is closed, check association between token and winner

                        if (StringUtils.isEmpty(auction.getWinningaccount())) {
                            // we don't have a winning bid, we'll transfer to the original token owner
                            try {
                                log.debug("auction " + auction.getAuctionaccountid() + " no winning bid, setting to " + Auction.TRANSFER_STATUS_PENDING);

                                auctionsRepository.setTransferPending(auction.getTokenid());
                            } catch (SQLException sqlException) {
                                log.error("Failed to set auction to " + Auction.TRANSFER_STATUS_PENDING + " status");
                                log.error(sqlException);
                            }
                        } else {
                            log.debug("auction " + auction.getAuctionaccountid() + " has winning bid");
                            log.info("Checking association between token " + auction.getTokenid() + " and account " + auction.getWinningaccount());
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
                            case FAILED, NOT_FOUND:
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
                    Utils.sleep(this.mirrorQueryFrequency);
                }
            } catch (SQLException sqlException) {
                log.error("Failed to fetch auctions");
                log.error(sqlException);
            }
        }
    }

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
        } catch (TimeoutException timeoutException) {
            log.error("timeout exception querying for balance");
            log.error(timeoutException);
        } catch (PrecheckStatusException precheckStatusException) {
            log.error("precheckStatusException exception querying for balance");
            log.error(precheckStatusException);
        } catch (SQLException sqlException) {
            log.error("unable to set auction to transferPending");
            log.error(sqlException);
        }
    }

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

            if (! testing) {
                try {
                    Client client = hederaClient.auctionClient(auctionAccountId, PrivateKey.fromString(operatorKey));
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

                    try {
                        TransactionScheduler transactionScheduler = new TransactionScheduler(hederaClient, auctionAccountId, transactionId, transferTransaction);
                        TransactionSchedulerResult transactionSchedulerResult = transactionScheduler.issueScheduledTransaction();

                        if (transactionSchedulerResult.success) {
                            transferInProgress = true;
                            log.info("token transfer scheduled (id " + shortTransactionId + ")");
                        } else {
                            log.error("error transferring token to winner auction: " + auction.getAuctionaccountid());
                            log.error(transactionSchedulerResult.status);
                        }
                    } catch (TimeoutException e) {
                        log.error("error scheduling token transfer transaction");
                        log.error(e);
                    }
                    client.close();
                } catch (Exception e) {
                    log.error("unable to create client for auction account");
                    log.error(e);
                }
            }

            if (transferInProgress || testing) {
                log.info("setting auction to transfer in progress (auction = " + auction.getAuctionaccountid() + ")");
                try {
                    auctionsRepository.setTransferInProgress(auction.getTokenid());
                } catch (SQLException e) {
                    log.error("unable to set auction to transfer in progress (auction = " + auction.getAuctionaccountid() + ")");
                    log.error(e);
                }
            }
        } else {
            log.error("Token owner for auction id " + auction.getId() + " is not set.");
            try {
                auctionsRepository.setTransferTransactionByAuctionId(auction.getId(), "Token not transferred by owner", "Token not transferred by owner");
            } catch (SQLException sqlException) {
                log.error("unable to end auction with token not transferred by owner");
                log.error(sqlException);
            }
        }
    }

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
                        } catch (SQLException sqlException) {
                            log.error("unable to set transaction to transfer complete");
                            log.error(sqlException);
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

            log.debug("querying mirror for successful transaction for account " + queryParameters.get("account.id") + ", timestamp:gt:".concat(nextTimestamp));
            Future<JsonObject> future = executor.submit(Utils.queryMirror(webClient, hederaClient, uri, queryParameters));
            try {
                JsonObject response = future.get();
                if (response != null) {
                    MirrorTransactions mirrorTransactions = response.mapTo(MirrorTransactions.class);
                    result = transferOccurredAlready(mirrorTransactions, auction.getTokenid());
                    log.info(result);
                    nextTimestamp = Utils.getTimestampFromMirrorLink(mirrorTransactions.links.next);
                }
            } catch (InterruptedException interruptedException) {
                log.error(interruptedException);
                Thread.currentThread().interrupt();
            } catch (ExecutionException executionException) {
                log.error(executionException);
            }

        }
        executor.shutdown();
        return result;
    }
}
