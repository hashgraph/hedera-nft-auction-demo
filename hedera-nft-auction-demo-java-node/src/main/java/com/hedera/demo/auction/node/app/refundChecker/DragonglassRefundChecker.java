package com.hedera.demo.auction.node.app.refundChecker;

import com.hedera.demo.auction.node.app.HederaClient;
import com.hedera.demo.auction.node.app.repository.BidsRepository;
import io.vertx.ext.web.client.WebClient;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class DragonglassRefundChecker extends AbstractRefundChecker implements RefundCheckerInterface {

    public DragonglassRefundChecker(HederaClient hederaClient, WebClient webClient, BidsRepository bidsRepository, int mirrorQueryFrequency) {
        super(hederaClient, webClient, bidsRepository, mirrorQueryFrequency);
    }

    @Override
    public void watch() {

        //TODO:
        log.info("Not implemented on Dragonglass");
        log.info("Checking for bid refunds");
    }
}
