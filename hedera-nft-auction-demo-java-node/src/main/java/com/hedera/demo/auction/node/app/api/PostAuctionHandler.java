package com.hedera.demo.auction.node.app.api;

import com.google.errorprone.annotations.Var;
import com.hedera.demo.auction.node.app.CreateAuction;
import com.hedera.hashgraph.sdk.PrecheckStatusException;
import com.hedera.hashgraph.sdk.ReceiptStatusException;
import io.github.cdimascio.dotenv.Dotenv;
import io.vertx.core.Handler;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import lombok.extern.log4j.Log4j2;
import org.jooq.tools.StringUtils;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeoutException;

import static java.nio.charset.StandardCharsets.UTF_8;

@Log4j2
public class PostAuctionHandler implements Handler<RoutingContext> {
    private final Dotenv env;
    public PostAuctionHandler(Dotenv env) {
        this.env = env;
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

                if (!Files.exists(Path.of(fileName))) {
                    try {
                        Files.createDirectory(Path.of("./sample-files"));
                    } catch (FileAlreadyExistsException e) {
                        log.info("./sample-files already exists.");
                    }
                    Files.createFile(Path.of(fileName));
                }

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

            CreateAuction createAuction = new CreateAuction();
            createAuction.setEnv(env);
            @Var String localTopicId = env.get("VUE_APP_TOPIC_ID");
            if (! StringUtils.isEmpty(data.topicId)) {
                localTopicId = data.topicId;
            }
            createAuction.create(fileName, localTopicId);

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
