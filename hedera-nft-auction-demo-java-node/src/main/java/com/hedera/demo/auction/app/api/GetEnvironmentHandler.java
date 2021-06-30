package com.hedera.demo.auction.app.api;

import io.vertx.core.Handler;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

/**
 * Returns environment variables for use by the UI
 */
public class GetEnvironmentHandler implements Handler<RoutingContext>  {

    private final String network;
    private final String topicId;
    private final String nodeOperator;

    /**
     * Constructor
     * @param network the network to use
     * @param topicId the topic id to use
     * @param nodeOperator the name of the node's operator
     */
    GetEnvironmentHandler(String network, String topicId, String nodeOperator) {
        this.network = network;
        this.topicId = topicId;
        this.nodeOperator = nodeOperator;
    }

    /**
     * Creates a JSON response containing the environment data
     *
     * @param routingContext the RoutingContext
     */
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
