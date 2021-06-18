package com.hedera.demo.auction.test.system;

import com.hedera.demo.auction.app.api.RequestCreateAuction;
import com.hedera.demo.auction.app.api.RequestCreateAuctionAccount;
import com.hedera.demo.auction.app.api.RequestCreateToken;
import com.hedera.demo.auction.app.api.RequestTokenTransfer;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

public abstract class AbstractE2ETest extends AbstractSystemTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractE2ETest.class);
    private static final Map<String, String> environment = new HashMap<>();
    protected int adminPort;

    private final WebClientOptions webClientOptions = new WebClientOptions()
            .setUserAgent("HederaAuction/1.0")
            .setKeepAlive(false);
    protected WebClient webClient = WebClient.create(Vertx.vertx(), webClientOptions);

    protected AbstractE2ETest() throws Exception {
        super();
    }

    protected Buffer createTokenBody() {
        RequestCreateToken requestCreateToken = new RequestCreateToken();
        requestCreateToken.name = tokenName;
        requestCreateToken.symbol = symbol;
        requestCreateToken.decimals = decimals;
        requestCreateToken.initialSupply = initialSupply;

        return JsonObject.mapFrom(requestCreateToken).toBuffer();
    }

    protected Buffer createAuctionAccountBody() {
        RequestCreateAuctionAccount requestCreateAuctionAccount = new RequestCreateAuctionAccount();
        requestCreateAuctionAccount.initialBalance = initialBalance;
        requestCreateAuctionAccount.keylist = new JsonArray();

        return JsonObject.mapFrom(requestCreateAuctionAccount).toBuffer();
    }

    protected Buffer createTokenTransferBody() {
        RequestTokenTransfer requestTokenTransfer = new RequestTokenTransfer();
        requestTokenTransfer.tokenid = tokenId.toString();
        requestTokenTransfer.auctionaccountid = auctionAccountId.toString();

        return JsonObject.mapFrom(requestTokenTransfer).toBuffer();
    }

    protected Buffer createAuctionBody() {
        RequestCreateAuction requestCreateAuction = new RequestCreateAuction();
        requestCreateAuction.auctionaccountid = auctionAccountId.toString();
        requestCreateAuction.reserve = auctionReserve;
        requestCreateAuction.tokenid = tokenId.toString();
        requestCreateAuction.endtimestamp = endTimeStamp;
        requestCreateAuction.topicId = topicId.toString();
        requestCreateAuction.winnercanbid = winnerCanBid;
        requestCreateAuction.minimumbid = minimumBid;

        return JsonObject.mapFrom(requestCreateAuction).toBuffer();
    }

    private static void addToEnvFromEnv(String key) {
        environment.put(key, dotenv.get(key));
    }

    protected GenericContainer appContainer (PostgreSQLContainer postgreSQLContainer, String topicId, String mirrorProvider) {

        addToEnvFromEnv("OPERATOR_ID");
        addToEnvFromEnv("OPERATOR_KEY");
        addToEnvFromEnv("VUE_APP_NETWORK");
        addToEnvFromEnv("REST_API");
        addToEnvFromEnv("VUE_APP_API_PORT");
        addToEnvFromEnv("API_VERTICLE_COUNT");

        addToEnvFromEnv("ADMIN_API");
        addToEnvFromEnv("ADMIN_API_PORT");
        addToEnvFromEnv("ADMIN_API_VERTICLE_COUNT");

        addToEnvFromEnv("AUCTION_NODE");
        addToEnvFromEnv("TRANSFER_ON_WIN");
        addToEnvFromEnv("REFUND");

        addToEnvFromEnv("POOL_SIZE");

        addToEnvFromEnv("REST_HEDERA_MAINNET");
        addToEnvFromEnv("REST_HEDERA_TESTNET");
        addToEnvFromEnv("REST_HEDERA_PREVIEWNET");

        addToEnvFromEnv("REST_KABUTO_MAINNET");
        addToEnvFromEnv("REST_KABUTO_TESTNET");
        addToEnvFromEnv("REST_KABUTO_PREVIEWNET");

        addToEnvFromEnv("REST_DRAGONGLASS_MAINNET");
        addToEnvFromEnv("REST_DRAGONGLASS_TESTNET");
        addToEnvFromEnv("REST_DRAGONGLASS_PREVIEWNET");

        addToEnvFromEnv("GRPC_HEDERA_MAINNET");
        addToEnvFromEnv("GRPC_HEDERA_TESTNET");
        addToEnvFromEnv("GRPC_HEDERA_PREVIEWNET");

        addToEnvFromEnv("GRPC_KABUTO_MAINNET");
        addToEnvFromEnv("GRPC_KABUTO_TESTNET");
        addToEnvFromEnv("GRPC_KABUTO_PREVIEWNET");

        addToEnvFromEnv("GRPC_DRAGONGLASS_MAINNET");
        addToEnvFromEnv("GRPC_DRAGONGLASS_TESTNET");
        addToEnvFromEnv("GRPC_DRAGONGLASS_PREVIEWNET");

        environment.put("VUE_APP_TOPIC_ID", topicId);
        environment.put("MIRROR_PROVIDER", mirrorProvider);
        environment.put("DATABASE_URL", "postgresql://pgdb:5432/postgres");
        environment.put("DATABASE_USERNAME", postgreSQLContainer.getUsername());
        environment.put("DATABASE_PASSWORD", postgreSQLContainer.getPassword());
        environment.put("MIRROR_QUERY_FREQUENCY", "10000");

        GenericContainer container = new GenericContainer(
                DockerImageName.parse("hedera/hedera-nft-auction-demo-node:latest")
        )
        .withExposedPorts(8081, 8082)
        .withStartupTimeout(Duration.ofMinutes(1))
        .withNetwork(testContainersNetwork)
        .withEnv(environment)
        .withLogConsumer(new Slf4jLogConsumer(LOGGER).withSeparateOutputStreams());

        container.start();
        adminPort = container.getMappedPort(8082);
        return container;
    }
}
