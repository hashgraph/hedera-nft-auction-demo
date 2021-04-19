package com.hedera.demo.auction.node.test.system.app;

import com.hedera.demo.auction.node.app.CreateAuction;
import com.hedera.demo.auction.node.app.CreateAuctionAccount;
import com.hedera.demo.auction.node.app.CreateToken;
import com.hedera.demo.auction.node.app.CreateTokenTransfer;
import com.hedera.demo.auction.node.app.CreateTopic;
import com.hedera.demo.auction.node.app.HederaClient;
import com.hedera.demo.auction.node.app.domain.Auction;
import com.hedera.demo.auction.node.app.repository.AuctionsRepository;
import com.hedera.demo.auction.node.app.repository.BidsRepository;
import com.hedera.hashgraph.sdk.AccountBalance;
import com.hedera.hashgraph.sdk.AccountBalanceQuery;
import com.hedera.hashgraph.sdk.AccountId;
import com.hedera.hashgraph.sdk.AccountInfo;
import com.hedera.hashgraph.sdk.AccountInfoQuery;
import com.hedera.hashgraph.sdk.PrivateKey;
import com.hedera.hashgraph.sdk.TokenId;
import com.hedera.hashgraph.sdk.TokenInfo;
import com.hedera.hashgraph.sdk.TokenInfoQuery;
import com.hedera.hashgraph.sdk.TopicId;
import com.hedera.hashgraph.sdk.TopicInfo;
import com.hedera.hashgraph.sdk.TopicInfoQuery;
import io.github.cdimascio.dotenv.Dotenv;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.flywaydb.core.Flyway;
import org.testcontainers.containers.PostgreSQLContainer;

import java.io.File;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.Callable;

public abstract class AbstractSystemTest {
    protected static final Dotenv dotenv = Dotenv.configure().filename(".env.system").ignoreIfMissing().load();
    protected HederaClient hederaClient;


    protected PostgreSQLContainer postgres;
    protected AuctionsRepository auctionsRepository;
    protected BidsRepository bidsRepository;
    protected Auction auction;

    protected CreateTopic createTopic;
    protected CreateAuctionAccount createAuctionAccount;
    protected CreateTokenTransfer createTokenTransfer;
    protected CreateToken createToken;
    protected CreateAuction createAuction;

    protected static final long initialBalance = 10;
    protected static AccountId auctionAccountId;
    protected static AccountInfo accountInfo;
    protected static AccountBalance accountBalance;

    protected static TopicId topicId;
    protected static TopicInfo topicInfo;

    protected static final String tokenName = "TestToken";
    protected static final String symbol = "TestSymbol";
    protected static final long initialSupply = 10;
    protected static final int decimals = 2;
    protected static TokenId tokenId;
    protected static TokenInfo tokenInfo;

    AbstractSystemTest() throws Exception {
        hederaClient = new HederaClient(dotenv);

        createTopic = new CreateTopic();
        createAuctionAccount = new CreateAuctionAccount();
        createToken = new CreateToken();
        createTokenTransfer = new CreateTokenTransfer();
        createAuction = new CreateAuction();

        createTopic.setEnv(dotenv); // other "create" classes share the same abstract class, no need to repeat
    }

    static JsonObject jsonThresholdKey(int threshold, PrivateKey pk1, PrivateKey pk2) {
        JsonObject thresholdKey = new JsonObject();
        if (threshold != 0) {
            thresholdKey.put("threshold", threshold);
        }
        JsonArray keyList = new JsonArray();
        keyList.add(new JsonObject().put("key", pk1.getPublicKey().toString()));
        keyList.add(new JsonObject().put("key", pk2.getPublicKey().toString()));
        thresholdKey.put("keys", keyList);

        return thresholdKey;
    }

    protected void createAccountAndGetInfo(String keys) throws Exception {
        auctionAccountId = createAuctionAccount.create(initialBalance, keys);
        accountInfo = new AccountInfoQuery()
                .setAccountId(auctionAccountId)
                .execute(hederaClient.client());
    }

    protected void createTokenAndGetInfo(String symbol) throws Exception {
        tokenId = createToken.create(tokenName, symbol, initialSupply, decimals);
        tokenInfo = new TokenInfoQuery()
                .setTokenId(tokenId)
                .execute(hederaClient.client());
    }

    protected void transferTokenAndGetBalance() throws Exception {
        createTokenTransfer.transfer(tokenId.toString(), auctionAccountId.toString());

        accountBalance = new AccountBalanceQuery()
                .setAccountId(auctionAccountId)
                .execute(hederaClient.client());
    }

    protected void createTopicAndGetInfo() throws Exception {
        topicId = createTopic.create();
        // check topic Id exists on Hedera
        topicInfo = new TopicInfoQuery()
                .setTopicId(topicId)
                .execute(hederaClient.client());
    }

    protected void createAuction(long reserve, long minimumBid, boolean winnerCanBid) throws Exception {
        createTopicAndGetInfo();
        createAccountAndGetInfo("");
        createTokenAndGetInfo(symbol);

        JsonObject auction = new JsonObject();
        auction.put("tokenid", tokenId.toString());
        auction.put("auctionaccountid", auctionAccountId.toString());
        auction.put("reserve", reserve);
        auction.put("minimumbid", minimumBid);
        auction.put("winnercanbid", winnerCanBid);

        // store auction data in temp.json file
        File tempFile = File.createTempFile("test-", ".json");
        PrintWriter printWriter = new PrintWriter(Files.newBufferedWriter(tempFile.toPath(), StandardCharsets.UTF_8));
        printWriter.write(auction.encodePrettily());
        printWriter.close();

        createAuction.create(tempFile.getAbsolutePath(), topicId.toString());
    }

    protected void migrate(PostgreSQLContainer postgres) {
        String postgresUrl = postgres.getJdbcUrl();
        String postgresUser = postgres.getUsername();
        String postgresPassword = postgres.getPassword();
        Flyway flyway = Flyway
                .configure()
                .dataSource(postgresUrl, postgresUser, postgresPassword)
                .locations("filesystem:./src/main/resources/migrations")
                .load();
        flyway.migrate();
    }

    protected Callable<Boolean> auctionsCountMatches(int matchCount) throws SQLException {
        return () -> {
            System.out.println("AuctionsCountEquals");
            List<Auction> auctionsList = auctionsRepository.getAuctionsList();
            return (auctionsList.size() == matchCount);
        };
    }
}
