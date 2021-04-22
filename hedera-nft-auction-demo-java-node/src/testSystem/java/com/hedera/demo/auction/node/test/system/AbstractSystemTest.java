package com.hedera.demo.auction.node.test.system;

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
import com.hedera.hashgraph.sdk.AccountCreateTransaction;
import com.hedera.hashgraph.sdk.AccountId;
import com.hedera.hashgraph.sdk.AccountInfo;
import com.hedera.hashgraph.sdk.AccountInfoQuery;
import com.hedera.hashgraph.sdk.Client;
import com.hedera.hashgraph.sdk.Hbar;
import com.hedera.hashgraph.sdk.Key;
import com.hedera.hashgraph.sdk.KeyList;
import com.hedera.hashgraph.sdk.PrecheckStatusException;
import com.hedera.hashgraph.sdk.PrivateKey;
import com.hedera.hashgraph.sdk.ReceiptStatusException;
import com.hedera.hashgraph.sdk.TokenCreateTransaction;
import com.hedera.hashgraph.sdk.TokenId;
import com.hedera.hashgraph.sdk.TokenInfo;
import com.hedera.hashgraph.sdk.TokenInfoQuery;
import com.hedera.hashgraph.sdk.TopicId;
import com.hedera.hashgraph.sdk.TopicInfo;
import com.hedera.hashgraph.sdk.TopicInfoQuery;
import com.hedera.hashgraph.sdk.TransactionReceipt;
import com.hedera.hashgraph.sdk.TransactionResponse;
import com.hedera.hashgraph.sdk.TransferTransaction;
import io.github.cdimascio.dotenv.Dotenv;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.flywaydb.core.Flyway;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;

import java.io.File;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeoutException;

public abstract class AbstractSystemTest {
    protected static final Dotenv dotenv = Dotenv.configure().filename(".env.system").ignoreIfMissing().load();
    protected HederaClient hederaClient;

    protected static Network testContainersNetwork = Network.newNetwork();

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

    protected static final long minimumBid = 0;
    protected static final boolean winnerCanBid = true;
    protected final long auctionReserve = 1000;
    protected final String endTimeStamp = "0000.12313";


    // test token owner
    PrivateKey tokenOwnerPrivateKey;
    AccountId tokenOwnerAccountId;
    Client tokenOwnerClient;

    protected AbstractSystemTest() throws Exception {
        hederaClient = new HederaClient(dotenv);

        createTopic = new CreateTopic();
        createAuctionAccount = new CreateAuctionAccount();
        createToken = new CreateToken();
        createTokenTransfer = new CreateTokenTransfer();
        createAuction = new CreateAuction();

        createTopic.setEnv(dotenv); // other "create" classes share the same abstract class, no need to repeat
    }

    protected static JsonObject jsonThresholdKey(int threshold, PrivateKey pk1, PrivateKey pk2) {
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
        getAccountInfo();
    }

    protected void getAccountInfo() throws Exception {
        accountInfo = new AccountInfoQuery()
                .setAccountId(auctionAccountId)
                .execute(hederaClient.client());
    }

    protected void createTokenAndGetInfo(String symbol) throws Exception {
        tokenId = createToken.create(tokenName, symbol, initialSupply, decimals);
        getTokenInfo();
    }

    protected void getTokenInfo() throws Exception {
        tokenInfo = new TokenInfoQuery()
                .setTokenId(tokenId)
                .execute(hederaClient.client());
    }

    protected void transferTokenAndGetBalance() throws Exception {
        createTokenTransfer.transfer(tokenId.toString(), auctionAccountId.toString());
        getAccountBalance();
    }

    protected void getAccountBalance() throws Exception {
        accountBalance = new AccountBalanceQuery()
                .setAccountId(auctionAccountId)
                .execute(hederaClient.client());
    }

    protected void createTopicAndGetInfo() throws Exception {
        topicId = createTopic.create();
        // check topic Id exists on Hedera
        getTopicInfo();
    }

    protected void getTopicInfo() throws Exception {
        topicInfo = new TopicInfoQuery()
                .setTopicId(topicId)
                .execute(hederaClient.client());
    }

