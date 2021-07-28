package com.hedera.demo.auction.app.api;

import com.hedera.demo.auction.app.CreateAuctionAccount;
import com.hedera.demo.auction.app.Utils;
import com.hedera.hashgraph.sdk.AccountId;
import com.hedera.hashgraph.sdk.PublicKey;
import io.github.cdimascio.dotenv.Dotenv;
import io.vertx.core.Handler;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.json.schema.Schema;
import io.vertx.json.schema.SchemaParser;
import io.vertx.json.schema.common.dsl.Schemas;
import lombok.extern.log4j.Log4j2;

import java.util.HashMap;
import java.util.Map;

import static io.vertx.json.schema.common.dsl.Schemas.objectSchema;
import static io.vertx.json.schema.draft7.dsl.Keywords.minimum;

/**
 * Creates a new auction account
 */
@Log4j2
public class PostAuctionAccountHandler implements Handler<RoutingContext> {
    private final Dotenv env;
    private final SchemaParser schemaParser;

    public PostAuctionAccountHandler(SchemaParser schemaParser, Dotenv env) {
        this.env = env;
        this.schemaParser = schemaParser;
    }

    /**
     * Given auction account details, create a new auction account
     *
     * @param routingContext the RoutingContext
     */
    @Override
    public void handle(RoutingContext routingContext) {
        var body = routingContext.getBodyAsJson();

        Schema auctionAccountSchemaBuilder = objectSchema()
                .requiredProperty("keylist", Schemas.objectSchema()
                        .requiredProperty("keys", Schemas.arraySchema()
                                .items(Schemas.objectSchema()
                                        .requiredProperty("key", Utils.KEY_STRING_MAX_SCHEMA)
                                )
                        )
                        .requiredProperty("threshold", Schemas.intSchema().with(minimum(1)))
                )
                .requiredProperty("initialBalance", Utils.LONG_NUMBER_SCHEMA)
                .build(schemaParser);

        auctionAccountSchemaBuilder.validateSync(body);

        try {
            JsonObject keys = new JsonObject();

            JsonObject keyList = body.getJsonObject("keylist");
            JsonArray allKeys = keyList.getJsonArray("keys");
            if (allKeys.size() == 0) {
                throw new Exception("no keys provided");
            }

            int threshold = keyList.getInteger("threshold", 0);
            if (threshold > allKeys.size()) {
                throw new Exception("threshold greater than number of keys.");
            }

            Map<String, Integer> keyMap = new HashMap();
            for (Object oneKeyObject : allKeys.getList()) {
                JsonObject keyJson = JsonObject.mapFrom(oneKeyObject);
                if (keyJson.containsKey("key")) {
                    String key = keyJson.getString("key");
                    try {
                        PublicKey.fromString(key);
                    } catch (@SuppressWarnings("UnusedException") Exception e) {
                        throw new Exception("invalid public key");
                    }
                    keyMap.put(keyJson.getString("key"), 0);
                }
            }

            if (allKeys.size() != keyMap.size()) {
                throw new Exception("duplicate keys detected.");
            }

            keys.put("keylist", body.getJsonObject("keylist"));
            RequestCreateAuctionAccount requestCreateAuctionAccount = body.mapTo(RequestCreateAuctionAccount.class);

            CreateAuctionAccount createAuctionAccount = new CreateAuctionAccount();
            createAuctionAccount.setEnv(env);
            AccountId auctionAccount = createAuctionAccount.create(requestCreateAuctionAccount);

            JsonObject response = new JsonObject();
            response.put("accountId", auctionAccount.toString());

            routingContext.response()
                    .putHeader("content-type", "application/json")
                    .end(Json.encodeToBuffer(response));
        } catch (Exception e) {
            log.error(e, e);
            routingContext.fail(500, e);
        }
    }
}
