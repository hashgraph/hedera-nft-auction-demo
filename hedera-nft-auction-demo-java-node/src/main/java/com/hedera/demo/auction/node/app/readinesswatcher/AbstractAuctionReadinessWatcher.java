package com.hedera.demo.auction.node.app.readinesswatcher;

import com.hedera.demo.auction.node.app.HederaClient;
import com.hedera.demo.auction.node.app.bidwatcher.BidsWatcher;
import com.hedera.demo.auction.node.app.domain.Auction;
import com.hedera.demo.auction.node.app.repository.AuctionsRepository;
import com.hedera.demo.auction.node.app.repository.BidsRepository;
import com.hedera.demo.auction.node.mirrormapping.MirrorTokenTransfer;
import com.hedera.demo.auction.node.mirrormapping.MirrorTransaction;
import com.hedera.demo.auction.node.mirrormapping.MirrorTransactions;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import kotlin.Pair;
import lombok.extern.log4j.Log4j2;

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

    protected AbstractAuctionReadinessWatcher(WebClient webClient, AuctionsRepository auctionsRepository, BidsRepository bidsRepository, Auction auction, String refundKey, int mirrorQueryFrequency) throws Exception {
        this.webClient = webClient;
        this.auctionsRepository = auctionsRepository;
        this.bidsRepository = bidsRepository;
        this.auction = auction;
        this.mirrorQueryFrequency = mirrorQueryFrequency;
        this.refundKey = refundKey;
        this.mirrorURL = HederaClient.getMirrorUrl();
    }

    protected Pair<Boolean, String> handleResponse(JsonObject response) {
        try {
            if (response != null) {
                MirrorTransactions mirrorTransactions = response.mapTo(MirrorTransactions.class);

                if (mirrorTransactions.transactions != null) {
                    for (MirrorTransaction transaction : mirrorTransactions.transactions) {
                        for (MirrorTokenTransfer tokenTransfer : transaction.tokenTransfers) {

                            String account = tokenTransfer.account;
                            String tokenId = tokenTransfer.tokenId;
                            long amount = tokenTransfer.amount;
                            if (checkAssociation(account, tokenId, amount)) {
                                // token is associated
                                log.info("Account " + auction.getAuctionaccountid() + " owns token " + auction.getTokenid() + ", starting auction");
                                auctionsRepository.setActive(auction, transaction.getConsensusTimestamp());
                                // start the thread to monitor bids
                                Thread t = new Thread(new BidsWatcher(webClient, auctionsRepository, bidsRepository, auction.getId(), refundKey, mirrorQueryFrequency));
                                t.start();
                                return new Pair<Boolean, String>(true, "");
                            }
                        }
                    }
                } else {
                    return new Pair<Boolean, String>(false, mirrorTransactions.links.getNext());
                }
            }
            return new Pair<Boolean, String>(false, "");
        } catch (RuntimeException | SQLException e) {
            log.error(e);
            return new Pair<Boolean, String>(false, "");
        }
    }

    protected boolean checkAssociation(String account, String tokenId, long amount) {
        if (account.equals(this.auction.getAuctionaccountid())) {
            if (tokenId.equals(this.auction.getTokenid())) {
                return (amount != 0);
            }
        }
        return false;
    }
}
