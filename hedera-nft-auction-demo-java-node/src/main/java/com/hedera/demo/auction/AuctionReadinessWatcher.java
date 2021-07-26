package com.hedera.demo.auction;

import com.google.errorprone.annotations.Var;
import com.hedera.demo.auction.app.HederaClient;
import com.hedera.demo.auction.app.Utils;
import com.hedera.demo.auction.app.domain.Auction;
import com.hedera.demo.auction.app.mirrormapping.MirrorTokenTransfer;
import com.hedera.demo.auction.app.mirrormapping.MirrorTransaction;
import com.hedera.demo.auction.app.mirrormapping.MirrorTransactions;
import com.hedera.demo.auction.app.repository.AuctionsRepository;
import io.vertx.core.json.JsonObject;
import lombok.extern.log4j.Log4j2;
import org.jooq.tools.StringUtils;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Log4j2
/**
 * Checks if an auction is ready to accept bids.
 */
public class AuctionReadinessWatcher implements Runnable {

    protected final Auction auction;
    protected final AuctionsRepository auctionsRepository;
    protected final int mirrorQueryFrequency;
    protected final String mirrorProvider;
    protected final HederaClient hederaClient;

    protected boolean runThread = true;
    @Nullable
    protected BidsWatcher bidsWatcher = null;
    protected String nextTimestamp = "0.0";
    protected boolean runOnce;

    /**
     * Constructor
     *
     * @param hederaClient the HederaClient to use to connect to Hedera
     * @param auctionsRepository the auctionrepository for database interaction
     * @param auction the auction to watch
     * @param mirrorQueryFrequency the frequency at which the mirror should be polled
     * @param runOnce specifies whether this should run once or in a loop
     */
    public AuctionReadinessWatcher(HederaClient hederaClient, AuctionsRepository auctionsRepository, Auction auction, int mirrorQueryFrequency, boolean runOnce) {
        this.auctionsRepository = auctionsRepository;
        this.auction = auction;
        this.mirrorQueryFrequency = mirrorQueryFrequency;
        this.hederaClient = hederaClient;
        this.mirrorProvider = hederaClient.mirrorProvider();
        this.runOnce = runOnce;
    }

    /**
     * Stops the thread cleanly
     */
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
        log.info("Watching auction account Id {}, token Id {}", auction.getAuctionaccountid(), auction.getTokenid());
        String uri = "/api/v1/transactions";

        // check auction status
        try {
            Auction checkAuction = auctionsRepository.getAuction(auction.getId());
            if ( ! checkAuction.isPending()) {
                log.info("auction is not pending, exiting thread");
                return;
            }
        } catch (Exception e) {
            log.error(e, e);
        }


        ExecutorService executor = Executors.newFixedThreadPool(1);
        while (runThread) {
            @Var String queryFromTimeStamp = nextTimestamp;
            while (!StringUtils.isEmpty(queryFromTimeStamp)) {
                log.debug("Checking ownership of token {} for account {}", auction.getTokenid(), auction.getAuctionaccountid());
                Map<String, String> queryParameters = new HashMap<>();
                queryParameters.put("account.id", auction.getAuctionaccountid());
                queryParameters.put("transactiontype", "CRYPTOTRANSFER");
                queryParameters.put("order", "asc");
                queryParameters.put("timestamp", "gt:".concat(queryFromTimeStamp));

                Future<JsonObject> future = executor.submit(Utils.queryMirror(hederaClient, uri, queryParameters));
                try {
                    JsonObject response = future.get();
                    if (response != null) {
                        MirrorTransactions mirrorTransactions = response.mapTo(MirrorTransactions.class);

                        if (handleResponse(mirrorTransactions)) {
                            // token is owned by the auction account, exit this thread
                            log.debug("Token owned by the account");
                            runThread = false;
                            break;
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
                } catch (InterruptedException e) {
                    log.error(e, e);
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    log.error(e, e);
                }
            }

            if (this.runOnce) {
                this.runThread = false;
            } else {
                Utils.sleep(this.mirrorQueryFrequency);
            }
        }
        executor.shutdown();
    }

    /**
     * Given a list of transactions from the mirror node, looks for a token transfer from the token owner and to the
     * auction account if the transaction was successful
     *
     * In the event the token transfer succeeded, the status of the auction is changed to "ACTIVE" and the owner of the token
     * recorded against the auction in the database along with the timestamp of the transfer transaction.
     * Finally, a bidwatcher thread is started for this auction
     *
     * @param mirrorTransactions a list of mirror transactions
     * @return boolean true if the token has been transferred successfully
     */
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
                            log.info("Account {} owns token {}, starting auction",  auction.getAuctionaccountid(), auction.getTokenid());
                            auctionsRepository.setActive(auction, tokenOwnerAccount, transaction.consensusTimestamp);
                            // start the thread to monitor bids
                            bidsWatcher = new BidsWatcher(hederaClient, auctionsRepository, auction.getId(), mirrorQueryFrequency, runOnce);
                            if (this.runOnce) {
                                // do not run as a thread
                                bidsWatcher.run();
                            } else {
                                Thread t = new Thread(bidsWatcher);
                                t.start();
                            }
                            return true;
                        }
                    }
                }
                return false;
            }
        } catch (Exception e) {
            log.error(e, e);
        }
        return false;
    }
}
