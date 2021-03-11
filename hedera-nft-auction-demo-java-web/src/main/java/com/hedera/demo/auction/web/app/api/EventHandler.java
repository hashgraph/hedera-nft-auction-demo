package com.hedera.demo.auction.web.app.api;

import io.vertx.core.Handler;
import io.vertx.core.http.ServerWebSocket;
import io.vertx.ext.web.RoutingContext;

import java.util.ArrayList;
import java.util.List;

public class EventHandler implements Handler<RoutingContext> {
    public static final List<ServerWebSocket> webSockets = new ArrayList<>();

    EventHandler() {
    }

    @Override
    public void handle(RoutingContext routingContext) {
        var webSocket = routingContext.request().upgrade();
        webSocket.accept();

        webSockets.add(webSocket);

        webSocket.closeHandler(v -> {
            // on close (by client), remove us from the list
            webSockets.remove(webSocket);
        });
    }
}
