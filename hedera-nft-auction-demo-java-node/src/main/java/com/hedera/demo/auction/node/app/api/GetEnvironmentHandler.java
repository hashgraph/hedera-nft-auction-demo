package com.hedera.demo.auction.node.app.api;

import io.vertx.core.Handler;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

public class GetEnvironmentHandler implements Handler<RoutingContext>  {

    private final String network;
    private final String topicId;
    private final String nodeOperator;

    GetEnvironmentHandler(String network, String topicId, String nodeOperator) {
        this.network = network;
        this.topicId = topicId;
        this.nodeOperator = nodeOperator;
    }

    @Override
    public void handle(RoutingContext routingContext) {

        JsonObject response = new JsonObject();
        response.put("network", this.network);
        response.put("topicId", this.topicId);
        response.put("nodeOperator", this.nodeOperator);

        routingContext.response()
                .putHeader("content-type", "application/json")
                .end(Json.encodeToBuffer(response));
    }
}
