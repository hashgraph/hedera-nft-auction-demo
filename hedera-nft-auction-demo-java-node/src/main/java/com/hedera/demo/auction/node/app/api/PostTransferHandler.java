package com.hedera.demo.auction.node.app.api;

import com.hedera.demo.auction.node.app.CreateTokenTransfer;
import io.vertx.core.Handler;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

public class PostTransferHandler implements Handler<RoutingContext> {
    public PostTransferHandler() {
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
            CreateTokenTransfer.transfer(data.tokenid, data.auctionaccountid);

            JsonObject response = new JsonObject();
            response.put("status", "transferred");

            routingContext.response()
                    .putHeader("content-type", "application/json")
                    .end(Json.encodeToBuffer(response));
        } catch (Exception e) {
            routingContext.fail(400, e);
            return;
        }
    }
}
