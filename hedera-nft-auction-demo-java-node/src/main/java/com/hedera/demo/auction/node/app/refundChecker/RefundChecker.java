package com.hedera.demo.auction.node.app.refundChecker;

import com.hedera.demo.auction.node.app.HederaClient;
import com.hedera.demo.auction.node.app.repository.BidsRepository;
import io.vertx.ext.web.client.WebClient;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class RefundChecker extends AbstractRefundChecker implements Runnable {

    public RefundChecker(HederaClient hederaClient, WebClient webClient, BidsRepository bidsRepository, int mirrorQueryFrequency) {
        super(hederaClient, webClient, bidsRepository, mirrorQueryFrequency);
    }

    @SneakyThrows
    @Override
    public void run() {

        log.info("Checking for bid refunds");

        RefundCheckerInterface refundChecker;

        switch (mirrorProvider) {
            case "HEDERA":
                refundChecker = new HederaRefundChecker(hederaClient, webClient, bidsRepository, mirrorQueryFrequency);
                break;
            default:
                throw new Exception("Support for non Hedera mirrors not implemented.");
        }
        refundChecker.watch();
    }
}
