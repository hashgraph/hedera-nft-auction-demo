package com.hedera.demo.auction.node.app.api;

import com.hedera.demo.auction.node.app.CreateTokenAssociation;
import io.vertx.core.Handler;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

public class PostAssociationHandler implements Handler<RoutingContext> {
    public PostAssociationHandler() {
    }

    @Override
    public void handle(RoutingContext routingContext) {
        var body = routingContext.getBodyAsJson();

        if (body == null) {
            routingContext.fail(400);
            return;
        }

        var data = body.mapTo(RequestTokenAccount.class);

        try {
            CreateTokenAssociation.associate(data.tokenid, data.auctionaccountid);

            JsonObject response = new JsonObject();
            response.put("status", "associated");

            routingContext.response()
                    .putHeader("content-type", "application/json")
                    .end(Json.encodeToBuffer(response));
        } catch (Exception e) {
            routingContext.fail(400, e);
            return;
        }
    }
}
