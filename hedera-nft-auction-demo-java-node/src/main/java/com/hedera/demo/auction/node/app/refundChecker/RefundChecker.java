package com.hedera.demo.auction.node.app.refundChecker;

import com.google.errorprone.annotations.Var;
import com.hedera.demo.auction.node.app.HederaClient;
import com.hedera.demo.auction.node.app.repository.AuctionsRepository;
import com.hedera.demo.auction.node.app.repository.BidsRepository;
import io.vertx.ext.web.client.WebClient;
import lombok.extern.log4j.Log4j2;

import javax.annotation.Nullable;

@Log4j2
public class RefundChecker extends AbstractRefundChecker implements Runnable {

    public RefundChecker(HederaClient hederaClient, WebClient webClient, AuctionsRepository auctionsRepository, BidsRepository bidsRepository, int mirrorQueryFrequency) {
        super(hederaClient, webClient, auctionsRepository, bidsRepository, mirrorQueryFrequency);
    }

    @Override
    public void run() {

        log.info("Checking for bid refunds");
        RefundCheckerInterface refundChecker = getRefundChecker();
        if (refundChecker != null) {
            refundChecker.watch();
        }
    }

    public void runOnce() {
        log.info("Checking for bid refunds");
        RefundCheckerInterface refundChecker = getRefundChecker();
        if (refundChecker != null) {
            refundChecker.watchOnce();
        }
    }

    @Nullable
    private RefundCheckerInterface getRefundChecker() {
        @Var RefundCheckerInterface refundChecker = null;

        switch (mirrorProvider) {
            case "HEDERA":
                refundChecker = new HederaRefundChecker(hederaClient, webClient, auctionsRepository, bidsRepository, mirrorQueryFrequency);
                break;
            default:
                log.error("Support for non Hedera mirrors not implemented.");
        }
        return refundChecker;
    }
}
