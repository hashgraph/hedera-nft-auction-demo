package com.hedera.demo.auction.node.app.api;

import com.google.errorprone.annotations.Var;
import com.hedera.demo.auction.node.app.CreateToken;
import com.hedera.hashgraph.sdk.TokenId;
import io.github.cdimascio.dotenv.Dotenv;
import io.vertx.core.Handler;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

public class PostCreateToken implements Handler<RoutingContext> {
    private final Dotenv env;
    public PostCreateToken(Dotenv env) {
        this.env = env;
    }

    @Override
    public void handle(RoutingContext routingContext) {
        var body = routingContext.getBodyAsJson();

        @Var RequestCreateToken data = new RequestCreateToken();
        if (body != null) {
            data = body.mapTo(RequestCreateToken.class);
        }

        try {
            CreateToken createToken = new CreateToken();
            createToken.setEnv(env);
            TokenId tokenId = createToken.create(data.name, data.symbol, data.initialSupply, data.decimals, data.memo);
            JsonObject response = new JsonObject();
            response.put("tokenId", tokenId.toString());

            routingContext.response()
                    .putHeader("content-type", "application/json")
                    .end(Json.encodeToBuffer(response));
        } catch (Exception e) {
            routingContext.fail(400, e);
            return;
        }
    }
}
