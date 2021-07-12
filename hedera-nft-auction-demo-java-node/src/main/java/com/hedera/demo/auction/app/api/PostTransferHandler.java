package com.hedera.demo.auction.app.api;

import com.hedera.demo.auction.app.CreateTokenTransfer;
import com.hedera.demo.auction.app.Utils;
import com.hedera.hashgraph.sdk.AccountId;
import com.hedera.hashgraph.sdk.TokenId;
import io.github.cdimascio.dotenv.Dotenv;
import io.vertx.core.Handler;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.json.schema.Schema;
import io.vertx.json.schema.SchemaParser;

import static io.vertx.json.schema.common.dsl.Schemas.objectSchema;

/**
 * Transfers a token from one account to another
 */
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
        var body = routingContext.getBodyAsJson();

        Schema transferSchemaBuilder = objectSchema()
                .requiredProperty("tokenid", Utils.LONG_STRING_MAX_SCHEMA)
                .requiredProperty("auctionaccountid", Utils.LONG_STRING_MAX_SCHEMA)
                .build(schemaParser);

        transferSchemaBuilder.validateSync(body);

        var data = body.mapTo(RequestTokenTransfer.class);

        try {
            try {
                TokenId.fromString(data.tokenid);
            } catch (@SuppressWarnings("UnusedException") Exception e) {
                throw new Exception("invalid format for tokenid, should be 0.0.1234");
            }

            try {
                AccountId.fromString(data.auctionaccountid);
            } catch (@SuppressWarnings("UnusedException") Exception e) {
                throw new Exception("invalid format for auctionaccountid, should be 0.0.1234");
            }

            CreateTokenTransfer createTokenTransfer = new CreateTokenTransfer();
            createTokenTransfer.setEnv(env);
            createTokenTransfer.transfer(data.tokenid, data.auctionaccountid);

            JsonObject response = new JsonObject();
            response.put("status", "transferred");

            routingContext.response()
                    .putHeader("content-type", "application/json")
                    .end(Json.encodeToBuffer(response));
        } catch (Exception e) {
            routingContext.fail(500, e);
            return;
        }
    }
}
