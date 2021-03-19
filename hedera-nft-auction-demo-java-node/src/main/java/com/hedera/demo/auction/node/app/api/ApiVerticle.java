package com.hedera.demo.auction.node.app.api;

import com.hedera.demo.auction.node.app.PGPool;
import io.github.cdimascio.dotenv.Dotenv;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.CorsHandler;
import io.vertx.pgclient.PgPool;
import lombok.extern.log4j.Log4j2;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

@Log4j2
public class ApiVerticle extends AbstractVerticle {

    private final static Dotenv env = Dotenv.configure().ignoreIfMissing().load();
    private final static PgPool pgPool = new PGPool(env).createPgPool();;
    private final static int httpPort = Optional.ofNullable(env.get("API_PORT")).map(Integer::parseInt).orElse(9005);

    @Override
    public void start(Promise<Void> startPromise) {

        var server = vertx.createHttpServer();
        var router = Router.router(vertx);

        GetAuctionsHandler getAuctionsHandler = new GetAuctionsHandler(pgPool);
        GetAuctionHandler getAuctionHandler = new GetAuctionHandler(pgPool);
        GetLastBidderBidHandler getLastBidderBidHandler = new GetLastBidderBidHandler(pgPool);
        GetBidsHandler getBidsHandler = new GetBidsHandler(pgPool);

        PostTopicHandler postTopicHandler = new PostTopicHandler();
        PostCreateToken postCreateToken = new PostCreateToken();
        PostAuctionAccountHandler postAuctionAccountHandler = new PostAuctionAccountHandler();
        PostAssociationHandler postAssociationHandler = new PostAssociationHandler();
        PostTransferHandler postTransferHandler = new PostTransferHandler();
        PostAuctionHandler postAuctionHandler = new PostAuctionHandler();
        PostEasySetupHandler postEasySetupHandler = new PostEasySetupHandler();

        Set<HttpMethod> allowedMethods = new LinkedHashSet<>(Arrays.asList(HttpMethod.GET, HttpMethod.POST));
        Set<String> allowedHeaders = new LinkedHashSet<>(Arrays.asList("content-type"));
        router.route().handler(CorsHandler.create("*").allowedMethods(allowedMethods).allowedHeaders(allowedHeaders)).failureHandler(this::failureHandler);

        router.get("/v1/auctions/:id").handler(getAuctionHandler);
        router.get("/v1/auctions").handler(getAuctionsHandler);
        router.get("/v1/lastbid/:auctionid/:bidderaccountid").handler(getLastBidderBidHandler);
        router.get("/v1/bids/:auctionid").handler(getBidsHandler);

        router.post("/v1/topic").handler(postTopicHandler);
        router.post("/v1/token").handler(postCreateToken);
        router.post("/v1/auctionaccount").handler(postAuctionAccountHandler);
        router.post("/v1/associate").handler(postAssociationHandler);
        router.post("/v1/transfer").handler(postTransferHandler);
        router.post("/v1/auction").handler(postAuctionHandler);
        router.post("/v1/easysetup").handler(postEasySetupHandler);

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
