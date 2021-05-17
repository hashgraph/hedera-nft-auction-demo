package com.hedera.demo.auction.node.app.auctionendtransfer;

import com.google.errorprone.annotations.Var;
import com.hedera.demo.auction.node.app.HederaClient;
import com.hedera.demo.auction.node.app.mirrormapping.MirrorTokenTransfer;
import com.hedera.demo.auction.node.app.mirrormapping.MirrorTransaction;
import com.hedera.demo.auction.node.app.mirrormapping.MirrorTransactions;
import com.hedera.demo.auction.node.app.repository.AuctionsRepository;
import io.vertx.ext.web.client.WebClient;
import lombok.extern.log4j.Log4j2;

import java.sql.SQLException;

@Log4j2
public abstract class AbstractAuctionEndTransfer {

    protected final WebClient webClient;
    protected final AuctionsRepository auctionsRepository;
    protected final String tokenId;
    protected final String winningAccountId;
    protected final String mirrorURL;
    protected int mirrorPort = 80;
    protected final HederaClient hederaClient;

    protected enum TransferResult {
        SUCCESS,
        FAILED,
        NOT_FOUND
    }

    protected AbstractAuctionEndTransfer(HederaClient hederaClient, WebClient webClient, AuctionsRepository auctionsRepository, String tokenId, String winningAccountId) {
        this.webClient = webClient;
        this.auctionsRepository = auctionsRepository;
        this.tokenId = tokenId;
        this.winningAccountId = winningAccountId;
        this.hederaClient = hederaClient;
        this.mirrorURL = hederaClient.mirrorUrl();
    }

    protected TransferResult transferOccurredAlready(MirrorTransactions mirrorTransactions, String tokenId) {
        @Var TransferResult result = TransferResult.NOT_FOUND;
        for (MirrorTransaction mirrorTransaction : mirrorTransactions.transactions) {
            for (MirrorTokenTransfer mirrorTokenTransfer : mirrorTransaction.tokenTransfers) {
                if (mirrorTokenTransfer.tokenId.equals(tokenId)) {
                    if (mirrorTransaction.isSuccessful()) {
                        // transaction complete
                        try {
                            auctionsRepository.setTransferTransactionByTokenId(tokenId, mirrorTransaction.transactionId, mirrorTransaction.getTransactionHashString());
                            return HederaAuctionEndTransfer.TransferResult.SUCCESS;
                        } catch (SQLException sqlException) {
                            log.error("unable to set transaction to transfer complete");
                            log.error(sqlException);
                        }
                    } else {
                        // note: we keep going through the transactions just in case one is successful later
                        result = HederaAuctionEndTransfer.TransferResult.FAILED;
                    }
                }
            }
        }
        return result;
    }
}
