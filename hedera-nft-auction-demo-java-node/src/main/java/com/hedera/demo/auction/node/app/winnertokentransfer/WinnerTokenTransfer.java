package com.hedera.demo.auction.node.app.winnertokentransfer;

import com.google.errorprone.annotations.Var;
import com.hedera.demo.auction.node.app.HederaClient;
import com.hedera.demo.auction.node.app.domain.Auction;
import com.hedera.demo.auction.node.app.repository.AuctionsRepository;
import com.hedera.hashgraph.sdk.AccountId;
import com.hedera.hashgraph.sdk.PrecheckStatusException;
import com.hedera.hashgraph.sdk.PrivateKey;
import com.hedera.hashgraph.sdk.ReceiptStatusException;
import com.hedera.hashgraph.sdk.Status;
import com.hedera.hashgraph.sdk.TokenId;
import com.hedera.hashgraph.sdk.TransactionId;
import com.hedera.hashgraph.sdk.TransactionReceipt;
import com.hedera.hashgraph.sdk.TransactionResponse;
import com.hedera.hashgraph.sdk.TransferTransaction;
import io.vertx.ext.web.client.WebClient;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.codec.binary.Hex;

import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.TimeoutException;

@Log4j2
public class WinnerTokenTransfer implements Runnable {

    protected final WebClient webClient;
    protected final AuctionsRepository auctionsRepository;
    protected final int mirrorQueryFrequency;
    protected final String mirrorProvider;
    protected final String refundKey;
    protected final HederaClient hederaClient;
    protected boolean runThread = true;

    public WinnerTokenTransfer(HederaClient hederaClient, WebClient webClient, AuctionsRepository auctionsRepository, String refundKey, int mirrorQueryFrequency) {
        this.webClient = webClient;
        this.auctionsRepository = auctionsRepository;
        this.mirrorQueryFrequency = mirrorQueryFrequency;
        this.refundKey = refundKey;
        this.hederaClient = hederaClient;
        this.mirrorProvider = hederaClient.mirrorProvider();
    }

    public void stop() {
        runThread = false;
    }

    /**
     * check association between winner and token for auctions that are currently "CLOSED"
     */
    @SneakyThrows
    @Override
    public void run() {
        @Var WinnerTokenTransferInterface winnerTokenTransfer;

        while (runThread) {
            List<Auction> auctionsList = auctionsRepository.getAuctionsList();
            for (Auction auction: auctionsList) {
                if (auction.isClosed()) {
                    // auction is closed, check association between token and winner
                    switch (mirrorProvider) {
                        case "HEDERA":
                            winnerTokenTransfer = new HederaWinnerTokenTransfer(hederaClient, webClient, auctionsRepository, auction.getTokenid(), auction.getWinningaccount());
                            break;
                        case "DRAGONGLASS":
                            winnerTokenTransfer = new DragonglassWinnerTokenTransfer(hederaClient, webClient, auctionsRepository, auction.getTokenid(), auction.getWinningaccount());
                            break;
                        default:
                            winnerTokenTransfer = new KabutoWinnerTokenTransfer(hederaClient, webClient, auctionsRepository, auction.getTokenid(), auction.getWinningaccount());
                            break;
                    }

                    log.info("Checking association between token " + auction.getTokenid() + " and account " + auction.getWinningaccount());

                    winnerTokenTransfer.checkAssociation();

                }
                if (auction.isTransferring()) {
                    if (auction.getTransfertxid().isEmpty()) {
                        // ok to transfer token to winner
                        transferToWinner(auction);
                    }
                }
            }
            try {
                Thread.sleep(this.mirrorQueryFrequency);
            } catch (InterruptedException e) {
                e.printStackTrace();
                log.error(e);
            }
        }
    }

    public void transferToWinner(Auction auction) {
        TokenId tokenId = TokenId.fromString(auction.getTokenid());
        AccountId auctionAccountId = AccountId.fromString(auction.getAuctionaccountid());
        AccountId winningAccountId = AccountId.fromString(auction.getWinningaccount());
        PrivateKey clientKey;

        if (this.refundKey.isBlank()) {
            // generate dummy key
            clientKey = PrivateKey.generate();
        } else {
            clientKey = PrivateKey.fromString(refundKey);
        }
        hederaClient.client().setOperator(auctionAccountId, clientKey);
        //TODO: Check a scheduled transaction has not already completed (success) for this

        // create a client for the auction's account
        String memo = "Token transfer from auction";
        // transfer the token

//            //TODO: Scheduled transaction here
//            // create a deterministic transaction id from the consensus timestamp of the payment transaction
//            // note: this assumes the scheduled transaction occurs quickly after the payment
//            String deterministicTxId = this.auctionAccountId.concat("@").concat(this.consensusTimestamp);
//            TransactionId transactionId = TransactionId.fromString(deterministicTxId);

        TransactionId transactionId = TransactionId.generate(auctionAccountId);
        @Var String transactionHash = "";

        TransferTransaction transferTransaction = new TransferTransaction();
        transferTransaction.setTransactionMemo(memo);
        transferTransaction.setTransactionId(transactionId);
        transferTransaction.addTokenTransfer(tokenId, auctionAccountId, -1L);
        transferTransaction.addTokenTransfer(tokenId, winningAccountId, 1L);
        transferTransaction.freezeWith(hederaClient.client());

        try {
            if ( ! this.refundKey.isBlank()) {
                transferTransaction.sign(clientKey);
                TransactionResponse response = transferTransaction.execute(hederaClient.client());
                transactionHash = Hex.encodeHexString(response.transactionHash);
                // check for receipt
                TransactionReceipt receipt = response.getReceipt(hederaClient.client());
                if (receipt.status != Status.SUCCESS) {
                    log.error("Transferring token " + tokenId + " to " + winningAccountId + " failed with " + receipt.status);
                    return;
                }
            }
        } catch (TimeoutException | PrecheckStatusException | ReceiptStatusException e) {
            log.error(e);
            return;
        }
        // update database
        try {
            auctionsRepository.setTransferTransaction(auction.getId(), transactionId.toString(), transactionHash);
        } catch (SQLException e) {
            log.error(e);
        }
    }
}
