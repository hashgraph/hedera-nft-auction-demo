package com.hedera.demo.auction.node.app.readinesswatcher;

import com.google.errorprone.annotations.Var;
import com.hedera.demo.auction.node.app.HederaClient;
import com.hedera.demo.auction.node.app.bidwatcher.BidsWatcher;
import com.hedera.demo.auction.node.app.domain.Auction;
import com.hedera.demo.auction.node.app.mirrormapping.MirrorTokenTransfer;
import com.hedera.demo.auction.node.app.mirrormapping.MirrorTransaction;
import com.hedera.demo.auction.node.app.mirrormapping.MirrorTransactions;
import com.hedera.demo.auction.node.app.repository.AuctionsRepository;
import com.hedera.demo.auction.node.app.repository.BidsRepository;
import io.vertx.ext.web.client.WebClient;
import lombok.extern.log4j.Log4j2;
import org.jooq.tools.StringUtils;

import java.sql.SQLException;

@Log4j2
public abstract class AbstractAuctionReadinessWatcher {

    protected final Auction auction;
    protected final WebClient webClient;
    protected final AuctionsRepository auctionsRepository;
    protected final BidsRepository bidsRepository;
    protected final int mirrorQueryFrequency;
    protected String mirrorURL;
    protected final String refundKey;
    protected final HederaClient hederaClient;
    protected boolean testing = false;
    protected int mirrorPort = 80;
    protected boolean runThread = true;
    protected BidsWatcher bidsWatcher = null;

    protected AbstractAuctionReadinessWatcher(HederaClient hederaClient, WebClient webClient, AuctionsRepository auctionsRepository, BidsRepository bidsRepository, Auction auction, String refundKey, int mirrorQueryFrequency) {
        this.webClient = webClient;
        this.auctionsRepository = auctionsRepository;
        this.bidsRepository = bidsRepository;
        this.auction = auction;
        this.mirrorQueryFrequency = mirrorQueryFrequency;
        this.refundKey = refundKey;
        this.hederaClient = hederaClient;
        this.mirrorURL = hederaClient.mirrorUrl();
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
                                bidsWatcher = new BidsWatcher(hederaClient, webClient, auctionsRepository, bidsRepository, auction.getId(), refundKey, mirrorQueryFrequency);
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
