package com.hedera.demo.auction.app.api;

import com.hedera.demo.auction.app.GenerateKey;
import com.hedera.hashgraph.sdk.PrivateKey;
import io.vertx.core.Handler;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

/**
 * Generates a new private/public key pair and returns it in a JSON response
 */
public class GetGeneratedKeysHandler implements Handler<RoutingContext> {

    public GetGeneratedKeysHandler() {
    }

    /**
     * Generates a private key and returns the key and its public key in a JSON payload
     *
     * @param routingContext the RoutingContext
     */
    @Override
    public void handle(RoutingContext routingContext) {
        GenerateKey generateKey = new GenerateKey();
        PrivateKey privateKey = generateKey.generate();

        JsonObject key = new JsonObject();
        key.put("PrivateKey", privateKey.toString());
        key.put("PublicKey", privateKey.getPublicKey().toString());

        routingContext.response()
                .putHeader("content-type", "application/json")
                .end(Json.encodeToBuffer(key));
    }
}
