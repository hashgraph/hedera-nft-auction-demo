package com.hedera.demo.auction.test.integration;

import com.hedera.demo.auction.app.HederaClient;

public class AbstractMirrorIntegrationTest extends AbstractIntegrationTest {

    protected String mirrorUrl;
    protected HederaClient hederaClient;

    public AbstractMirrorIntegrationTest(String mirrorProvider) throws Exception {
        this.hederaClient = new HederaClient(env);
        this.hederaClient.setMirrorProvider(mirrorProvider);
        this.mirrorUrl = this.hederaClient.mirrorUrl();
    }
}
