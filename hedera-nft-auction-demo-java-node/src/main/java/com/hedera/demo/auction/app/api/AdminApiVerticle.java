package com.hedera.demo.auction.app.api;

import com.hedera.demo.auction.app.Utils;
import io.github.cdimascio.dotenv.Dotenv;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CorsHandler;
import io.vertx.json.schema.SchemaParser;
import io.vertx.json.schema.SchemaRouter;
import io.vertx.json.schema.SchemaRouterOptions;
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
        String filesPath = config().getString("filesPath");
        String apiKey = config().getString("x-api-key");

        HttpServerOptions options = Utils.httpServerOptions(config());
        var server = vertx.createHttpServer(options);
        var router = Router.router(vertx);

        SchemaRouter schemaRouter = SchemaRouter.create(vertx, new SchemaRouterOptions());
        SchemaParser schemaParser = SchemaParser.createOpenAPI3SchemaParser(schemaRouter);

        AuthenticationHandler authenticationHandler = new AuthenticationHandler(apiKey);
        PostTopicHandler postTopicHandler = new PostTopicHandler(env);
        PostCreateToken postCreateToken = new PostCreateToken(schemaParser, env, filesPath);
        PostAuctionAccountHandler postAuctionAccountHandler = new PostAuctionAccountHandler(schemaParser, env);
        PostTransferHandler postTransferHandler = new PostTransferHandler(schemaParser, env);
        PostAuctionHandler postAuctionHandler = new PostAuctionHandler(schemaParser, env);
        PostEasySetupHandler postEasySetupHandler = new PostEasySetupHandler(schemaParser);
        PostValidators postValidators = new PostValidators(schemaParser);
        RootHandler rootHandler = new RootHandler();

        Set<HttpMethod> allowedMethods = new LinkedHashSet<>(Arrays.asList(HttpMethod.POST));
        Set<String> allowedHeaders = new LinkedHashSet<>(Arrays.asList("content-type"));

        router.route()
                .handler(BodyHandler.create())
                .handler(CorsHandler.create("*")
                        .allowedMethods(allowedMethods)
                        .allowedHeaders(allowedHeaders))
                .failureHandler(AdminApiVerticle::failureHandler);

        router.post("/v1/admin/topic")
            .handler(authenticationHandler)
            .handler(postTopicHandler);

        router.post("/v1/admin/easysetup")
                .handler(authenticationHandler)
                .handler(postEasySetupHandler);

        router.post("/v1/admin/token")
                .handler(authenticationHandler)
                .handler(postCreateToken);

        router.post("/v1/admin/auctionaccount")
                .handler(authenticationHandler)
                .handler(postAuctionAccountHandler);

        router.post("/v1/admin/auction")
                .handler(authenticationHandler)
                .handler(postAuctionHandler);

        router.post("/v1/admin/transfer")
                .handler(authenticationHandler)
                .handler(postTransferHandler);

        router.post("/v1/admin/validators")
                .handler(authenticationHandler)
                .handler(postValidators);

        router.get("/").handler(rootHandler);

        server
                .requestHandler(router)
                .exceptionHandler(error -> {
                    log.error(error, error);
                })
                .listen(httpPort, result -> {
                    if (result.succeeded()) {
                        log.info("Admin API Web Server Listening on port: {}", httpPort);
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
