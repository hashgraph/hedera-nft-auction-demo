package com.hedera.demo.auction.app.api;

import com.hedera.demo.auction.app.CreateAuction;
import com.hedera.demo.auction.app.Utils;
import io.github.cdimascio.dotenv.Dotenv;
import io.vertx.core.Handler;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.json.schema.Schema;
import io.vertx.json.schema.SchemaParser;
import io.vertx.json.schema.common.dsl.Schemas;
import lombok.extern.log4j.Log4j2;
import org.jooq.tools.StringUtils;

import static io.vertx.json.schema.common.dsl.Schemas.objectSchema;

/**
 * Creates a new auction
 */
@Log4j2
public class PostAuctionHandler implements Handler<RoutingContext> {
    private final Dotenv env;
    private final SchemaParser schemaParser;
    public PostAuctionHandler(SchemaParser schemaParser, Dotenv env) {
        this.env = env;
        this.schemaParser = schemaParser;
    }

    /**
     * Given auction details, create a new auction
     *
     * @param routingContext the RoutingContext
     */
    @Override
    public void handle(RoutingContext routingContext) {
        var body = routingContext.getBodyAsJson();

        Schema auctionSchemaBuilder = objectSchema()
                .requiredProperty("tokenid", Utils.LONG_STRING_MAX_SCHEMA)
                .requiredProperty("auctionaccountid", Utils.LONG_STRING_MAX_SCHEMA)
                .requiredProperty("reserve", Utils.LONG_NUMBER_SCHEMA)
                .requiredProperty("minimumbid", Utils.LONG_NUMBER_SCHEMA)
                .optionalProperty("endtimestamp", Utils.SHORT_STRING_SCHEMA)
                .requiredProperty("winnercanbid", Schemas.booleanSchema())
                .requiredProperty("title", Utils.LONG_STRING_MAX_SCHEMA)
                .requiredProperty("description", Utils.LONG_STRING_MAX_SCHEMA)
                .optionalProperty("topicid", Utils.SHORT_STRING_SCHEMA)
                .build(schemaParser);

        auctionSchemaBuilder.validateSync(body);

        RequestCreateAuction requestCreateAuction = body.mapTo(RequestCreateAuction.class);
        String isValid = requestCreateAuction.isValid();

        try {
            if (StringUtils.isEmpty(isValid)) {
                CreateAuction createAuction = new CreateAuction();
                createAuction.setEnv(env);
                if (StringUtils.isEmpty(requestCreateAuction.topicid)) {
                    requestCreateAuction.topicid = env.get("TOPIC_ID");
                    if (requestCreateAuction.topicid == null) {
                        throw new Exception("TOPIC_ID environment variable not set");
                    }
                }
                createAuction.create(requestCreateAuction);

                JsonObject response = new JsonObject();
                response.put("status", "created");

                routingContext.response()
                        .putHeader("content-type", "application/json")
                        .end(Json.encodeToBuffer(response));

            } else {
                throw new Exception(isValid);
            }
        } catch (InterruptedException e) {
            log.error(e, e);
            routingContext.fail(500, e);
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.error(e, e);
            routingContext.fail(500, e);
        }
    }
}
