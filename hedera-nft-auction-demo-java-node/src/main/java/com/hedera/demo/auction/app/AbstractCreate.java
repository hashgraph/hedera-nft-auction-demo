package com.hedera.demo.auction.app;

import io.github.cdimascio.dotenv.Dotenv;

import java.util.Optional;

/**
 * Abstract class to support creation of accounts, topics, etc...
 */
public abstract class AbstractCreate {
    protected static Dotenv env = Dotenv.configure().ignoreIfMissing().load();
    protected HederaClient hederaClient;
    @SuppressWarnings("FieldMissingNullable")
    protected static String topicId = Optional.ofNullable(env.get("TOPIC_ID")).orElse("");

    AbstractCreate() throws Exception {
        hederaClient = new HederaClient(env);
    }
    //For testing to override default .env
    public void setEnv(Dotenv overrideEnv) throws Exception {
        env = overrideEnv;
        hederaClient = new HederaClient(env);
    }
}
