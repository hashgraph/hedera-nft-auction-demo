package com.hedera.demo.auction.node.app.api;

import com.google.errorprone.annotations.Var;
import com.hedera.demo.auction.node.app.CreateToken;
import com.hedera.hashgraph.sdk.TokenId;
import io.vertx.core.Handler;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

public class PostCreateToken implements Handler<RoutingContext> {
    public PostCreateToken() {
    }

    @Override
    public void handle(RoutingContext routingContext) {
        var body = routingContext.getBodyAsJson();

        @Var RequestCreateToken data = new RequestCreateToken();
        if (body != null) {
            data = body.mapTo(RequestCreateToken.class);
        }

        try {
            TokenId tokenId = CreateToken.create(data.name, data.symbol, data.initialSupply, data.decimals);
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
