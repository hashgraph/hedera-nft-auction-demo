package com.hedera.demo.auction.node.app.api;

import io.github.cdimascio.dotenv.Dotenv;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CorsHandler;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.PoolOptions;
import lombok.extern.log4j.Log4j2;
import org.jooq.tools.StringUtils;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

@Log4j2
public class ApiVerticle extends AbstractVerticle {

    @Override
    public void start(Promise<Void> startPromise) throws Exception {

        Dotenv env = Dotenv
                .configure()
                .filename(config().getString("envFile"))
                .directory(config().getString("envPath"))
                .ignoreIfMissing()
                .load();

        String url = Optional.ofNullable(config().getString("DATABASE_URL")).orElse(Optional.ofNullable(env.get("DATABASE_URL")).orElse(""));
        String username = Optional.ofNullable(config().getString("DATABASE_USERNAME")).orElse(Optional.ofNullable(env.get("DATABASE_USERNAME")).orElse(""));
        String password = Optional.ofNullable(config().getString("DATABASE_PASSWORD")).orElse(Optional.ofNullable(env.get("DATABASE_PASSWORD")).orElse(""));
        int poolSize = Integer.parseInt(Optional.ofNullable(config().getString("POOL_SIZE")).orElse(Optional.ofNullable(env.get("POOL_SIZE")).orElse("1")));
        int httpPort = Integer.parseInt(Optional.ofNullable(config().getString("VUE_APP_API_PORT")).orElse(Optional.ofNullable(env.get("VUE_APP_API_PORT")).orElse("9005")));

        if (StringUtils.isEmpty(url)) {
            throw new Exception("missing environment variable DATABASE_URL");
        }
        if (StringUtils.isEmpty(username)) {
            throw new Exception("missing environment variable DATABASE_USERNAME");
        }
        if (StringUtils.isEmpty(password)) {
            throw new Exception("missing environment variable DATABASE_PASSWORD");
        }
        PgConnectOptions pgConnectOptions = PgConnectOptions
                        .fromUri(url)
                        .setUser(username)
                        .setPassword(password);
        PoolOptions poolOptions = new PoolOptions().setMaxSize(poolSize);

        PgPool pgPool = PgPool.pool(vertx, pgConnectOptions, poolOptions);

        var server = vertx.createHttpServer();
        var router = Router.router(vertx);

        GetAuctionsHandler getAuctionsHandler = new GetAuctionsHandler(pgPool);
        GetAuctionHandler getAuctionHandler = new GetAuctionHandler(pgPool);
        GetLastBidderBidHandler getLastBidderBidHandler = new GetLastBidderBidHandler(pgPool);
        GetBidsHandler getBidsHandler = new GetBidsHandler(pgPool);
        RootHandler rootHandler = new RootHandler();

        Set<HttpMethod> allowedMethods = new LinkedHashSet<>(Arrays.asList(HttpMethod.GET));
        Set<String> allowedHeaders = new LinkedHashSet<>(Arrays.asList("content-type"));
        router.route()
                .handler(BodyHandler.create())
                .handler(CorsHandler.create("*")
                        .allowedMethods(allowedMethods)
                        .allowedHeaders(allowedHeaders))
                .failureHandler(ApiVerticle::failureHandler);

        router.get("/v1/auctions/:id").handler(getAuctionHandler);
        router.get("/v1/auctions").handler(getAuctionsHandler);
        router.get("/v1/lastbid/:auctionid/:bidderaccountid").handler(getLastBidderBidHandler);
        router.get("/v1/bids/:auctionid").handler(getBidsHandler);
        router.get("/").handler(rootHandler);

        System.out.println("API Web Server Listening on port: " + httpPort);
        server
                .requestHandler(router)
                .listen(httpPort, result -> {
                    if (result.succeeded()) {
                        startPromise.complete();
                    } else {
                        startPromise.fail(result.cause());
                    }
                });
    }

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
        }

        response.end();
    }
}
