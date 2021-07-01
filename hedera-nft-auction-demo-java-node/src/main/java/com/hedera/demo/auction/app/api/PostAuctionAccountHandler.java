package com.hedera.demo.auction.app.api;

import com.hedera.demo.auction.app.CreateAuctionAccount;
import com.hedera.hashgraph.sdk.AccountId;
import io.github.cdimascio.dotenv.Dotenv;
import io.vertx.core.Handler;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

/**
 * Creates a new auction account
 */
public class PostAuctionAccountHandler implements Handler<RoutingContext> {
    private final Dotenv env;
    public PostAuctionAccountHandler(Dotenv env) {
        this.env = env;
    }

    /**
     * Given auction account details, create a new auction account
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

        try {
            JsonObject keys = new JsonObject();

            keys.put("keyList", body.getJsonObject("keyList"));
            CreateAuctionAccount createAuctionAccount = new CreateAuctionAccount();
            createAuctionAccount.setEnv(env);
            AccountId auctionAccount = createAuctionAccount.create(body.getLong("initialBalance"), keys.toString());

            JsonObject response = new JsonObject();
            response.put("accountId", auctionAccount.toString());

            routingContext.response()
                    .putHeader("content-type", "application/json")
                    .end(Json.encodeToBuffer(response));
        } catch (Exception e) {
            routingContext.fail(500, e);
            return;
        }
    }
}
