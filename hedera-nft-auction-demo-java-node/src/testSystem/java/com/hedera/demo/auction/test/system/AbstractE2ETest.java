package com.hedera.demo.auction.test.system;

import com.hedera.demo.auction.app.api.RequestCreateAuction;
import com.hedera.demo.auction.app.api.RequestCreateAuctionAccount;
import com.hedera.demo.auction.app.api.RequestCreateAuctionAccountKey;
import com.hedera.demo.auction.app.api.RequestCreateToken;
import com.hedera.demo.auction.app.api.RequestTokenTransfer;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;

public abstract class AbstractE2ETest extends AbstractSystemTest {
    protected int adminPort;

    private final WebClientOptions webClientOptions = new WebClientOptions()
            .setUserAgent("HederaAuction/1.0")
            .setKeepAlive(false)
            .setSsl(true)
            .setVerifyHost(true)
            .setTrustAll(true);
    protected WebClient webClient = WebClient.create(Vertx.vertx(), webClientOptions);

    protected AbstractE2ETest() throws Exception {
        super();
    }

    protected Buffer createTokenBody() {
        RequestCreateToken requestCreateToken = new RequestCreateToken();
        requestCreateToken.setName(tokenName);
        requestCreateToken.setSymbol(symbol);
        requestCreateToken.decimals = decimals;
        requestCreateToken.initialSupply = initialSupply;

        return JsonObject.mapFrom(requestCreateToken).toBuffer();
    }

    protected Buffer createAuctionAccountBody() {
        RequestCreateAuctionAccount requestCreateAuctionAccount = new RequestCreateAuctionAccount();
        RequestCreateAuctionAccountKey requestCreateAuctionAccountKey = new RequestCreateAuctionAccountKey();
        requestCreateAuctionAccountKey.key = hederaClient.operatorPublicKey().toString();
        requestCreateAuctionAccount.keylist.keys.add(requestCreateAuctionAccountKey);
        requestCreateAuctionAccount.keylist.threshold = 1;

        requestCreateAuctionAccount.initialBalance = initialBalance;

        return JsonObject.mapFrom(requestCreateAuctionAccount).toBuffer();
    }

    protected Buffer createTokenTransferBody() {
        RequestTokenTransfer requestTokenTransfer = new RequestTokenTransfer();
        if (tokenId != null) {
            requestTokenTransfer.tokenid = tokenId.toString();
        }
        if (auctionAccountId != null) {
            requestTokenTransfer.auctionaccountid = auctionAccountId.toString();
        }

        return JsonObject.mapFrom(requestTokenTransfer).toBuffer();
    }

    protected Buffer createAuctionBody() {
        RequestCreateAuction requestCreateAuction = new RequestCreateAuction();
        if (auctionAccountId != null) {
            requestCreateAuction.auctionaccountid = auctionAccountId.toString();
        }
        requestCreateAuction.reserve = auctionReserve;
        if (tokenId != null) {
            requestCreateAuction.tokenid = tokenId.toString();
        }
        requestCreateAuction.endtimestamp = endTimeStamp;
        if (topicId != null) {
            requestCreateAuction.topicid = topicId.toString();
        }
        requestCreateAuction.winnercanbid = winnerCanBid;
        requestCreateAuction.minimumbid = minimumBid;

        return JsonObject.mapFrom(requestCreateAuction).toBuffer();
    }

//    private static void addToEnvFromEnv(String key) {
//        environment.put(key, dotenv.get(key));
//    }

//    protected GenericContainer appContainer (PostgreSQLContainer postgreSQLContainer, String topicId, String mirrorProvider) {
//
//        addToEnvFromEnv("OPERATOR_ID");
//        addToEnvFromEnv("OPERATOR_KEY");
//        addToEnvFromEnv("NETWORK");
//        addToEnvFromEnv("REST_API");
//        addToEnvFromEnv("API_PORT");
//        addToEnvFromEnv("API_VERTICLE_COUNT");
//
//        addToEnvFromEnv("ADMIN_API_PORT");
//        addToEnvFromEnv("ADMIN_API_VERTICLE_COUNT");
//
//        addToEnvFromEnv("AUCTION_NODE");
//        addToEnvFromEnv("TRANSFER_ON_WIN");
//
//
//        addToEnvFromEnv("REST_HEDERA_MAINNET");
//        addToEnvFromEnv("REST_HEDERA_TESTNET");
//        addToEnvFromEnv("REST_HEDERA_PREVIEWNET");
//
//        environment.put("TOPIC_ID", topicId);
//        environment.put("MIRROR_PROVIDER", mirrorProvider);
//        environment.put("DATABASE_URL", "postgresql://pgdb:5432/postgres");
//        environment.put("POSTGRES_USER", postgreSQLContainer.getUsername());
//        environment.put("POSTGRES_PASSWORD", postgreSQLContainer.getPassword());
//        environment.put("MIRROR_QUERY_FREQUENCY", "10000");
//
//        GenericContainer container = new GenericContainer(
//                DockerImageName.parse("hedera/hedera-nft-auction-demo-node:latest")
//        )
//        .withExposedPorts(8081, 8082)
//        .withStartupTimeout(Duration.ofMinutes(1))
//        .withNetwork(testContainersNetwork)
//        .withEnv(environment)
//        .withLogConsumer(new Slf4jLogConsumer(LOGGER).withSeparateOutputStreams());
//
//        container.start();
//        adminPort = container.getMappedPort(8082);
//        return container;
//    }
}
