package com.hedera.demo.auction.node.app.refundChecker;

import com.hedera.demo.auction.node.app.HederaClient;
import com.hedera.demo.auction.node.app.repository.BidsRepository;
import com.hedera.demo.auction.node.app.mirrormapping.MirrorTransaction;
import com.hedera.demo.auction.node.app.mirrormapping.MirrorTransactions;
import io.github.cdimascio.dotenv.Dotenv;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.codec.binary.Hex;

import java.sql.SQLException;
import java.util.Base64;
import java.util.Optional;

@Log4j2
public class AbstractRefundChecker {

    protected final WebClient webClient;
    protected final BidsRepository bidsRepository;
    protected final int mirrorQueryFrequency;
    protected final String mirrorURL;
    protected final String mirrorProvider;
    protected final Dotenv env;
    protected final HederaClient hederaClient;

    public AbstractRefundChecker(HederaClient hederaClient, WebClient webClient, BidsRepository bidsRepository, Dotenv env) {
        this.webClient = webClient;
        this.bidsRepository = bidsRepository;
        this.mirrorQueryFrequency = Integer.parseInt(Optional.ofNullable(env.get("MIRROR_QUERY_FREQUENCY")).orElse("5000"));
        this.mirrorURL = hederaClient.mirrorUrl();
        this.env = env;
        this.hederaClient = hederaClient;
        this.mirrorProvider = hederaClient.mirrorProvider();
    }

    protected void handleResponse(JsonObject response, String timestamp, String transactionId) {
        try {
            MirrorTransactions mirrorTransactions = response.mapTo(MirrorTransactions.class);
            if (mirrorTransactions.transactions.size() > 0) {
                for (MirrorTransaction transaction : mirrorTransactions.transactions) {
                    if (transaction.isSuccessful()) {
                        // set refunded to true
                        log.debug("Found successful refund transaction on " + timestamp + " transaction id " + transactionId);
                        byte[] txHashBytes = Base64.getDecoder().decode(transaction.getTransactionHash());
                        String transactionHash = Hex.encodeHexString(txHashBytes);

                        bidsRepository.setRefunded(timestamp, transactionHash);
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
