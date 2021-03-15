package com.hedera.demo.auction.web.app;

import com.hedera.demo.auction.web.app.api.AuctionHandler;
import com.hedera.demo.auction.web.app.api.AuctionsHandler;
import com.hedera.demo.auction.web.app.api.BidsHandler;
import com.hedera.demo.auction.web.app.api.LastBidderBidHandler;
import io.github.cdimascio.dotenv.Dotenv;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.CorsHandler;
import io.vertx.pgclient.PgPool;
import lombok.extern.log4j.Log4j2;

import java.util.*;

@Log4j2
public class App extends AbstractVerticle {
    private final static Dotenv env = Dotenv.configure().ignoreIfMissing().load();
    final int httpPort = Optional.ofNullable(env.get("API_PORT")).map(Integer::parseInt).orElse(9005);

    final PgPool pgPool = new PGPool(env).createPgPool();

    public static void main(String[] args) {
        Vertx.vertx().deployVerticle(
                App.class.getName(),
                new DeploymentOptions().setInstances(16));
    }

    private static String requireEnv(String name) {
        return Objects.requireNonNull(env.get(name), "missing environment variable " + name);
    }
    @Override
    public void start(Promise<Void> startPromise) {
        var server = vertx.createHttpServer();
        var router = Router.router(vertx);

        AuctionsHandler auctionsHandler = new AuctionsHandler(pgPool);
        AuctionHandler auctionHandler = new AuctionHandler(pgPool);
        LastBidderBidHandler lastBidderBidHandler = new LastBidderBidHandler(pgPool);
        BidsHandler bidsHandler = new BidsHandler(pgPool);

        Set<HttpMethod> allowedMethods = new LinkedHashSet<>(Arrays.asList(HttpMethod.GET));
        Set<String> allowedHeaders = new LinkedHashSet<>(Arrays.asList("content-type"));
        router.route().handler(CorsHandler.create("*").allowedMethods(allowedMethods).allowedHeaders(allowedHeaders)).failureHandler(this::failureHandler);

        router.get("/v1/auctions/:id").handler(auctionHandler);
        router.get("/v1/auctions").handler(auctionsHandler);
        router.get("/v1/lastbid/:auctionid/:bidderaccountid").handler(lastBidderBidHandler);
        router.get("/v1/bids/:auctionid").handler(bidsHandler);

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

    private void failureHandler(RoutingContext routingContext) {
        var response = routingContext.response();

        // if we got into the failure handler the status code
        // has likely been populated
        if (routingContext.statusCode() > 0) {
            response.setStatusCode(routingContext.statusCode());
        }

        var cause = routingContext.failure();
        if (cause != null) {
            log.error(cause);
            cause.printStackTrace();
            response.setStatusCode(500);
        }

        response.end();
    }
}