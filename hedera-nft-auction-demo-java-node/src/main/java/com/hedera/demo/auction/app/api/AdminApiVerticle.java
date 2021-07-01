package com.hedera.demo.auction.app.api;

import io.github.cdimascio.dotenv.Dotenv;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CorsHandler;
import lombok.extern.log4j.Log4j2;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

/**
 * REST API verticle for the admin API
 */
@Log4j2
public class AdminApiVerticle extends AbstractVerticle {

    /**
     * Starts the verticle and sets up the necessary handlers for each available endpoint
     * @param startPromise the Promise to callback when complete
     */
    @Override
    public void start(Promise<Void> startPromise) {

        Dotenv env = Dotenv
                .configure()
                .filename(config().getString("envFile"))
                .directory(config().getString("envPath"))
                .ignoreIfMissing()
                .load();

        int httpPort = Integer.parseInt(Optional.ofNullable(config().getString("ADMIN_API_PORT")).orElse(Optional.ofNullable(env.get("ADMIN_API_PORT")).orElse("9006")));

        var server = vertx.createHttpServer();
        var router = Router.router(vertx);

        PostTopicHandler postTopicHandler = new PostTopicHandler(env);
        PostCreateToken postCreateToken = new PostCreateToken(env);
        PostAuctionAccountHandler postAuctionAccountHandler = new PostAuctionAccountHandler(env);
        PostTransferHandler postTransferHandler = new PostTransferHandler(env);
        PostAuctionHandler postAuctionHandler = new PostAuctionHandler(env);
        PostEasySetupHandler postEasySetupHandler = new PostEasySetupHandler();
        PostValidators postValidators = new PostValidators(env);
        RootHandler rootHandler = new RootHandler();

        Set<HttpMethod> allowedMethods = new LinkedHashSet<>(Arrays.asList(HttpMethod.POST));
        Set<String> allowedHeaders = new LinkedHashSet<>(Arrays.asList("content-type"));
        router.route()
                .handler(BodyHandler.create())
                .handler(CorsHandler.create("*")
                        .allowedMethods(allowedMethods)
                        .allowedHeaders(allowedHeaders))
                .failureHandler(AdminApiVerticle::failureHandler);

        router.post("/v1/admin/topic").handler(postTopicHandler);
        router.post("/v1/admin/token").handler(postCreateToken);
        router.post("/v1/admin/auctionaccount").handler(postAuctionAccountHandler);
        router.post("/v1/admin/transfer").handler(postTransferHandler);
        router.post("/v1/admin/auction").handler(postAuctionHandler);
        router.post("/v1/admin/easysetup").handler(postEasySetupHandler);
        router.post("/v1/admin/validators").handler(postValidators);
        router.get("/").handler(rootHandler);

        log.info("Admin API Web Server Listening on port: {}", httpPort);
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
