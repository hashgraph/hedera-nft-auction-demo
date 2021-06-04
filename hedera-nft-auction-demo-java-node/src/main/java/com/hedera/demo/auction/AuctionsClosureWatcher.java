package com.hedera.demo.auction;

import com.google.errorprone.annotations.Var;
import com.hedera.demo.auction.app.HederaClient;
import com.hedera.demo.auction.app.Utils;
import com.hedera.demo.auction.app.domain.Auction;
import com.hedera.demo.auction.app.mirrormapping.MirrorTransactions;
import com.hedera.demo.auction.app.repository.AuctionsRepository;
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
import org.jooq.tools.StringUtils;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;

@Log4j2
public class AuctionsClosureWatcher implements Runnable {

    private final WebClient webClient;
    private final AuctionsRepository auctionsRepository;
    private final int mirrorQueryFrequency;
    private final boolean transferOnWin;
    private final String refundKey;
    private final HederaClient hederaClient;
    private final String masterKey;
    protected boolean runThread = true;

    public AuctionsClosureWatcher(HederaClient hederaClient, WebClient webClient, AuctionsRepository auctionsRepository, int mirrorQueryFrequency, boolean transferOnWin, String refundKey, String masterKey) {
        this.webClient = webClient;
        this.auctionsRepository = auctionsRepository;
        this.mirrorQueryFrequency = mirrorQueryFrequency;
        this.transferOnWin = transferOnWin;
        this.refundKey = refundKey;
        this.hederaClient = hederaClient;
        this.masterKey = masterKey;
    }

    @Override
    public void run() {

        String uri = "/api/v1/transactions";

        ExecutorService executor = Executors.newFixedThreadPool(1);
        while (runThread) {

            Map<String, String> queryParameters = new HashMap<>();
            queryParameters.put("limit", "1");
            Future<JsonObject> future = executor.submit(Utils.queryMirror(webClient, hederaClient, uri, queryParameters));
            try {
                JsonObject response = future.get();
                if (response != null) {
                    MirrorTransactions mirrorTransactions = response.mapTo(MirrorTransactions.class);
                    handleResponse(mirrorTransactions);
                }
            } catch (InterruptedException interruptedException) {
                log.error(interruptedException);
                Thread.currentThread().interrupt();
            } catch (ExecutionException executionException) {
                log.error(executionException);
            }
            Utils.sleep(this.mirrorQueryFrequency);
        }
        executor.shutdown();
    }

    public void stop() {
        runThread = false;
    }

    private void handleResponse(MirrorTransactions mirrorTransactions) {
        if (mirrorTransactions.transactions != null) {
            if (mirrorTransactions.transactions.size() > 0) {
                closeAuctionIfPastEnd(mirrorTransactions.transactions.get(0).consensusTimestamp);
            }
        }
    }

    private void closeAuctionIfPastEnd(String consensusTimestamp) {
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
                        if ( !StringUtils.isEmpty(this.refundKey) && !StringUtils.isEmpty(masterKey)) {
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

    private void setSignatureRequiredOnAuctionAccount(int auctionId) {
        @Var Auction auction = null;
        try {
            auction = auctionsRepository.getAuction(auctionId);
        } catch (Exception e) {
            log.error("error getting auction id " + auctionId + " from database");
            log.error(e);
        }

        if (auction != null) {
            try {
                Client auctionClient = hederaClient.auctionClient(auction, PrivateKey.fromString(masterKey));

                PrivateKey masterKeyPrivate = PrivateKey.fromString(this.masterKey);
                auctionClient.setOperator(AccountId.fromString(auction.getAuctionaccountid()), masterKeyPrivate);
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
