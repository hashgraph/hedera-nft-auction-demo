package com.hedera.demo.auction.app.api;

import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.ext.web.RoutingContext;
import lombok.extern.log4j.Log4j2;

/**
 * Checks the header-provided X-API-KEY is valid
 */
@Log4j2
public class AuthenticationHandler implements Handler<RoutingContext> {

    private final String apiKey;
    public AuthenticationHandler(String apiKey) {
        this.apiKey = apiKey;
    }

    /**
     * Given an X-API-KEY in the request header, verify it is valid
     *
     * @param routingContext the RoutingContext
     */
    @Override
    public void handle(RoutingContext routingContext) {

        MultiMap headers = routingContext.request().headers();
        if (! apiKey.equals(headers.get("x-api-key"))) {
            log.warn("invalid api key supplied");

            routingContext.fail(500, new Exception("invalid x-api-key"));
        } else {
            routingContext.next();
        }
        return;
    }
}