    protected void createAuction(long reserve, long minimumBid, boolean winnerCanBid) throws Exception {
        createTopicAndGetInfo();
        // auction account id
        createAccountAndGetInfo("");
        // simulating creation from a different account
        tokenOwnerPrivateKey = PrivateKey.generate();

        // create a temp account for the token creation
        TransactionResponse accountCreateResponse = new AccountCreateTransaction()
                .setKey(tokenOwnerPrivateKey.getPublicKey())
                .setInitialBalance(Hbar.from(100))
                .execute(hederaClient.client());

        TransactionReceipt accountCreateResponseReceipt = accountCreateResponse.getReceipt(hederaClient.client());

        tokenOwnerAccountId = accountCreateResponseReceipt.accountId;

        tokenOwnerClient = Client.forTestnet();
        tokenOwnerClient.setOperator(tokenOwnerAccountId, tokenOwnerPrivateKey);

        // create a token
        TransactionResponse tokenCreateResponse = new TokenCreateTransaction()
                .setTokenName(tokenName)
                .setTokenSymbol(symbol)
                .setDecimals(0)
                .setInitialSupply(1)
                .setTreasuryAccountId(tokenOwnerAccountId)
                .execute(tokenOwnerClient);

        TransactionReceipt tokenCreateResponseReceipt = tokenCreateResponse.getReceipt(hederaClient.client());

        tokenId = tokenCreateResponseReceipt.tokenId;

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

    protected void transferToken() throws TimeoutException, PrecheckStatusException, ReceiptStatusException {
        // transfer token to auction
        TransactionResponse tokenTransferResponse = new TransferTransaction()
                .addTokenTransfer(tokenId, tokenOwnerAccountId, -1)
                .addTokenTransfer(tokenId, auctionAccountId, 1)
                .execute(tokenOwnerClient);

        tokenTransferResponse.getReceipt(tokenOwnerClient);
    }

    protected Callable<Boolean> auctionsCountMatches(int matchCount) throws SQLException {
        return () -> {
            List<Auction> auctionsList = auctionsRepository.getAuctionsList();
            return (auctionsList.size() == matchCount);
        };
    }

    protected Callable<Boolean> tokenAssociatedNotTransferred() throws SQLException {
        return () -> {
            AccountBalance balance = new AccountBalanceQuery()
                    .setAccountId(auctionAccountId)
                    .execute(hederaClient.client());
            if (balance.token.containsKey(tokenId)) {
                if (balance.token.get(tokenId) == 0) {
                    return true;
                }
            }
            return false;
        };
    }

    private static String getAuctionValue(Auction auctionSource, String parameter) {
        switch (parameter) {
            case "winningAccount":
                return auctionSource.getWinningaccount();
            case "winningBid":
                return auctionSource.getWinningbid().toString();
            case "endTimestamp":
                return auctionSource.getEndtimestamp();
            case "lastConsensusTimestamp":
                return auctionSource.getLastconsensustimestamp();
            case "startTimestamp":
                return auctionSource.getStarttimestamp();
            case "status":
                return auctionSource.getStatus();
            case "transferTxHash":
                return auctionSource.getTransfertxhash();
            case "transferTxId":
                return auctionSource.getTransfertxid();
            case "winningTxHash":
                return auctionSource.getWinningtxhash();
            case "winningTxId":
                return auctionSource.getWinningtxid();
            default:
                return "";
        }
    }

    protected Callable<Boolean> auctionValueAssert(String parameter, String value, String condition) throws SQLException {
        return () -> {
            Auction testAuction = auctionsRepository.getAuction(auction.getId());

            String valueToCheck = getAuctionValue(testAuction, parameter);

            if (condition.equals("equals")) {
                return (value.equals(valueToCheck));
            }

            return false;
        };
    }

    protected static String[] keylistToStringArray(KeyList keyList) {
        Object[] accountKeysWithin = keyList.toArray();
        String[] pubKeys = new String[keyList.size()];

        for (int i=0; i < keyList.size(); i++) {
            Key pubKey = (Key)accountKeysWithin[i];
            pubKeys[i] = pubKey.toString();
        }

        return pubKeys;
    }

}
