package com.hedera.demo.auction.node.app.refundChecker;

import com.google.errorprone.annotations.Var;
import com.hedera.demo.auction.node.app.HederaClient;
import com.hedera.demo.auction.node.app.domain.Bid;
import com.hedera.demo.auction.node.app.mirrormapping.MirrorTransaction;
import com.hedera.demo.auction.node.app.mirrormapping.MirrorTransactions;
import com.hedera.demo.auction.node.app.repository.AuctionsRepository;
import com.hedera.demo.auction.node.app.repository.BidsRepository;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import lombok.extern.log4j.Log4j2;

import java.sql.SQLException;

@Log4j2
public class AbstractRefundChecker {

    protected final WebClient webClient;
    protected final AuctionsRepository auctionsRepository;
    protected final BidsRepository bidsRepository;
    protected final int mirrorQueryFrequency;
    protected final String mirrorURL;
    protected final String mirrorProvider;
    protected final HederaClient hederaClient;
    protected boolean runThread = true;

    public AbstractRefundChecker(HederaClient hederaClient, WebClient webClient, AuctionsRepository auctionsRepository, BidsRepository bidsRepository, int mirrorQueryFrequency) {
        this.webClient = webClient;
        this.auctionsRepository = auctionsRepository;
        this.bidsRepository = bidsRepository;
        this.mirrorQueryFrequency = mirrorQueryFrequency;
        this.mirrorURL = hederaClient.mirrorUrl();
        this.hederaClient = hederaClient;
        this.mirrorProvider = hederaClient.mirrorProvider();
    }

    public void stop() {
        runThread = false;
    }

    public boolean handleResponse(JsonObject response) {
        @Var boolean refundsProcessed = false;
        MirrorTransactions mirrorTransactions = response.mapTo(MirrorTransactions.class);
        for (MirrorTransaction transaction : mirrorTransactions.transactions) {
            if (transaction.getMemoString().contains(Bid.REFUND_MEMO_PREFIX)) {
                String bidTransactionId = transaction.getMemoString().replace(Bid.REFUND_MEMO_PREFIX,"");
                if (transaction.isSuccessful()) {
                    // set bid refund complete
                    log.debug("Found successful refund transaction on " + transaction.consensusTimestamp + " for bid transaction id " + bidTransactionId);
                    try {
                        bidsRepository.setRefunded(bidTransactionId, transaction.transactionId, transaction.getTransactionHashString());
                        refundsProcessed = true;
                    } catch (SQLException sqlException) {
                        log.error("Error setting bid to refunded (bid transaction id + " + bidTransactionId + ")");
                        log.error(sqlException);
                    }
                } else {
                    // set bid refund pending
                    log.debug("Found failed refund transaction on " + transaction.consensusTimestamp + " for bid transaction id " + bidTransactionId);
                    try {
                        bidsRepository.setRefundPending(bidTransactionId);
                        refundsProcessed = true;
                    } catch (SQLException sqlException) {
                        log.error("Error setting bid to refund pending (bid transaction id + " + bidTransactionId + ")");
                        log.error(sqlException);
                    }
                }
            }
        }
        return refundsProcessed;
    }
}
