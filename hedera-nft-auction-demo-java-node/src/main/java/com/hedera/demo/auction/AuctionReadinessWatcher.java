package com.hedera.demo.auction;

import com.google.errorprone.annotations.Var;
import com.hedera.demo.auction.app.HederaClient;
import com.hedera.demo.auction.app.Utils;
import com.hedera.demo.auction.app.domain.Auction;
import com.hedera.demo.auction.app.mirrormapping.MirrorTokenTransfer;
import com.hedera.demo.auction.app.mirrormapping.MirrorTransaction;
import com.hedera.demo.auction.app.mirrormapping.MirrorTransactions;
import com.hedera.demo.auction.app.repository.AuctionsRepository;
import com.hedera.demo.auction.app.repository.BidsRepository;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import lombok.extern.log4j.Log4j2;
import org.jooq.tools.StringUtils;

import javax.annotation.Nullable;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Log4j2
public class AuctionReadinessWatcher implements Runnable {

    protected final Auction auction;
    protected final WebClient webClient;
    protected final AuctionsRepository auctionsRepository;
    protected final BidsRepository bidsRepository;
    protected final int mirrorQueryFrequency;
    protected final String mirrorProvider;
    protected final HederaClient hederaClient;

    protected boolean testing = false;
    protected boolean runThread = true;
    @Nullable
    protected BidsWatcher bidsWatcher = null;
    protected String nextTimestamp = "0.0";

    public AuctionReadinessWatcher(HederaClient hederaClient, WebClient webClient, AuctionsRepository auctionsRepository, BidsRepository bidsRepository, Auction auction, int mirrorQueryFrequency) {
        this.webClient = webClient;
        this.auctionsRepository = auctionsRepository;
        this.bidsRepository = bidsRepository;
        this.auction = auction;
        this.mirrorQueryFrequency = mirrorQueryFrequency;
        this.hederaClient = hederaClient;
        this.mirrorProvider = hederaClient.mirrorProvider();
    }

    public void setTesting() {
        this.testing = true;
    }

    public void stop() {
        if (bidsWatcher != null) {
            bidsWatcher.stop();
        }
        runThread = false;
    }

    /**
     * check transaction history for token, if associated update auction status
     * start new bidding monitor thread
     * and close this thread
     *
     * Note: Considered simply checking balances for the account, but this doesn't give us
     * a common consensus timestamp to indicate the start of the auction
     */
    @Override
    public void run() {
        log.info("Watching auction account Id " + auction.getAuctionaccountid() + ", token Id " + auction.getTokenid());
        String uri = "/api/v1/transactions";

        ExecutorService executor = Executors.newFixedThreadPool(1);
        while (runThread) {
            @Var String queryFromTimeStamp = nextTimestamp;
            while (!StringUtils.isEmpty(queryFromTimeStamp)) {
                log.debug("Checking ownership of token " + auction.getTokenid() + " for account " + auction.getAuctionaccountid());
                Map<String, String> queryParameters = new HashMap<>();
                queryParameters.put("account.id", auction.getAuctionaccountid());
                queryParameters.put("transactiontype", "CRYPTOTRANSFER");
                queryParameters.put("order", "asc");
                queryParameters.put("timestamp", "gt:".concat(queryFromTimeStamp));

                Future<JsonObject> future = executor.submit(Utils.queryMirror(webClient, hederaClient, uri, queryParameters));
                try {
                    JsonObject response = future.get();
                    if (response != null) {
                        MirrorTransactions mirrorTransactions = response.mapTo(MirrorTransactions.class);
                        log.debug("Got " + mirrorTransactions.transactions.size() + " to verify");

                        if (handleResponse(mirrorTransactions)) {
                            // token is owned by the auction account, exit this thread
                            log.debug("Token owned by the account");
                            runThread = false;
                            break;
                        } else {
                            if (testing) {
                                runThread = false;
                            }
                        }
                        queryFromTimeStamp = Utils.getTimestampFromMirrorLink(mirrorTransactions.links.next);
                        if (StringUtils.isEmpty(queryFromTimeStamp)) {
                            int transactionCount = mirrorTransactions.transactions.size();
                            if (transactionCount > 0) {
                                queryFromTimeStamp = mirrorTransactions.transactions.get(transactionCount - 1).consensusTimestamp;
                            }
                        }
                        if (! StringUtils.isEmpty(queryFromTimeStamp)) {
                            nextTimestamp = queryFromTimeStamp;
                        }
                    }
                } catch (InterruptedException interruptedException) {
                    log.error(interruptedException);
                    Thread.currentThread().interrupt();
                } catch (ExecutionException executionException) {
                    log.error(executionException);
                }
            }

            if (testing) {
                runThread = false;
            } else {
                Utils.sleep(this.mirrorQueryFrequency);
            }
        }
        executor.shutdown();
    }

    public boolean handleResponse(MirrorTransactions mirrorTransactions) {
        try {
            if (mirrorTransactions.transactions != null) {
                for (MirrorTransaction transaction : mirrorTransactions.transactions) {
                    if (transaction.isSuccessful()) {
                        @Var String tokenOwnerAccount = "";
                        @Var boolean auctionAccountFound = false;
                        for (MirrorTokenTransfer tokenTransfer : transaction.tokenTransfers) {
                            if (tokenTransfer.tokenId.equals(this.auction.getTokenid())) {
                                if (tokenTransfer.amount == -1) {
                                    // token owner
                                    tokenOwnerAccount = tokenTransfer.account;
                                } else if (tokenTransfer.amount == 1 && tokenTransfer.account.equals(auction.getAuctionaccountid())) {
                                    // auction account
                                    auctionAccountFound = true;
                                }
                            }
                        }

                        if (auctionAccountFound && ! StringUtils.isEmpty(tokenOwnerAccount)) {
                            // we have a transfer from the token owner to the auction account
                            // token is associated
                            log.info("Account " + auction.getAuctionaccountid() + " owns token " + auction.getTokenid() + ", starting auction");
                            auctionsRepository.setActive(auction, tokenOwnerAccount, transaction.consensusTimestamp);
                            // start the thread to monitor bids
                            if (!this.testing) {
                                bidsWatcher = new BidsWatcher(hederaClient, webClient, auctionsRepository, auction.getId(), mirrorQueryFrequency);
                                Thread t = new Thread(bidsWatcher);
                                t.start();
                            }
                            return true;
                        }
                    }
                }
                return false;
            }
        } catch (RuntimeException | SQLException e) {
            log.error(e);
        }
        return false;
    }
}
