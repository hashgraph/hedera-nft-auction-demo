package com.hedera.demo.auction.node.app.closurewatcher;

import com.google.errorprone.annotations.Var;
import com.hedera.demo.auction.node.app.HederaClient;
import com.hedera.demo.auction.node.app.domain.Auction;
import com.hedera.demo.auction.node.app.mirrormapping.MirrorTransactions;
import com.hedera.demo.auction.node.app.repository.AuctionsRepository;
import com.hedera.hashgraph.sdk.AccountId;
import com.hedera.hashgraph.sdk.AccountUpdateTransaction;
import com.hedera.hashgraph.sdk.Client;
import com.hedera.hashgraph.sdk.PrivateKey;
import com.hedera.hashgraph.sdk.Status;
import com.hedera.hashgraph.sdk.TransactionReceipt;
import com.hedera.hashgraph.sdk.TransactionResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import lombok.extern.log4j.Log4j2;

import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.TimeoutException;

@Log4j2
public abstract class AbstractAuctionsClosureWatcher {

    protected final WebClient webClient;
    protected final AuctionsRepository auctionsRepository;
    protected final int mirrorQueryFrequency;
    protected String mirrorURL;
    protected boolean transferOnWin;
    private final String refundKey;
    private final HederaClient hederaClient;
    protected boolean runThread = true;
    private final boolean masterNode;

    protected AbstractAuctionsClosureWatcher(HederaClient hederaClient, WebClient webClient, AuctionsRepository auctionsRepository, int mirrorQueryFrequency, boolean transferOnWin, String refundKey, boolean masterNode) {
        this.webClient = webClient;
        this.auctionsRepository = auctionsRepository;
        this.mirrorQueryFrequency = mirrorQueryFrequency;
        this.hederaClient = hederaClient;
        this.mirrorURL = hederaClient.mirrorUrl();
        this.transferOnWin = transferOnWin;
        this.refundKey = refundKey;
        this.masterNode = masterNode;
    }

    void handleResponse(JsonObject response) {
        if (response != null) {
            MirrorTransactions mirrorTransactions = response.mapTo(MirrorTransactions.class);

            if (mirrorTransactions.transactions != null) {
                if (mirrorTransactions.transactions.size() > 0) {
                    closeAuctionIfPastEnd(mirrorTransactions.transactions.get(0).consensusTimestamp);
                }
            }
        }
    }

    public void stop() {
        runThread = false;
    }

    protected void closeAuctionIfPastEnd(String consensusTimestamp) {
        try {
            for (Map.Entry<String, Integer> auctions : auctionsRepository.openAndPendingAuctions().entrySet()) {
                String endTimestamp = auctions.getKey();
                int auctionId = auctions.getValue();

                if (consensusTimestamp.compareTo(endTimestamp) > 0) {
                    // latest transaction past auctions end, close it
                    log.info("Closing/ending auction id " + auctionId);
                    try {
                        if ( transferOnWin) {
                            // if the auction transfers the token on winning, set the auction to closed (no more bids)
                            log.debug("transferOnWin, setting auction to CLOSED");
                            auctionsRepository.setClosed(auctionId);
                        } else {
                            // if the auction doesn't transfer the token on winning, set the auction to ended (no more bids - all done)
                            log.debug("transferOnWin false, setting auction to ENDED");
                            auctionsRepository.setEnded(auctionId);
                        }
                        //TODO: Enable scheduled transaction here when the ACCOUNT_UPDATE transaction type is
                        // supported by scheduled transactions, in the mean time, only the master node is able to do this.
                        if ( ! this.refundKey.isBlank() && masterNode) {
                            setSignatureRequiredOnAuctionAccount(auctionId);
                        }

                    } catch (SQLException e) {
                        log.error("unable to set transfer transaction on auction");
                        log.error(e);
                    }
                }
            }
        } catch (SQLException sqlException) {
            log.error("unable to fetch pending and open auctions");
            log.error(sqlException);
        }
    }

    protected void setSignatureRequiredOnAuctionAccount(int auctionId) {
        @Var Auction auction = null;
        try {
            auction = auctionsRepository.getAuction(auctionId);
        } catch (Exception e) {
            log.error("error getting auction id " + auctionId + " from database");
            log.error(e);
        }

        if (auction != null) {
            try {
                Client auctionClient = hederaClient.auctionClient(auction, PrivateKey.fromString(refundKey));

                PrivateKey refundKeyPrivate = PrivateKey.fromString(this.refundKey);
                auctionClient.setOperator(AccountId.fromString(auction.getAuctionaccountid()), refundKeyPrivate);
                log.info("Setting signature required key on account " + auction.getAuctionaccountid());

                AccountUpdateTransaction accountUpdateTransaction = new AccountUpdateTransaction();
                accountUpdateTransaction.setAccountId(AccountId.fromString(auction.getAuctionaccountid()));
                accountUpdateTransaction.setReceiverSignatureRequired(true);

                try {
                    TransactionResponse response = accountUpdateTransaction.execute(auctionClient);
                    // check for receipt
                    TransactionReceipt receipt = response.getReceipt(auctionClient);
                    if (receipt.status != Status.SUCCESS) {
                        log.error("Setting receiver signature required on account " + auction.getAuctionaccountid() + " failed with " + receipt.status);
                    }
                } catch (Exception e) {
                    log.error(e);
                }
                auctionClient.close();
            } catch (TimeoutException timeoutException) {
                log.error(timeoutException);
                timeoutException.printStackTrace();
            } catch (Exception e) {
                log.error(e);
                e.printStackTrace();
            }
        }
    }
}
