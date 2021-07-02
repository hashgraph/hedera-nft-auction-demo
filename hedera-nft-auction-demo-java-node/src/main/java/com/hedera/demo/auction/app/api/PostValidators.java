package com.hedera.demo.auction.app.api;

import com.hedera.demo.auction.app.ManageValidator;
import io.vertx.core.Handler;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import lombok.extern.log4j.Log4j2;

/**
 * Adds, deletes or modifies a validator
 */
@Log4j2
public class PostValidators implements Handler<RoutingContext> {
    public PostValidators() {
    }

    /**
     * Given validator details, add, update or delete validator(s)
     *
     * @param routingContext the RoutingContext
     */
    @Override
    public void handle(RoutingContext routingContext) {
        var body = routingContext.getBodyAsJson();
        if (body == null) {
            log.error("empty message body");
            routingContext.fail(500);
            return;
        }

        if (body.containsKey("validators")) {
            JsonArray validators = body.getJsonArray("validators");
            if (validators != null) {
                for (Object validatorObject : validators) {
                    JsonObject validatorJson = JsonObject.mapFrom(validatorObject);
                    String[] args = new String[5];

                    args[0] = "--name=".concat(validatorJson.getString("name", ""));
                    args[1] = "--nameToUpdate=".concat(validatorJson.getString("nameToUpdate", ""));
                    args[2] = "--operation=".concat(validatorJson.getString("operation", ""));
                    args[3] = "--url=".concat(validatorJson.getString("url", ""));
                    args[4] = "--publicKey=".concat(validatorJson.getString("publicKey", ""));

                    try {
                        ManageValidator manageValidator = new ManageValidator();
                        manageValidator.manage(args);

                        JsonObject response = new JsonObject();
                        log.info("validator request submission successful");
                        response.put("status", "success");

                        routingContext.response()
                                .putHeader("content-type", "application/json")
                                .end(Json.encodeToBuffer(response));
                    } catch (Exception e) {
                        log.error(e, e);
                        routingContext.fail(500, e);
                        return;
                    }
                }
            } else {
                log.error("message body does not contain validators array");
                routingContext.fail(500);
                return;
            }
        } else {
            log.error("message body does not contain validators attribute");
            routingContext.fail(500);
            return;
        }
    }
}
