package com.hedera.demo.auction.test.system;

import com.hedera.demo.auction.app.*;
import com.hedera.demo.auction.app.api.RequestCreateAuction;
import com.hedera.demo.auction.app.api.RequestCreateAuctionAccount;
import com.hedera.demo.auction.app.api.RequestCreateAuctionAccountKey;
import com.hedera.demo.auction.app.api.RequestTokenTransfer;
import com.hedera.demo.auction.app.domain.Auction;
import com.hedera.demo.auction.app.domain.Bid;
import com.hedera.demo.auction.app.domain.Validator;
import com.hedera.demo.auction.app.repository.AuctionsRepository;
import com.hedera.demo.auction.app.repository.BidsRepository;
import com.hedera.demo.auction.app.repository.ValidatorsRepository;
import com.hedera.hashgraph.sdk.*;
import io.github.cdimascio.dotenv.Dotenv;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import lombok.extern.log4j.Log4j2;
import org.flywaydb.core.Flyway;
import org.junit.platform.commons.util.StringUtils;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;

import javax.annotation.Nullable;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.Callable;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@Log4j2
public abstract class AbstractSystemTest {
    protected static final Dotenv dotenv = Dotenv.configure().filename(".env.system").ignoreIfMissing().load();
    protected HederaClient hederaClient;
    protected HederaClient hederaTestRunnerClient;
    protected HederaClient hederaBidderClient;

    protected static Network testContainersNetwork = Network.newNetwork();

    protected PostgreSQLContainer<?> postgres;
    protected AuctionsRepository auctionsRepository;
    protected BidsRepository bidsRepository;
    protected ValidatorsRepository validatorsRepository;
    protected Auction auction;

    protected CreateTopic createTopic;
    protected CreateAuctionAccount createAuctionAccount;
    protected CreateTokenTransfer createTokenTransfer;
    protected CreateToken createToken;
    protected CreateAuction createAuction;
    protected EasySetup easySetup;

    protected static final long initialBalance = 10;
    @Nullable
    protected static AccountId auctionAccountId;
    @Nullable
    protected static AccountInfo accountInfo;
    @Nullable
    protected static AccountBalance accountBalance;
    protected static Map<String, Long> accountBalances = new HashMap<>();

    @Nullable
    protected static TopicId topicId;
    @Nullable
    protected static TopicInfo topicInfo;
    protected final static String POSTGRES_CONTAINER_VERSION = "postgres:17.4";

    protected static final String tokenName = "TestToken";
    protected static final String symbol = "TestSymbol";
    protected static final long initialSupply = 1;
    protected static final int decimals = 0;
    protected static final String tokenMemo = "token Memo";
    @Nullable
    protected static TokenId tokenId;
    @Nullable
    protected static TokenInfo tokenInfo;

    protected static final long minimumBid = 0;
    protected static final boolean winnerCanBid = true;
    protected long auctionReserve = 1000;
    protected final String endTimeStamp = "0000.12313";

    protected boolean transferOnWin = true;
    protected final PrivateKey auctionAccountKey = PrivateKey.generateED25519();

    protected long maxBid = 0;
    protected AccountId maxBidAccount;

    protected Map<String, AccountId> biddingAccounts = new HashMap<>();
    protected final PrivateKey bidAccountKey = PrivateKey.generateED25519();

    protected PrivateKey masterKey = PrivateKey.fromString(Objects.requireNonNull(dotenv.get("MASTER_KEY")));
    protected final String filesPath = Optional.ofNullable(dotenv.get("FILES_LOCATION")).orElse("./sample-files");

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
        createToken = new CreateToken(filesPath);
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
        JsonArray keys = JsonArray.EMPTY_JSON_ARRAY;
        JsonObject key1 = Json.createObjectBuilder().add("key", pubKey1).build();
        keys.add(key1);

        JsonObject keyList = Json.createObjectBuilder()
            .add("keys", keys)
            .add("threshold", threshold)
            .build();

