package com.hedera.demo.auction.node.app.refundChecker;

import com.hedera.demo.auction.node.app.HederaClient;
import com.hedera.demo.auction.node.app.repository.BidsRepository;
import io.github.cdimascio.dotenv.Dotenv;
import io.vertx.ext.web.client.WebClient;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class RefundChecker extends AbstractRefundChecker implements Runnable {

    public RefundChecker(HederaClient hederaClient, WebClient webClient, BidsRepository bidsRepository, Dotenv env) {
        super(hederaClient, webClient, bidsRepository, env);
    }

    @SneakyThrows
    @Override
    public void run() {

        log.info("Checking for bid refunds");

        RefundCheckerInterface refundChecker;

        switch (mirrorProvider) {
            case "HEDERA":
                refundChecker = new HederaRefundChecker(hederaClient, webClient, bidsRepository, env);
                break;
            case "DRAGONGLASS":
                refundChecker = new DragonglassRefundChecker(hederaClient, webClient, bidsRepository, env);
                break;
            default:
                refundChecker = new KabutoRefundChecker(hederaClient, webClient, bidsRepository, env);
                break;
        }
        refundChecker.watch();
    }
}
