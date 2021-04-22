package com.hedera.demo.auction.node.test.system.e2e;

import com.hedera.demo.auction.node.test.system.AbstractSystemTest;
import io.github.cdimascio.dotenv.DotenvEntry;
import io.vertx.core.Vertx;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.images.builder.ImageFromDockerfile;

import java.io.File;
import java.nio.file.Paths;
import java.time.Duration;

public abstract class AbstractE2ETest extends AbstractSystemTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractE2ETest.class);
    private final WebClientOptions webClientOptions = new WebClientOptions()
            .setUserAgent("HederaAuction/1.0")
            .setKeepAlive(false);
    protected WebClient webClient = WebClient.create(Vertx.vertx(), webClientOptions);

    protected AbstractE2ETest() throws Exception {
        super();
    }

    protected GenericContainer appContainer (PostgreSQLContainer postgreSQLContainer, String topicId, String mirrorProvider) {
        ClassLoader classLoader = getClass().getClassLoader();
        File dockerfile = new File(classLoader.getResource("Dockerfile").getFile());
        File jarFile = new File("build/libs/hedera-nft-auction-demo-node-1.0.jar").getAbsoluteFile();

//        GenericContainer container = new GenericContainer(
//                new ImageFromDockerfile()
//                        .withDockerfile(dockerfile.toPath())
//                        .withFileFromPath("hedera-nft-auction-demo-node-1.0.jar", jarFile.toPath())
//                )
//        .withExposedPorts(8081, 8082)
//        .withStartupTimeout(Duration.ofMinutes(1))
//        .withLogConsumer(new Slf4jLogConsumer(LOGGER).withSeparateOutputStreams());


        GenericContainer container = new GenericContainer(
                new ImageFromDockerfile()
                .withDockerfileFromBuilder(builder ->
                        builder
                .from("adoptopenjdk:14-jre-hotspot")
                        .copy("hedera-nft-auction-demo-node-1.0.jar", "/opt/hedera-nft-auction-demo-node-1.0.jar")
                        .cmd("java -jar /opt/hedera-nft-auction-demo-node-1.0.jar")
                        .build()
                ).withFileFromPath("hedera-nft-auction-demo-node-1.0.jar", Paths.get("build/libs/hedera-nft-auction-demo-node-1.0.jar"))
        )
        .withExposedPorts(8081, 8082)
        .withStartupTimeout(Duration.ofMinutes(2))
        .withLogConsumer(new Slf4jLogConsumer(LOGGER).withSeparateOutputStreams());

        for (DotenvEntry dotenvEntry : dotenv.entries()) {
            if (dotenvEntry.getKey().equals("VUE_APP_TOPIC_ID")) {
                container.withEnv("VUE_APP_TOPIC_ID", topicId);
            } else if (dotenvEntry.getKey().equals("MIRROR_PROVIDER")) {
                container.withEnv("MIRROR_PROVIDER", mirrorProvider);
            } else if (dotenvEntry.getKey().equals("DATABASE_URL")) {
                container.withEnv("DATABASE_URL", postgreSQLContainer.getJdbcUrl().replaceAll("jdbc:", ""));
            } else if (dotenvEntry.getKey().equals("DATABASE_USERNAME")) {
                container.withEnv("DATABASE_USERNAME", postgreSQLContainer.getUsername());
            } else if (dotenvEntry.getKey().equals("DATABASE_PASSWORD")) {
                container.withEnv("DATABASE_PASSWORD", postgreSQLContainer.getPassword());
            } else if (dotenvEntry.getKey().equals("MIRROR_QUERY_FREQUENCY")) {
                container.withEnv("MIRROR_QUERY_FREQUENCY", "1000");
            } else {
                container.withEnv(dotenvEntry.getKey(), dotenvEntry.getValue());
            }
        }

        container.start();
        return container;
    }
}
