package com.hedera.demo.auction.node.test.integration.mirror;

import com.hedera.demo.auction.node.app.HederaClient;
import com.hedera.demo.auction.node.test.integration.AbstractIntegrationTest;

public class AbstractMirrorTest extends AbstractIntegrationTest {

//    private final static boolean restAPI = Optional.ofNullable(env.get("REST_API")).map(Boolean::parseBoolean).orElse(false);
//    private final static int restApiVerticleCount = Optional.ofNullable(env.get("API_VERTICLE_COUNT")).map(Integer::parseInt).orElse(2);
//    private final static boolean adminAPI = Optional.ofNullable(env.get("ADMIN_API")).map(Boolean::parseBoolean).orElse(false);
//    private final static int adminApiVerticleCount = Optional.ofNullable(env.get("ADMIN_API_VERTICLE_COUNT")).map(Integer::parseInt).orElse(2);
//    private final static boolean auctionNode = Optional.ofNullable(env.get("AUCTION_NODE")).map(Boolean::parseBoolean).orElse(false);
//
//    private final static String topicId = Optional.ofNullable(env.get("VUE_APP_TOPIC_ID")).orElse("");
//    private final static int mirrorQueryFrequency = Integer.parseInt(Optional.ofNullable(env.get("MIRROR_QUERY_FREQUENCY")).orElse("5000"));
//    private final static String refundKey = Optional.ofNullable(env.get("REFUND_KEY")).orElse("");
//    private final static String postgresUrl = Optional.ofNullable(env.get("DATABASE_URL")).orElse("postgresql://localhost:5432/postgres");
//    private final static String postgresUser = Optional.ofNullable(env.get("DATABASE_USERNAME")).orElse("postgres");
//    private final static String postgresPassword = Optional.ofNullable(env.get("DATABASE_PASSWORD")).orElse("password");
//    private final static boolean transferOnWin = Optional.ofNullable(env.get("TRANSFER_ON_WIN")).map(Boolean::parseBoolean).orElse(true);
    protected String mirrorURL;
    protected HederaClient hederaClient;

    public AbstractMirrorTest(String mirrorProvider) throws Exception {
        this.hederaClient = new HederaClient(env);
        this.hederaClient.setMirrorProvider(mirrorProvider);
        this.mirrorURL = this.hederaClient.mirrorUrl();
    }
}
