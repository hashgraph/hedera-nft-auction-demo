package com.hedera.demo.auction.node.app.api;

import com.google.errorprone.annotations.Var;
import com.hedera.demo.auction.node.app.CreateAuction;
import com.hedera.hashgraph.sdk.PrecheckStatusException;
import com.hedera.hashgraph.sdk.ReceiptStatusException;
import io.vertx.core.Handler;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import org.jooq.tools.StringUtils;

import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.TimeoutException;

import static java.nio.charset.StandardCharsets.UTF_8;

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
            @Var String fileName = data.auctionFile;

            if (StringUtils.isEmpty(fileName)) {
                fileName = "./sample-files/initDemo.json";

                JsonObject auction = new JsonObject();
                auction.put("tokenid", data.tokenid);
                auction.put("auctionaccountid", data.auctionaccountid);
                auction.put("reserve", data.reserve);
                auction.put("minimumbid", data.minimumbid);
                auction.put("endtimestamp", data.endtimestamp);
                auction.put("winnercanbid", data.winnercanbid);

                // store auction data in initDemo.json file
                FileWriter myWriter = new FileWriter(fileName, UTF_8);
                myWriter.write(auction.encodePrettily());
                myWriter.close();
            }

            CreateAuction.create(fileName, "");

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
