package com.hedera.demo.auction.node.app.refundChecker;

import com.hedera.demo.auction.node.app.HederaClient;
import com.hedera.demo.auction.node.app.repository.BidsRepository;
import io.github.cdimascio.dotenv.Dotenv;
import io.vertx.ext.web.client.WebClient;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class KabutoRefundChecker extends AbstractRefundChecker implements RefundCheckerInterface {

    public KabutoRefundChecker(HederaClient hederaClient, WebClient webClient, BidsRepository bidsRepository, Dotenv env) {
        super(hederaClient, webClient, bidsRepository, env);
    }

    @Override
    public void watch() {

        //TODO:
        log.info("Not implemented on Kabuto");
        log.info("Checking for bid refunds");
    }
}
