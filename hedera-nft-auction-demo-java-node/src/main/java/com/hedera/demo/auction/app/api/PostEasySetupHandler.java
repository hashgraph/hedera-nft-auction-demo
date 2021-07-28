package com.hedera.demo.auction.app.api;

import com.hedera.demo.auction.app.EasySetup;
import com.hedera.demo.auction.app.Utils;
import io.vertx.core.Handler;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.json.schema.Schema;
import io.vertx.json.schema.SchemaParser;
import lombok.extern.log4j.Log4j2;

import static io.vertx.json.schema.common.dsl.Schemas.booleanSchema;
import static io.vertx.json.schema.common.dsl.Schemas.objectSchema;

/**
 * API to invoke the easySetup method
 */
@Log4j2
public class PostEasySetupHandler implements Handler<RoutingContext> {
    private final SchemaParser schemaParser;
    public PostEasySetupHandler(SchemaParser schemaParser) {
        this.schemaParser = schemaParser;
    }

    /**
     * Runs easy setup given the input parameters for the token's name, symbol and whether to clean up the database
     *
     * @param routingContext the RoutingContext
     */
    @Override
    public void handle(RoutingContext routingContext) {
        var body = routingContext.getBodyAsJson();

        Schema easySetupSchemaBuilder = objectSchema()
                .requiredProperty("symbol", Utils.HEDERA_STRING_MAX_SCHEMA)
                .requiredProperty("name", Utils.HEDERA_STRING_MAX_SCHEMA)
                .requiredProperty("clean", booleanSchema())
                .build(schemaParser);

        easySetupSchemaBuilder.validateSync(body);

        RequestEasySetup requestEasySetup = body.mapTo(RequestEasySetup.class);

        try {
            EasySetup easySetup = new EasySetup();
            easySetup.setup(requestEasySetup);

            JsonObject response = new JsonObject();
            response.put("status", "auction setup");

            routingContext.response()
                    .putHeader("content-type", "application/json")
                    .end(Json.encodeToBuffer(response));
        } catch (Exception e) {
            log.error(e, e);
            routingContext.fail(500, e);
        }
    }
}
