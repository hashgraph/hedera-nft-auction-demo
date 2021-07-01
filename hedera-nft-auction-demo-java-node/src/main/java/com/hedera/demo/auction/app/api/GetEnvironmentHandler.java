package com.hedera.demo.auction.app.api;

import com.hedera.demo.auction.app.domain.Validator;
import io.vertx.core.Handler;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.pgclient.PgPool;

/**
 * Returns environment variables for use by the UI
 */
public class GetEnvironmentHandler implements Handler<RoutingContext>  {

    private final PgPool pgPool;
    private final String network;
    private final String topicId;
    private final String nodeOperator;

    /**
     * Constructor
     * @param pgPool the database connection pool to use
     * @param network the network to use
     * @param topicId the topic id to use
     * @param nodeOperator the name of the node's operator
     */
    GetEnvironmentHandler(PgPool pgPool, String network, String topicId, String nodeOperator) {
        this.pgPool = pgPool;
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

        String sql = "SELECT * FROM validators ORDER BY name";

        pgPool.preparedQuery(sql).execute(ar -> {
            if (ar.failed()) {
                routingContext.fail(ar.cause());
                return;
            }

            var rows = ar.result();
            var validators = new JsonArray();

            for (var row : rows) {
                var item = new Validator(row);
                validators.add(item);
            }

            response.put("validators", validators);

            routingContext.response()
                    .putHeader("content-type", "application/json")
                    .end(Json.encodeToBuffer(response));

        });
    }
}
