package com.hedera.demo.auction.node.test.system;

import com.hedera.demo.auction.node.app.*;
import com.hedera.demo.auction.node.app.domain.Auction;
import com.hedera.demo.auction.node.app.domain.Bid;
import com.hedera.demo.auction.node.app.repository.AuctionsRepository;
import com.hedera.demo.auction.node.app.repository.BidsRepository;
import com.hedera.hashgraph.sdk.*;
import io.github.cdimascio.dotenv.Dotenv;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import lombok.extern.log4j.Log4j2;
import org.flywaydb.core.Flyway;
import org.junit.platform.commons.util.StringUtils;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;

import java.io.File;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

@Log4j2
public abstract class AbstractSystemTest {
    protected static final Dotenv dotenv = Dotenv.configure().filename(".env.system").ignoreIfMissing().load();
    protected HederaClient hederaClient;
    protected HederaClient hederaTestRunnerClient;
    protected HederaClient hederaBidderClient;

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

    protected static final long initialBalance = 100;
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

    protected boolean transferOnWin = true;
    protected final PrivateKey auctionAccountKey = PrivateKey.generate();

    protected long maxBid = 0;
    protected AccountId maxBidAccount;

    protected Map<String, AccountId> biddingAccounts = new HashMap<>();
    protected final PrivateKey bidAccountKey = PrivateKey.generate();

    // test token owner
    PrivateKey tokenOwnerPrivateKey;
    protected AccountId tokenOwnerAccountId;
    protected Client tokenOwnerClient;
    protected Client bidderClient;
    protected Client testRunnerClient;

    protected AbstractSystemTest() throws Exception {
        hederaClient = new HederaClient(dotenv);
        hederaTestRunnerClient = new HederaClient(dotenv);
        hederaBidderClient = new HederaClient(dotenv);

        bidderClient = hederaBidderClient.client();
        testRunnerClient = hederaTestRunnerClient.client();

        createTopic = new CreateTopic();
        createAuctionAccount = new CreateAuctionAccount();
        createToken = new CreateToken();
        createTokenTransfer = new CreateTokenTransfer();
        createAuction = new CreateAuction();

        createTopic.setEnv(dotenv); // other "create" classes share the same abstract class, no need to repeat
    }

    protected void setMaxBid(long newMaxBid, AccountId newAccount) {
        if (newMaxBid > maxBid) {
            maxBid = newMaxBid;
            maxBidAccount = newAccount;
        }
    }

    protected static JsonObject jsonThresholdKey(int threshold, PrivateKey pk1) {
        JsonObject thresholdKey = new JsonObject();
        if (threshold != 0) {
            thresholdKey.put("threshold", threshold);
        }
        JsonArray keyList = new JsonArray();
        keyList.add(new JsonObject().put("key", pk1.getPublicKey().toString()));
        thresholdKey.put("keys", keyList);

        return thresholdKey;
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
                .setDecimals(decimals)
                .setInitialSupply(initialSupply)
                .setTreasuryAccountId(tokenOwnerAccountId)
                .execute(tokenOwnerClient);

        TransactionReceipt tokenCreateResponseReceipt = tokenCreateResponse.getReceipt(hederaClient.client());

        tokenId = tokenCreateResponseReceipt.tokenId;

        getTokenInfo();
    }

    protected void getTokenInfo() throws Exception {
        tokenInfo = new TokenInfoQuery()
                .setTokenId(tokenId)
                .execute(hederaClient.client());
    }

    protected void transferTokenAndGetBalance() throws Exception {
        createTokenTransfer.transfer(tokenId.toString(), auctionAccountId.toString(), tokenOwnerAccountId, tokenOwnerPrivateKey);
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

    protected void transferToken() throws Exception {
        createTokenTransfer.transfer(tokenId.toString(), auctionAccountId.toString(), tokenOwnerAccountId, tokenOwnerPrivateKey);
    }

    protected Callable<Boolean> auctionsCountMatches(int matchCount) {
        return () -> {
            List<Auction> auctionsList = auctionsRepository.getAuctionsList();
            return (auctionsList.size() == matchCount);
        };
    }

    protected Callable<Boolean> alwaysTrueForDelay() {
        return () -> {
            return true;
        };
    }

    protected Callable<Boolean> tokenAssociatedNotTransferred() {
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

    protected Callable<Boolean> tokenTransferred(AccountId accountId) {
        return () -> {
            AccountBalance balance = new AccountBalanceQuery()
                    .setAccountId(accountId)
                    .execute(hederaClient.client());
            if (balance.token.containsKey(tokenId)) {
                if (balance.token.get(tokenId) != 0) {
                    return true;
                }
            }
            return false;
        };
    }

    protected Callable<Boolean> tokenNotTransferred(AccountId accountId) {
        return () -> {
            AccountBalance balance = new AccountBalanceQuery()
                    .setAccountId(accountId)
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
            case "tokenOwnerAccountId":
                return auctionSource.getTokenowneraccount();
            default:
                return "";
        }
    }

    protected Callable<Boolean> auctionValueAssert(String parameter, String value, String condition) {
        return () -> {
            Auction testAuction = auctionsRepository.getAuction(auction.getId());

            String valueToCheck = getAuctionValue(testAuction, parameter);

            return checkCondition(value, condition, valueToCheck);
        };
    }

    private static boolean checkCondition(String value, String condition, String valueToCheck) {
        log.info("Checking condition " + condition + " on value " + value + " against " + valueToCheck);
        if (condition.equals("equals")) {
            return (value.equals(valueToCheck));
        } else if (condition.equals("notnull")) {
            return (StringUtils.isNotBlank(valueToCheck));
        } else if (condition.equals("isnull")) {
            return (StringUtils.isBlank(valueToCheck));
        } else if (condition.equals("true")) {
            return (valueToCheck.equals("true"));
        } else if (condition.equals("false")) {
            return (valueToCheck.equals("false"));
        }

        return false;
    }

    private static String getBidValue(Bid bidSource, String parameter) {
        switch (parameter) {
            case "bidderAccountId":
                return bidSource.getBidderaccountid();
            case "status":
                return bidSource.getStatus();
            case "timestamp":
                return bidSource.getTimestamp();
            case "transactionHash":
                return bidSource.getTransactionhash();
            case "transactionId":
                return bidSource.getTransactionid();
            case "refundTxHash":
                return bidSource.getRefundtxhash();
            case "refundTxId":
                return bidSource.getRefundtxid();
            case "bidAmount":
                return String.valueOf(bidSource.getBidamount());
            case "refunded":
                return bidSource.getRefunded().toString();
            default:
                return "";
        }
    }

    protected Callable<Boolean> bidValueAssert(String bidAccount, long bidAmount, String parameter, String value, String condition) throws SQLException {
        return () -> {
            Bid testBid = bidsRepository.getBid(auction.getId(), bidAccount, bidAmount);
            if (testBid == null) {
                return false;
            }
            String valueToCheck = getBidValue(testBid, parameter);

            return checkCondition(value, condition, valueToCheck);
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
