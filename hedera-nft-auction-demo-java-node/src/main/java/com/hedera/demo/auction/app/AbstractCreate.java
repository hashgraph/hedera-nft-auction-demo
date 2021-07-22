package com.hedera.demo.auction.app;

import com.hedera.demo.auction.app.repository.AuctionsRepository;
import com.hedera.demo.auction.app.repository.BidsRepository;
import com.hedera.demo.auction.app.repository.ValidatorsRepository;
import io.github.cdimascio.dotenv.Dotenv;

import java.util.Objects;
import java.util.Optional;

/**
 * Abstract class to support creation of accounts, topics, etc...
 */
public abstract class AbstractCreate {
    protected static Dotenv env = Dotenv.configure().ignoreIfMissing().load();
    protected HederaClient hederaClient;
    protected static String topicId;
    protected static String filesPath;
    protected static String url;
    protected static String database;
    protected static String username;
    protected static String password;

    protected static SqlConnectionManager connectionManager;
    protected static AuctionsRepository auctionsRepository;
    protected static BidsRepository bidsRepository;
    protected static ValidatorsRepository validatorsRepository;

    AbstractCreate() throws Exception {
        setEnv(env);
    }
    //For testing to override default .env
    public void setEnv(Dotenv overrideEnv) throws Exception {
        env = overrideEnv;
        hederaClient = new HederaClient(env);

        filesPath = Optional.ofNullable(env.get("FILES_LOCATION")).orElse("./sample-files");

        url = Objects.requireNonNull(env.get("DATABASE_URL"), "missing environment variable DATABASE_URL");
        database = Objects.requireNonNull(env.get("POSTGRES_DB"), "missing environment variable POSTGRES_DB");
        username = Objects.requireNonNull(env.get("POSTGRES_USER"), "missing environment variable POSTGRES_USER");
        password = Objects.requireNonNull(env.get("POSTGRES_PASSWORD"), "missing environment variable POSTGRES_PASSWORD");

        connectionManager = new SqlConnectionManager(url.concat(database), username, password);
        auctionsRepository = new AuctionsRepository(connectionManager);
        bidsRepository = new BidsRepository(connectionManager);
        validatorsRepository = new ValidatorsRepository(connectionManager);
    }
    //For testing to override topicId
    public void setTopicId(String overrideTopicId) {
        topicId = overrideTopicId;
    }

    //For testing to override connection manager
    public void setConnectionManager(SqlConnectionManager sqlConnectionManager) throws Exception {
        connectionManager = sqlConnectionManager;
        auctionsRepository = new AuctionsRepository(connectionManager);
        bidsRepository = new BidsRepository(connectionManager);
        validatorsRepository = new ValidatorsRepository(connectionManager);
    }
}
