package com.hedera.demo.auction.app.api;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

public class RootHandler implements Handler<RoutingContext>  {

    @Override
    public void handle(RoutingContext routingContext) {
        routingContext.response()
            .putHeader("content-type", "application/json")
            .end();
    }
}
