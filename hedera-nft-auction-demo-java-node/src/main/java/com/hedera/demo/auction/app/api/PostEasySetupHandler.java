package com.hedera.demo.auction.app.api;

import com.google.errorprone.annotations.Var;
import com.hedera.demo.auction.app.EasySetup;
import io.vertx.core.Handler;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

/**
 * API to invoke the easySetup method
 */
public class PostEasySetupHandler implements Handler<RoutingContext> {
    public PostEasySetupHandler() {
    }

    /**
     * Runs easy setup given the input parameters for the token's name, symbol and whether to clean up the database
     *
     * @param routingContext the RoutingContext
     */
    @Override
    public void handle(RoutingContext routingContext) {
        var body = routingContext.getBodyAsJson();
        @Var RequestEasySetup data = new RequestEasySetup();

        if (body != null) {
            data = body.mapTo(RequestEasySetup.class);
        }

        try {
            String[] args = new String[3];

            args[0] = "--symbol=".concat(data.symbol);
            args[1] = "--name=".concat(data.name);
            args[2] = "";
            if (! data.clean) {
                args[2] = "--no-clean";
            }

            EasySetup easySetup = new EasySetup();
            easySetup.setup(args);

            JsonObject response = new JsonObject();
            response.put("status", "auction setup");

            routingContext.response()
                    .putHeader("content-type", "application/json")
                    .end(Json.encodeToBuffer(response));
        } catch (Exception e) {
            routingContext.fail(500, e);
            return;
        }
    }
}
