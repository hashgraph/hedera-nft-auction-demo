package com.hedera.demo.auction.app.api;

import com.hedera.demo.auction.app.CreateTokenTransfer;
import com.hedera.demo.auction.app.Utils;
import io.github.cdimascio.dotenv.Dotenv;
import io.vertx.core.Handler;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.json.schema.Schema;
import io.vertx.json.schema.SchemaParser;
import lombok.extern.log4j.Log4j2;
import org.jooq.tools.StringUtils;

import static io.vertx.json.schema.common.dsl.Schemas.objectSchema;

/**
 * Transfers a token from one account to another
 */
@Log4j2
public class PostTransferHandler implements Handler<RoutingContext> {
    private final Dotenv env;
    private final SchemaParser schemaParser;
    public PostTransferHandler(SchemaParser schemaParser, Dotenv env) {
        this.env = env;
        this.schemaParser = schemaParser;
    }

    /**
     * Transfer the specified token from the operator specified in .env to the specified auction account id
     *
     * @param routingContext the RoutingContext
     */
    @Override
    public void handle(RoutingContext routingContext) {
        JsonObject body = routingContext.getBodyAsJson();

        Schema transferSchemaBuilder = objectSchema()
                .requiredProperty("tokenid", Utils.LONG_STRING_MAX_SCHEMA)
                .requiredProperty("auctionaccountid", Utils.LONG_STRING_MAX_SCHEMA)
                .build(schemaParser);

        transferSchemaBuilder.validateSync(body);

        RequestTokenTransfer requestTokenTransfer = body.mapTo(RequestTokenTransfer.class);

        String isValid = requestTokenTransfer.isValid();
        try {
            if (StringUtils.isEmpty(isValid)) {
                CreateTokenTransfer createTokenTransfer = new CreateTokenTransfer();
                createTokenTransfer.setEnv(env);
                createTokenTransfer.transfer(requestTokenTransfer);

                JsonObject response = new JsonObject();
                response.put("status", "transferred");

                routingContext.response()
                        .putHeader("content-type", "application/json")
                        .end(Json.encodeToBuffer(response));

            } else {
                throw new Exception(isValid);
            }
        } catch (Exception e) {
            log.error(e, e);
            routingContext.fail(500, e);
        }
    }
}
