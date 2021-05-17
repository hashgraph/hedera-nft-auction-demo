package com.hedera.demo.auction.node.app.auctionendtransfer;

import com.google.errorprone.annotations.Var;
import com.hedera.demo.auction.node.app.HederaClient;
import com.hedera.demo.auction.node.app.Utils;
import com.hedera.demo.auction.node.app.domain.Auction;
import com.hedera.demo.auction.node.app.repository.AuctionsRepository;
import com.hedera.demo.auction.node.app.scheduledoperations.TransactionScheduler;
import com.hedera.demo.auction.node.app.scheduledoperations.TransactionSchedulerResult;
import com.hedera.hashgraph.sdk.*;
import io.vertx.ext.web.client.WebClient;
import lombok.extern.log4j.Log4j2;
import org.jooq.tools.StringUtils;

import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.TimeoutException;

@Log4j2
public class AuctionEndTransfer implements Runnable {

    protected final WebClient webClient;
    protected final AuctionsRepository auctionsRepository;
    protected final int mirrorQueryFrequency;
    protected final String mirrorProvider;
    protected final String refundKey;
    protected final HederaClient hederaClient;
    protected boolean runThread = true;
    protected boolean testing = false;

    public AuctionEndTransfer(HederaClient hederaClient, WebClient webClient, AuctionsRepository auctionsRepository, String refundKey, int mirrorQueryFrequency) {
        this.webClient = webClient;
        this.auctionsRepository = auctionsRepository;
        this.mirrorQueryFrequency = mirrorQueryFrequency;
        this.refundKey = refundKey;
        this.hederaClient = hederaClient;
        this.mirrorProvider = hederaClient.mirrorProvider();
    }

    public void setTesting() {
        this.testing = true;
    }
    public void stop() {
        runThread = false;
    }

    /**
     * check association between winner and token for auctions that are currently "CLOSED"
     */
    @Override
    public void run() {
        @Var AuctionEndTransferInterface auctionEndTransferInterface;

        while (runThread) {
            try {
                List<Auction> auctionsList = auctionsRepository.getAuctionsList();
                for (Auction auction: auctionsList) {
                    switch (mirrorProvider) {
                        case "HEDERA":
                            auctionEndTransferInterface = new HederaAuctionEndTransfer(hederaClient, webClient, auctionsRepository, auction.getTokenid(), auction.getWinningaccount());
                            break;
                        default:
                            log.error("Support for non Hedera mirrors not implemented.");
                            return;
                    }
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
                        AbstractAuctionEndTransfer.TransferResult result = auctionEndTransferInterface.checkTransferInProgress(auction);
                        switch (result) {
                            case FAILED, NOT_FOUND:
                                // transfer the token
                                if (! StringUtils.isEmpty(this.refundKey)) {
                                    transferToken(auction);
                                }
                                break;
                            case SUCCESS:
                                // transfer already occurred and the checkTransferInProgress should have updated the auction
                                // status accordingly
                                break;
                        }
                    }
                }
            } catch (SQLException sqlException) {
                log.error("Failed to fetch auctions");
                log.error(sqlException);
            }
            Utils.sleep(this.mirrorQueryFrequency);
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
        @Var boolean delayTransfer = false;

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
                        Client client = hederaClient.auctionClient(auctionAccountId, PrivateKey.fromString(refundKey));
                        String memo = "Token transfer from auction";
                        //TODO: Use transferTimestamp (which also needs to be set when TRANSFERRING earlier)
                        String txId = auction.getAuctionaccountid().concat("@").concat(auction.getTransfertimestamp());
                        TransactionId transactionId = TransactionId.fromString(txId);
                        transactionId.setScheduled(true);
                        String shortTransactionId = transactionId.toString().replace("?scheduled", "");

                        TransferTransaction transferTransaction = new TransferTransaction();
                        transferTransaction.setTransactionMemo(memo);
                        transferTransaction.setTransactionId(transactionId);
                        transferTransaction.addTokenTransfer(tokenId, auctionAccountId, -1L);
                        transferTransaction.addTokenTransfer(tokenId, transferToAccountId, 1L);
                        if (!tokenOwnerAccount.equals(transferToAccountId)) {
                            // we have a winner, add hbar transfer to the original token owner to the transaction
                            transferTransaction.addHbarTransfer(auctionAccountId, Hbar.fromTinybars(-auction.getWinningbid()));
                            transferTransaction.addHbarTransfer(tokenOwnerAccount, Hbar.fromTinybars(auction.getWinningbid()));
                        }

                        try {
                            TransactionScheduler transactionScheduler = new TransactionScheduler(client, auctionAccountId, PrivateKey.fromString(refundKey), transactionId, transferTransaction);
                            TransactionSchedulerResult transactionSchedulerResult = transactionScheduler.issueScheduledTransaction();

                            if (transactionSchedulerResult.success) {
                                transferInProgress = true;
                                log.info("token transfer scheduled (id " + shortTransactionId + ")");
                            } else if (transactionSchedulerResult.status == Status.TRANSACTION_EXPIRED) {
                                delayTransfer = true;
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
            if (delayTransfer) {
                // the transfer timestamp is too far in the past for a deterministic transaction id, add 30s and let the process
                // try again later
                log.info("delaying token transfer (auction = " + auction.getAuctionaccountid() + ")");
                String transferTimestamp = Utils.addToTimestamp(auction.getTransfertimestamp(), 30);

                try {
                    auctionsRepository.setTransferTimestamp(auction.getId(), transferTimestamp);
                } catch (SQLException sqlException) {
                    log.error("unable to set auction next transfer timestamp (auction = " + auction.getAuctionaccountid() + ")");
                    log.error(sqlException);
                }
            }
        } else {
            log.error("Token owner for auction id " + auction.getId() + " is not set.");
        }
    }
}
