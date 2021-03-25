package com.hedera.demo.auction.node.app.refundChecker;

import com.hedera.demo.auction.node.app.HederaClient;
import com.hedera.demo.auction.node.app.repository.BidsRepository;
import io.github.cdimascio.dotenv.Dotenv;
import io.vertx.ext.web.client.WebClient;

import java.util.Optional;

public class AbstractRefundChecker {

    protected final WebClient webClient;
    protected final BidsRepository bidsRepository;
    protected final int mirrorQueryFrequency;
    protected final String mirrorURL;
    protected final String mirrorProvider = HederaClient.getMirrorProvider();
    protected final Dotenv env;

    public AbstractRefundChecker(WebClient webClient, BidsRepository bidsRepository, Dotenv env) throws Exception {
        this.webClient = webClient;
        this.bidsRepository = bidsRepository;
        this.mirrorQueryFrequency = Integer.parseInt(Optional.ofNullable(env.get("MIRROR_QUERY_FREQUENCY")).orElse("5000"));
        this.mirrorURL = HederaClient.getMirrorUrl();
        this.env = env;
    }
}