        JsonObject key = Json.createObjectBuilder()
            .add("keylist", keyList)
            .build();
        return key;
    }

    protected static JsonObject jsonThresholdKey(int threshold, String pubKey1, String pubKey2) {
        JsonArray keys = JsonArray.EMPTY_JSON_ARRAY;
        JsonObject key1 = Json.createObjectBuilder().add("key", pubKey1).build();
        JsonObject key2 = Json.createObjectBuilder().add("key", pubKey2).build();
        keys.add(key1);
        keys.add(key2);

        JsonObject keyList = Json.createObjectBuilder()
            .add("keys", keys)
            .add("threshold", threshold)
            .build();

        JsonObject key = Json.createObjectBuilder()
            .add("keylist", keyList)
            .build();

        return key;
    }

    protected void createAccountAndGetInfo(JsonObject keys) throws Exception {
        var requestCreateAuctionAccount = new RequestCreateAuctionAccount();
        requestCreateAuctionAccount.initialBalance = initialBalance;
        JsonObject keyList = keys.getJsonObject("keylist");
        JsonArray keysArray = keyList.getJsonArray("keys");
        for (JsonValue keyObject : keysArray) {
            var requestCreateAuctionAccountKey = new RequestCreateAuctionAccountKey();
            requestCreateAuctionAccountKey.key = keyObject.asJsonObject().getString("key");
            requestCreateAuctionAccount.keylist.keys.add(requestCreateAuctionAccountKey);
        }

        requestCreateAuctionAccount.keylist.threshold = keyList.asJsonObject().getInt("threshold");
        auctionAccountId = createAuctionAccount.create(requestCreateAuctionAccount);
        getAccountInfo();
    }

    protected void getAccountInfo() throws Exception {
        accountInfo = new AccountInfoQuery()
                .setAccountId(auctionAccountId)
                .execute(hederaClient.client());
    }

    protected void createTokenAndGetInfo(String symbol) throws Exception {
        // simulating creation from a different account
        tokenOwnerPrivateKey = PrivateKey.generateED25519();

        // create a temp account for the token creation
        TransactionResponse accountCreateResponse = new AccountCreateTransaction()
                .setKeyWithoutAlias(tokenOwnerPrivateKey.getPublicKey())
                .setInitialBalance(Hbar.from(100))
                .execute(hederaClient.client());

        TransactionReceipt accountCreateResponseReceipt = accountCreateResponse.getReceipt(hederaClient.client());

        assertNotNull(accountCreateResponseReceipt);
        assertNotNull(accountCreateResponseReceipt.accountId);
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
        if (tokenId == null) {
            throw new Exception("tokenId is null");
        }
        if (auctionAccountId == null) {
            throw new Exception("auctionAccountId is null");
        }
        var requestTokenTransfer = new RequestTokenTransfer();
        requestTokenTransfer.tokenid = tokenId.toString();
        requestTokenTransfer.auctionaccountid = auctionAccountId.toString();
        createTokenTransfer.transfer(requestTokenTransfer, tokenOwnerAccountId, tokenOwnerPrivateKey);
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

        if (tokenId == null) {
            throw new Exception("tokenId is null");
        }
        if (auctionAccountId == null) {
            throw new Exception("auctionAccountId is null");
        }
        if (topicId == null) {
            throw new Exception("topicId is null");
        }

        var requestCreateAuction = new RequestCreateAuction();
        requestCreateAuction.tokenid = tokenId.toString();
        requestCreateAuction.auctionaccountid = auctionAccountId.toString();
        requestCreateAuction.reserve = reserve;
        requestCreateAuction.minimumbid = minimumBid;
        requestCreateAuction.winnercanbid = winnerCanBid;
        requestCreateAuction.topicid = topicId.toString();

        createAuction.create(requestCreateAuction);
    }

    protected void migrate(PostgreSQLContainer<?> postgres) {
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
        if (tokenId == null) {
            throw new Exception("tokenId is null");
        }
        if (auctionAccountId == null) {
            throw new Exception("auctionAccountId is null");
        }
        var requestTokenTransfer = new RequestTokenTransfer();
        requestTokenTransfer.tokenid = tokenId.toString();
        requestTokenTransfer.auctionaccountid = auctionAccountId.toString();
        createTokenTransfer.transfer(requestTokenTransfer, tokenOwnerAccountId, tokenOwnerPrivateKey);
    }

    protected Callable<Boolean> auctionsCountMatches(int matchCount, JsonObject assertion) {
        log.info("asserting {}", assertion.toString());
        return auctionsCountMatches(matchCount);
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
//TODO: Get balance from mirror
            return accountBalance.token.containsKey(tokenId);
        };
    }
    protected Callable<Boolean> tokenAssociated(JsonObject  assertion) {
        log.info("asserting {}", assertion.toString());
        return tokenAssociated();
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
//TODO: Get balance from mirror
            if (balance.token.containsKey(tokenId)) {
                if (balance.token.get(tokenId) == 0) {
                    return true;
                }
            }
            return false;
        };
    }

    protected Callable<Boolean> tokenTransferred(JsonObject assertion, AccountId accountId) {
        return () -> {
            log.info("asserting {}", assertion.toString());
            AccountBalance balance = new AccountBalanceQuery()
                    .setAccountId(accountId)
                    .execute(hederaClient.client());
//TODO: Get balance from mirror
            if (balance.token.containsKey(tokenId)) {
                if (balance.token.get(tokenId) != 0) {
                    return true;
                }
            }
            return false;
        };
    }

    protected Callable<Boolean> tokenNotTransferred(JsonObject assertion, AccountId accountId) {
        return () -> {
            log.info("asserting {}", assertion.toString());
            AccountBalance balance = new AccountBalanceQuery()
                    .setAccountId(accountId)
                    .execute(hederaClient.client());
//TODO: Get balance from mirror
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
                return auctionSource.getWinningAccount();
            case "winningBid":
                return auctionSource.getWinningBid().toString();
            case "endTimestamp":
                return auctionSource.getEndTimestamp();
            case "lastConsensusTimestamp":
                return auctionSource.getLastConsensusTimestamp();
            case "startTimestamp":
                return auctionSource.getStartTimestamp();
            case "status":
                return auctionSource.getStatus();
            case "transferTxHash":
                return auctionSource.getTransferTxHash();
            case "transferTxId":
                return auctionSource.getTransferTxId();
            case "winningTxHash":
                return auctionSource.getWinningTxHash();
            case "winningTxId":
                return auctionSource.getWinningTxId();
            case "tokenOwnerAccountId":
                return auctionSource.getTokenOwnerAccount();
            default:
                return "";
        }
    }

    protected Callable<Boolean> auctionValueAssert(JsonObject assertion, String parameter, String value, String condition) {
        return () -> {
            log.info("asserting {}", assertion.toString());
            Auction testAuction = auctionsRepository.getAuction(auction.getId());

            String valueToCheck = getAuctionValue(testAuction, parameter);

            return checkCondition(value, condition, valueToCheck);
        };
    }

    protected Callable<Boolean> checkBalance(JsonObject assertion, String account, String condition) {
        return () -> {
            log.info("asserting {}", assertion);
            AccountId accountId;
            if (account == null) {
                log.warn("account is null");
                return false;
            }
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
                    log.warn("Invalid account {} for getBalance task", account);
                    return false;
            }

            if (accountId == null) {
                log.warn("Cannot check balance of null account {}", account);
                return false;
            }

            AccountBalance balance = new AccountBalanceQuery()
                    .setAccountId(accountId)
                    .execute(testRunnerClient);

            if (accountBalances == null) {
                log.warn("balance query result is null");
                return false;
            } else {
                log.info("checking balance for {} {} {} than {}", account, balance.hbars.toTinybars(), condition, accountBalances.get(account));
                if (accountBalances.get(account) != null) {
                    if (condition.equals("greater")) {
                        return balance.hbars.toTinybars() > accountBalances.get(account);
                    } else if (condition.equals("smaller") || condition.equals("lower")) {
                        return balance.hbars.toTinybars() < accountBalances.get(account);
                    } else if (condition.equals("equals")) {
                        return balance.hbars.toTinybars() == accountBalances.get(account);
                    }
                    return false;
                } else {
                    return false;
                }
            }
        };
    }
    private static boolean checkCondition(String value, String condition, String valueToCheck) {
        log.info("Checking condition {} on value {} against {}", condition, value, valueToCheck);
        return switch (condition) {
            case "equals" -> value.equals(valueToCheck);
            case "notnull" -> StringUtils.isNotBlank(valueToCheck);
            case "isnull" -> StringUtils.isBlank(valueToCheck);
            case "true" -> valueToCheck.equals("true");
            case "false" -> valueToCheck.equals("false");
            default -> false;
        };

    }

    private static String getBidValue(Bid bidSource, String parameter) {
        return switch (parameter) {
            case "bidderAccountId" -> bidSource.getBidderaccountid();
            case "status" -> bidSource.getStatus();
            case "timestamp" -> bidSource.getTimestamp();
            case "transactionHash" -> bidSource.getTransactionhash();
            case "transactionId" -> bidSource.getTransactionid();
            case "refundTxHash" -> bidSource.getRefundtxhash();
            case "refundTxId" -> bidSource.getRefundtxid();
            case "bidAmount" -> String.valueOf(bidSource.getBidamount());
            case "refundstatus" -> bidSource.getRefundstatus();
            case "refunded" -> bidSource.isRefunded() ? "true" : "false";
            default -> "";
        };
    }

    protected Callable<Boolean> bidValueAssert(JsonObject assertion, String bidAccount, long bidAmount, String parameter, String value, String condition) throws SQLException {
        return () -> {
            log.info("asserting {}", assertion);
            Bid testBid = bidsRepository.getBid(auction.getId(), bidAccount, bidAmount);
            if (testBid == null) {
                return false;
            }
            String valueToCheck = getBidValue(testBid, parameter);

            return checkCondition(value, condition, valueToCheck);
        };
    }

    protected Callable<Boolean> validatorAssert(String name, String url, String publicKey) {
        return () -> {
            log.info("checking validator {} {} {}", name, url, publicKey);
            List<Validator> validators = validatorsRepository.getValidatorsList();
            if (validators.isEmpty()) {
                return false;
            }
            Validator validator = validators.get(0);

            boolean valid = name.equals(validator.getName()) && url.equals(validator.getUrl()) && publicKey.equals(validator.getPublicKey());
            return valid;
        };
    }

    protected Callable<Boolean> validatorsAssertCount() {
        return () -> {
            List<Validator> validators = validatorsRepository.getValidatorsList();
            return validators.isEmpty();
        };
    }

    protected static String[] keylistToStringArray(KeyList keyList) {
        Object[] accountKeysWithin = keyList.toArray();
        String[] pubKeys = new String[keyList.size()];

        for (int i=0; i < keyList.size(); i++) {
            var pubKey = (Key)accountKeysWithin[i];
            pubKeys[i] = pubKey.toString();
        }

        return pubKeys;
    }

}
