package com.hedera.demo.auction.test.system;

import com.hedera.demo.auction.app.CreateAuction;
import com.hedera.demo.auction.app.CreateAuctionAccount;
import com.hedera.demo.auction.app.CreateToken;
import com.hedera.demo.auction.app.CreateTokenTransfer;
import com.hedera.demo.auction.app.CreateTopic;
import com.hedera.demo.auction.app.EasySetup;
import com.hedera.demo.auction.app.HederaClient;
import com.hedera.demo.auction.app.domain.Auction;
import com.hedera.demo.auction.app.domain.Bid;
import com.hedera.demo.auction.app.repository.AuctionsRepository;
import com.hedera.demo.auction.app.repository.BidsRepository;
import com.hedera.demo.auction.app.repository.ScheduledOperationsRepository;
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
import com.hedera.hashgraph.sdk.PrivateKey;
import com.hedera.hashgraph.sdk.TokenCreateTransaction;
import com.hedera.hashgraph.sdk.TokenId;
import com.hedera.hashgraph.sdk.TokenInfo;
import com.hedera.hashgraph.sdk.TokenInfoQuery;
import com.hedera.hashgraph.sdk.TopicId;
import com.hedera.hashgraph.sdk.TopicInfo;
import com.hedera.hashgraph.sdk.TopicInfoQuery;
import com.hedera.hashgraph.sdk.TransactionReceipt;
import com.hedera.hashgraph.sdk.TransactionResponse;
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
    protected ScheduledOperationsRepository scheduledOperationsRepository;
    protected Auction auction;

    protected CreateTopic createTopic;
    protected CreateAuctionAccount createAuctionAccount;
    protected CreateTokenTransfer createTokenTransfer;
    protected CreateToken createToken;
    protected CreateAuction createAuction;
    protected EasySetup easySetup;

    protected static final long initialBalance = 100;
    protected static AccountId auctionAccountId;
    protected static AccountInfo accountInfo;
    protected static AccountBalance accountBalance;
    protected static Map<String, Long> accountBalances = new HashMap<>();

    protected static TopicId topicId;
    protected static TopicInfo topicInfo;

    protected static final String tokenName = "TestToken";
    protected static final String symbol = "TestSymbol";
    protected static final long initialSupply = 1;
    protected static final int decimals = 0;
    protected static final String tokenMemo = "token Memo";
    protected static TokenId tokenId;
    protected static TokenInfo tokenInfo;

    protected static final long minimumBid = 0;
    protected static final boolean winnerCanBid = true;
    protected long auctionReserve = 1000;
    protected final String endTimeStamp = "0000.12313";

    protected boolean transferOnWin = true;
    protected final PrivateKey auctionAccountKey = PrivateKey.generate();

    protected long maxBid = 0;
    protected AccountId maxBidAccount;

    protected Map<String, AccountId> biddingAccounts = new HashMap<>();
    protected final PrivateKey bidAccountKey = PrivateKey.generate();

    protected PrivateKey masterKey = PrivateKey.generate();

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
        easySetup = new EasySetup();

        createTopic.setEnv(dotenv); // other "create" classes share the same abstract class, no need to repeat
    }

    protected void setMaxBid(long newMaxBid, AccountId newAccount) {
        if (newMaxBid > maxBid) {
            maxBid = newMaxBid;
            maxBidAccount = newAccount;
        }
    }

    protected static JsonObject jsonThresholdKey(int threshold, String pubKey1) {
        JsonArray keys = new JsonArray();
        JsonObject key1 = new JsonObject().put("key", pubKey1);
        keys.add(key1);

        JsonObject keyList = new JsonObject();
        keyList.put("keys", keys);
        keyList.put("threshold", threshold);

        JsonObject key = new JsonObject();
        key.put("keyList", keyList);
        return key;
    }

    protected static JsonObject jsonThresholdKey(int threshold, String pubKey1, String pubKey2) {
        JsonArray keys = new JsonArray();
        JsonObject key1 = new JsonObject().put("key", pubKey1);
        JsonObject key2 = new JsonObject().put("key", pubKey2);
        keys.add(key1).add(key2);

        JsonObject keyList = new JsonObject();
        keyList.put("keys", keys);
        keyList.put("threshold", threshold);

        JsonObject key = new JsonObject();
        key.put("keyList", keyList);

        return key;
    }

    protected static JsonObject jsonThresholdKey(int threshold1, int threshold2, String masterPubKey, String pubKey2) {
        JsonObject masterKey = new JsonObject().put("key", masterPubKey);

        JsonArray otherKeys = new JsonArray();
        JsonObject key1 = new JsonObject().put("key", pubKey2);
        otherKeys.add(key1);

        JsonObject otherKeyList = new JsonObject();
        otherKeyList.put("keys", otherKeys);
        otherKeyList.put("threshold", threshold1);
        JsonObject otherKeysObject = new JsonObject();
        otherKeysObject.put("keyList", otherKeyList);

        JsonArray keys = new JsonArray();
        keys.add(masterKey);
        keys.add(otherKeysObject);

        JsonObject key = new JsonObject();
        JsonObject keyList = new JsonObject();
        keyList.put("keys", keys);
        keyList.put("threshold", threshold2);
        key.put("keyList", keyList);

        return key;
    }

    protected static JsonObject jsonThresholdKey(int threshold1, int threshold2, String masterPubKey, String pubKey2, String pubKey3) {
        JsonObject masterKey = new JsonObject().put("key", masterPubKey);

        JsonArray otherKeys = new JsonArray();
        JsonObject key1 = new JsonObject().put("key", pubKey2);
        JsonObject key2 = new JsonObject().put("key", pubKey3);
        otherKeys.add(key1).add(key2);

        JsonObject otherKeyList = new JsonObject();
        otherKeyList.put("keys", otherKeys);
        otherKeyList.put("threshold", threshold1);
        JsonObject otherKeysObject = new JsonObject();
        otherKeysObject.put("keyList", otherKeyList);

        JsonArray keys = new JsonArray();
        keys.add(masterKey);
        keys.add(otherKeysObject);

        JsonObject key = new JsonObject();
        JsonObject keyList = new JsonObject();
        keyList.put("keys", keys);
        keyList.put("threshold", threshold2);
        key.put("keyList", keyList);

        return key;
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

    protected Callable<Boolean> tokenAssociated() {
        return () -> {
            AccountBalance accountBalance = new AccountBalanceQuery()
                    .setAccountId(auctionAccountId)
                    .execute(hederaClient.client());

            return accountBalance.token.containsKey(tokenId);
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

    protected Callable<Boolean> checkBalance(String account, String condition) {
        return () -> {
            AccountId accountId;
            switch (account) {
                case "tokenOwner":
                    accountId = tokenOwnerAccountId;
                    break;
                case "auctionAccount":
                    accountId = auctionAccountId;
                    break;
                case "winner":
                    accountId = maxBidAccount;
                    break;
                default:
                    log.warn("Invalid account " + account + " for getBalance task");
                    return false;
            }

            if (accountId == null) {
                log.warn("Cannot check balance of null account " + account);
                return false;
            }

            AccountBalance balance = new AccountBalanceQuery()
                    .setAccountId(accountId)
                    .execute(testRunnerClient);

            log.info("checking balance for " + account + " " + balance.hbars.toTinybars() + " " + condition + " than " + accountBalances.get(account));
            if (condition.equals("greater")) {
                return balance.hbars.toTinybars() > accountBalances.get(account);
            } else if (condition.equals("smaller") || condition.equals("lower")) {
                return balance.hbars.toTinybars() < accountBalances.get(account);
            } else if (condition.equals("equals")) {
                return balance.hbars.toTinybars() == accountBalances.get(account);
            }
            return false;
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
            case "refundstatus":
                return bidSource.getRefundstatus();
            case "refunded":
                return bidSource.isRefunded() ? "true" : "false";
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
