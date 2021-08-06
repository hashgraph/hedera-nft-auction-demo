package com.hedera.demo.auction.app;

import com.hedera.demo.auction.app.repository.AuctionsRepository;
import com.hedera.demo.auction.app.repository.BidsRepository;
import com.hedera.demo.auction.app.repository.ValidatorsRepository;
import io.github.cdimascio.dotenv.Dotenv;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.Optional;

/**
 * Abstract class to support creation of accounts, topics, etc...
 */
@SuppressWarnings("FieldMissingNullable")
public abstract class AbstractCreate {
    protected static Dotenv env = Dotenv.configure().ignoreIfMissing().load();
    protected HederaClient hederaClient;
    protected static String topicId = "";
    protected static String filesPath = "";
    protected static String url = "";
    protected static String database = "";
    protected static String username = "";
    protected static String password = "";

    @Nullable
    protected static SqlConnectionManager connectionManager;
    @Nullable
    protected static AuctionsRepository auctionsRepository;
    @Nullable
    protected static BidsRepository bidsRepository;
    @Nullable
    protected static ValidatorsRepository validatorsRepository;

    AbstractCreate() throws Exception {
        hederaClient = new HederaClient(env);
        setEnv(env);
    }

    public void setEnv(Dotenv overrideEnv) throws Exception {
        env = overrideEnv;
        hederaClient = new HederaClient(env);

        filesPath = Optional.ofNullable(env.get("FILES_LOCATION")).orElse("./sample-files");
        topicId = Objects.requireNonNull(env.get("TOPIC_ID"), "missing environment variable TOPIC_ID");

        String newUrl = Objects.requireNonNull(env.get("DATABASE_URL"), "missing environment variable DATABASE_URL");
        String newDatabase = Objects.requireNonNull(env.get("POSTGRES_DB"), "missing environment variable POSTGRES_DB");
        String newUsername = Objects.requireNonNull(env.get("POSTGRES_USER"), "missing environment variable POSTGRES_USER");
        String newPassword = Objects.requireNonNull(env.get("POSTGRES_PASSWORD"), "missing environment variable POSTGRES_PASSWORD");

        if (connectionManager != null) {
            String newConnection = newUrl.concat(newDatabase).concat(newUsername).concat(newPassword);
            String oldConnection = url.concat(database).concat(username).concat(password);

            if (! newConnection.equals(oldConnection)) {
                // close the connection if it's not null
                connectionManager.getConnection().close();
            }

        }

        url = newUrl;
        database = newDatabase;
        username = newUsername;
        password = newPassword;

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
        if (connectionManager != null) {
            // close the connection if it's not null
            connectionManager.getConnection().close();
        }
        connectionManager = sqlConnectionManager;
        auctionsRepository = new AuctionsRepository(connectionManager);
        bidsRepository = new BidsRepository(connectionManager);
        validatorsRepository = new ValidatorsRepository(connectionManager);
    }
}
