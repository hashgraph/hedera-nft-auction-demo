package com.hedera.demo.auction.app.api;

import com.hedera.demo.auction.app.CreateToken;
import com.hedera.hashgraph.sdk.TokenId;
import io.github.cdimascio.dotenv.Dotenv;
import io.vertx.core.Handler;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

/**
 * Creates a new token
 */
public class PostCreateToken implements Handler<RoutingContext> {
    private final Dotenv env;
    public PostCreateToken(Dotenv env) {
        this.env = env;
    }

    /**
     * Given token details, create a new token
     *
     * @param routingContext the RoutingContext
     */
    @Override
    public void handle(RoutingContext routingContext) {
        var body = routingContext.getBodyAsJson();

        try {
            if (body != null) {
                CreateToken createToken = new CreateToken();
                createToken.setEnv(env);
                TokenId tokenId = createToken.create(body.encode());
                JsonObject response = new JsonObject();
                response.put("tokenId", tokenId.toString());

                routingContext.response()
                        .putHeader("content-type", "application/json")
                        .end(Json.encodeToBuffer(response));
            }
        } catch (Exception e) {
            routingContext.fail(400, e);
            return;
        }
    }
}
