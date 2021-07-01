package com.hedera.demo.auction.app.api;

import com.google.errorprone.annotations.Var;
import com.hedera.demo.auction.app.CreateAuction;
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

/**
 * Creates a new auction
 */
@Log4j2
public class PostAuctionHandler implements Handler<RoutingContext> {
    private final Dotenv env;
    public PostAuctionHandler(Dotenv env) {
        this.env = env;
    }

    /**
     * Given auction details, create a new auction
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
                auction.put("title", data.title);
                auction.put("description", data.description);

                // store auction data in initDemo.json file
                FileWriter myWriter = new FileWriter(fileName, UTF_8);
                myWriter.write(auction.encodePrettily());
                myWriter.close();
            }

            CreateAuction createAuction = new CreateAuction();
            createAuction.setEnv(env);
            @Var String localTopicId = env.get("TOPIC_ID");
            if (! StringUtils.isEmpty(data.topicId)) {
                localTopicId = data.topicId;
            }
            createAuction.create(fileName, localTopicId);

            JsonObject response = new JsonObject();
            response.put("status", "created");

            routingContext.response()
                    .putHeader("content-type", "application/json")
                    .end(Json.encodeToBuffer(response));
        } catch (InterruptedException e) {
            routingContext.fail(500, e);
            Thread.currentThread().interrupt();
        } catch (TimeoutException | PrecheckStatusException | ReceiptStatusException | IOException e) {
            routingContext.fail(500, e);
            return;
        } catch (Exception e) {
            log.error(e, e);
            routingContext.fail(500, e);
        }
    }
}
