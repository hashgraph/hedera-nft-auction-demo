package com.hedera.demo.auction.app.api;

import com.hedera.demo.auction.app.CreateTokenTransfer;
import io.github.cdimascio.dotenv.Dotenv;
import io.vertx.core.Handler;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

/**
 * Transfers a token from one account to another
 */
public class PostTransferHandler implements Handler<RoutingContext> {
    private final Dotenv env;
    public PostTransferHandler(Dotenv env) {
        this.env = env;
    }

    /**
     * Transfer the specified token from the operator specified in .env to the specified auction account id
     *
     * @param routingContext the RoutingContext
     */
    @Override
    public void handle(RoutingContext routingContext) {
        var body = routingContext.getBodyAsJson();

        if (body == null) {
            routingContext.fail(500);
            return;
        }

        var data = body.mapTo(RequestTokenTransfer.class);

        try {
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
