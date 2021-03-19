package com.hedera.demo.auction.node.app.api;

import com.hedera.demo.auction.node.app.CreateAuction;
import com.hedera.hashgraph.sdk.PrecheckStatusException;
import com.hedera.hashgraph.sdk.ReceiptStatusException;
import io.vertx.core.Handler;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class PostAuctionHandler implements Handler<RoutingContext> {
    public PostAuctionHandler() {
    }

    @Override
    public void handle(RoutingContext routingContext) {
        var body = routingContext.getBodyAsJson();

        if (body == null) {
            routingContext.fail(400);
            return;
        }

        var data = body.mapTo(RequestCreateAuction.class);

        try {
            CreateAuction.create(data.auctionFile, "");

            JsonObject response = new JsonObject();
            response.put("status", "created");

            routingContext.response()
                    .putHeader("content-type", "application/json")
                    .end(Json.encodeToBuffer(response));
        } catch (InterruptedException | TimeoutException | PrecheckStatusException | ReceiptStatusException | IOException e) {
            routingContext.fail(400, e);
            return;
        } catch (Exception e) {
            e.printStackTrace();
            routingContext.fail(400, e);
        }
    }
}
