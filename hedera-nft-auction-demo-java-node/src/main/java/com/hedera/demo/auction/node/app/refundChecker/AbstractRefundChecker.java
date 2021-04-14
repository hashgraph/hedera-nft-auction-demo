package com.hedera.demo.auction.node.app.refundChecker;

import com.hedera.demo.auction.node.app.HederaClient;
import com.hedera.demo.auction.node.app.mirrormapping.MirrorTransaction;
import com.hedera.demo.auction.node.app.mirrormapping.MirrorTransactions;
import com.hedera.demo.auction.node.app.repository.BidsRepository;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import lombok.extern.log4j.Log4j2;

import java.sql.SQLException;

@Log4j2
public class AbstractRefundChecker {

    protected final WebClient webClient;
    protected final BidsRepository bidsRepository;
    protected final int mirrorQueryFrequency;
    protected final String mirrorURL;
    protected final String mirrorProvider;
    protected final HederaClient hederaClient;

    public AbstractRefundChecker(HederaClient hederaClient, WebClient webClient, BidsRepository bidsRepository, int mirrorQueryFrequency) {
        this.webClient = webClient;
        this.bidsRepository = bidsRepository;
        this.mirrorQueryFrequency = mirrorQueryFrequency;
        this.mirrorURL = hederaClient.mirrorUrl();
        this.hederaClient = hederaClient;
        this.mirrorProvider = hederaClient.mirrorProvider();
    }

    public void handleResponse(JsonObject response, String timestamp, String transactionId) {
        try {
            MirrorTransactions mirrorTransactions = response.mapTo(MirrorTransactions.class);
            if (mirrorTransactions.transactions.size() > 0) {
                for (MirrorTransaction transaction : mirrorTransactions.transactions) {
                    if (transaction.isSuccessful()) {
                        // set refunded to true
                        log.debug("Found successful refund transaction on " + timestamp + " transaction id " + transactionId);

                        bidsRepository.setRefunded(timestamp, transaction.getTransactionHashString());
                    } else {
                        log.debug("Refund transaction on " + timestamp + " transaction id " + transactionId + " failed: " + transaction.result);
                    }

                }
            } else {
                log.debug("No " + transactionId + " transaction found");
            }
        } catch (RuntimeException | SQLException e) {
            log.error(e);
        }
    }

}
