package com.hedera.demo.auction.app.api;

import com.hedera.demo.auction.app.CreateToken;
import com.hedera.demo.auction.app.Utils;
import com.hedera.hashgraph.sdk.TokenId;
import io.github.cdimascio.dotenv.Dotenv;
import io.vertx.core.Handler;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.json.schema.Schema;
import io.vertx.json.schema.SchemaParser;
import io.vertx.json.schema.common.dsl.Schemas;

import static io.vertx.json.schema.common.dsl.Schemas.objectSchema;
import static io.vertx.json.schema.draft7.dsl.Keywords.minimum;

/**
 * Creates a new token
 */
public class PostCreateToken implements Handler<RoutingContext> {
    private final Dotenv env;
    private final String filesPath;
    private final SchemaParser schemaParser;

    public PostCreateToken(SchemaParser schemaParser, Dotenv env) {
        this.env = env;
        this.filesPath = Utils.filesPath(env);
        this.schemaParser = schemaParser;
    }

    /**
     * Given token details, create a new token
     *
     * @param routingContext the RoutingContext
     */
    @Override
    public void handle(RoutingContext routingContext) {
        var body = routingContext.getBodyAsJson();

        Schema tokenSchemaBuilder = objectSchema()
                .requiredProperty("name", Utils.HEDERA_STRING_MAX_SCHEMA.defaultValue("Token"))
                .requiredProperty("symbol", Utils.HEDERA_STRING_MAX_SCHEMA.defaultValue("TT"))
                .optionalProperty("initialSupply", Utils.LONG_NUMBER_SCHEMA.defaultValue(1))
                .optionalProperty("decimals", Schemas.intSchema().with(minimum(0)).defaultValue(0))
                .optionalProperty("memo", Utils.HEDERA_STRING_MAX_SCHEMA)
                .optionalProperty("description", Schemas.objectSchema()
                        .requiredProperty("type", Utils.SHORT_STRING_SCHEMA)
                        .requiredProperty("description", Utils.LONG_STRING_MAX_SCHEMA)
                )
                .optionalProperty("image", Schemas.objectSchema()
                        .requiredProperty("type", Utils.SHORT_STRING_SCHEMA)
                        .requiredProperty("description", Utils.LONG_STRING_MAX_SCHEMA)
                )
                .optionalProperty("certificate", Schemas.objectSchema()
                        .requiredProperty("type", Utils.SHORT_STRING_SCHEMA)
                        .requiredProperty("description", Utils.LONG_STRING_MAX_SCHEMA)
                ).build(schemaParser);

        tokenSchemaBuilder.validateSync(body);

        try {
            CreateToken createToken = new CreateToken();
            createToken.setEnv(env);

            RequestCreateToken tokenData = body.mapTo(RequestCreateToken.class);
            tokenData.checkIsValid(filesPath);

            TokenId tokenId = createToken.create(body.encode());
            JsonObject response = new JsonObject();
            response.put("tokenId", tokenId.toString());

            routingContext.response()
                    .putHeader("content-type", "application/json")
                    .end(Json.encodeToBuffer(response));
        } catch (Exception e) {
            routingContext.fail(500, e);
            return;
        }
    }
}
