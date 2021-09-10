package com.hedera.demo.auction.app.api;

import com.hedera.demo.auction.app.SqlConnectionManager;
import com.hedera.demo.auction.app.Utils;
import com.hedera.demo.auction.app.domain.Auction;
import com.hedera.demo.auction.app.repository.AuctionsRepository;
import com.hedera.demo.auction.app.repository.BidsRepository;
import com.hedera.demo.auction.app.repository.ValidatorsRepository;
import io.github.cdimascio.dotenv.Dotenv;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CorsHandler;
import io.vertx.ext.web.validation.ValidationHandler;
import io.vertx.ext.web.validation.builder.Parameters;
import io.vertx.json.schema.SchemaParser;
import io.vertx.json.schema.SchemaRouter;
import io.vertx.json.schema.SchemaRouterOptions;
import lombok.extern.log4j.Log4j2;
import org.jooq.tools.StringUtils;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

import static io.vertx.json.schema.common.dsl.Schemas.intSchema;
import static io.vertx.json.schema.draft7.dsl.Keywords.minimum;

/**
 * REST API verticle for the client API
 */
@Log4j2
public class ApiVerticle extends AbstractVerticle {

    /**
     * Starts the verticle and sets up the necessary handlers for each available endpoint
     * @param startPromise the Promise to callback when complete
     */
    @Override
    public void start(Promise<Void> startPromise) throws Exception {

        Dotenv env = Dotenv
                .configure()
                .filename(config().getString("envFile"))
                .directory(config().getString("envPath"))
                .ignoreIfMissing()
                .load();

        String url = Optional.ofNullable(config().getString("DATABASE_URL")).orElse(Optional.ofNullable(env.get("DATABASE_URL")).orElse(""));
        String database = Optional.ofNullable(config().getString("POSTGRES_DB")).orElse(Optional.ofNullable(env.get("POSTGRES_DB")).orElse(""));
        String username = Optional.ofNullable(config().getString("POSTGRES_USER")).orElse(Optional.ofNullable(env.get("POSTGRES_USER")).orElse(""));
        String password = Optional.ofNullable(config().getString("POSTGRES_PASSWORD")).orElse(Optional.ofNullable(env.get("POSTGRES_PASSWORD")).orElse(""));
        int httpPort = Integer.parseInt(Optional.ofNullable(config().getString("API_PORT")).orElse(Optional.ofNullable(env.get("API_PORT")).orElse("9005")));

        if (StringUtils.isEmpty(url)) {
            throw new Exception("missing environment variable DATABASE_URL");
        }
        if (StringUtils.isEmpty(database)) {
            throw new Exception("missing environment variable POSTGRES_DB");
        }
        if (StringUtils.isEmpty(username)) {
            throw new Exception("missing environment variable POSTGRES_USER");
        }
        if (StringUtils.isEmpty(password)) {
            throw new Exception("missing environment variable POSTGRES_PASSWORD");
        }

        SqlConnectionManager connectionManager = new SqlConnectionManager(url.concat(database), username, password);

        HttpServerOptions options = Utils.httpServerOptions(config());
        var server = vertx.createHttpServer(options);
        var router = Router.router(vertx);

        AuctionsRepository auctionsRepository = new AuctionsRepository(connectionManager);
        BidsRepository bidsRepository = new BidsRepository(connectionManager);
        ValidatorsRepository validatorsRepository = new ValidatorsRepository(connectionManager);

        GetAuctionsHandler getAuctionsHandler = new GetAuctionsHandler(auctionsRepository);
        GetAuctionHandler getAuctionHandler = new GetAuctionHandler(auctionsRepository);
        GetLastBidderBidHandler getLastBidderBidHandler = new GetLastBidderBidHandler(bidsRepository);
        GetBidsHandler getBidsHandler = new GetBidsHandler(bidsRepository, 50);
        GetAuctionsReserveNotMetHandler getAuctionsReserveNotMetHandler = new GetAuctionsReserveNotMetHandler(auctionsRepository);
        GetAuctionsForStatusHandler getClosedAuctionsHandler = new GetAuctionsForStatusHandler(auctionsRepository, Auction.CLOSED);
        GetAuctionsForStatusHandler getEndedAuctionsHandler = new GetAuctionsForStatusHandler(auctionsRepository, Auction.ENDED);
        GetAuctionsForStatusHandler getActiveAuctionsHandler = new GetAuctionsForStatusHandler(auctionsRepository, Auction.ACTIVE);
        GetAuctionsForStatusHandler getPendingAuctionsHandler = new GetAuctionsForStatusHandler(auctionsRepository, Auction.PENDING);
        GetAuctionsSoldHandler getAuctionsSoldHandler = new GetAuctionsSoldHandler(auctionsRepository);

        GetGeneratedKeysHandler getGeneratedKeysHandler = new GetGeneratedKeysHandler();

        GetEnvironmentHandler getEnvironmentHandler = new GetEnvironmentHandler(validatorsRepository, env.get("NETWORK"), config().getString("topicId"), env.get("NODE_OWNER", ""));
        RootHandler rootHandler = new RootHandler();

        Set<HttpMethod> allowedMethods = new LinkedHashSet<>(Arrays.asList(HttpMethod.GET));
        Set<String> allowedHeaders = new LinkedHashSet<>(Arrays.asList("content-type"));
        router.route()
                .handler(BodyHandler.create())
                .handler(CorsHandler.create("*")
                        .allowedMethods(allowedMethods)
                        .allowedHeaders(allowedHeaders))
                .failureHandler(ApiVerticle::failureHandler);

        SchemaRouter schemaRouter = SchemaRouter.create(vertx, new SchemaRouterOptions());
        SchemaParser schemaParser = SchemaParser.createOpenAPI3SchemaParser(schemaRouter);

        router.get("/v1/environment").handler(getEnvironmentHandler);
        router.get("/v1/reservenotmetauctions").handler(getAuctionsReserveNotMetHandler);
        router.get("/v1/closedauctions").handler(getClosedAuctionsHandler);
        router.get("/v1/endedauctions").handler(getEndedAuctionsHandler);
        router.get("/v1/activeauctions").handler(getActiveAuctionsHandler);
        router.get("/v1/pendingauctions").handler(getPendingAuctionsHandler);
        router.get("/v1/soldauctions").handler(getAuctionsSoldHandler);

        router.get("/v1/auctions/:id")
                .handler(ValidationHandler
                    .builder(schemaParser)
                    .pathParameter(Parameters.param("id", intSchema().with(minimum(1))))
                    .build())
                .handler(getAuctionHandler);

        router.get("/v1/auctions").handler(getAuctionsHandler);

        router.get("/v1/lastbid/:auctionid/:bidderaccountid")
                .handler(ValidationHandler
                        .builder(schemaParser)
                        .pathParameter(Parameters.param("auctionid", intSchema().with(minimum(1))))
                        .pathParameter(Parameters.param("bidderaccountid", Utils.SHORT_STRING_SCHEMA))
                        .build())
                .handler(getLastBidderBidHandler);

        router.get("/v1/bids/:auctionid")
                .handler(ValidationHandler
                        .builder(schemaParser)
                        .pathParameter(Parameters.param("auctionid", intSchema().with(minimum(1))))
                        .build())
                .handler(getBidsHandler);

        router.get("/v1/generatekey").handler(getGeneratedKeysHandler);
        router.get("/").handler(rootHandler);

        server
                .requestHandler(router)
                .exceptionHandler(error -> {
                    log.error(error, error);
                })
                .listen(httpPort, result -> {
                    if (result.succeeded()) {
                        log.info("API Web Server Listening on port: {}", httpPort);
                        startPromise.complete();
                    } else {
                        startPromise.fail(result.cause());
                    }
                })
;
    }

    /**
     * Generic failure handler for REST API calls
     *
     * @param routingContext the RoutingContext for which the failure occurred
     */
    private static void failureHandler(RoutingContext routingContext) {
        var response = routingContext.response();

        // if we got into the failure handler the status code
        // has likely been populated
        if (routingContext.statusCode() > 0) {
            response.setStatusCode(routingContext.statusCode());
        }

        var cause = routingContext.failure();
        if (cause != null) {
            log.error(cause);
            response.setStatusCode(500);
            response.setStatusMessage(cause.getMessage());
        }

        response.end();
    }
}
