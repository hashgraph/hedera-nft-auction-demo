package com.hedera.demo.auction.node.app;

import io.github.cdimascio.dotenv.Dotenv;

import java.util.Optional;

public abstract class AbstractCreate {
    protected static Dotenv env = Dotenv.configure().ignoreIfMissing().load();
    protected HederaClient hederaClient;
    protected static String topicId = Optional.ofNullable(env.get("VUE_APP_TOPIC_ID")).orElse("");

    AbstractCreate() throws Exception {
        hederaClient = new HederaClient(env);
    }
    //For testing to override default .env
    public void setEnv(Dotenv overrideEnv) throws Exception {
        env = overrideEnv;
        hederaClient = new HederaClient(env);
    }
}
