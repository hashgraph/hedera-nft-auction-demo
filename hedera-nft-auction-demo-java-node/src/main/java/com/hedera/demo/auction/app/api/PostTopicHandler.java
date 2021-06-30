package com.hedera.demo.auction.app.api;

import com.hedera.demo.auction.app.CreateTopic;
import com.hedera.hashgraph.sdk.PrecheckStatusException;
import com.hedera.hashgraph.sdk.ReceiptStatusException;
import com.hedera.hashgraph.sdk.TopicId;
import io.github.cdimascio.dotenv.Dotenv;
import io.vertx.core.Handler;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import lombok.extern.log4j.Log4j2;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

/**
 * Creates a new topic
 */
@Log4j2
public class PostTopicHandler implements Handler<RoutingContext> {

    private final Dotenv env;
    public PostTopicHandler(Dotenv env) {
        this.env = env;
    }

    /**
     * Create a new topic on Hedera
     *
     * @param routingContext the RoutingContext
     */
    @Override
    public void handle(RoutingContext routingContext) {

        try {
            CreateTopic createTopic = new CreateTopic();
            createTopic.setEnv(env);
            TopicId topicId = createTopic.create();
            JsonObject response = new JsonObject();
            response.put("topicId", topicId.toString());

            routingContext.response()
                    .putHeader("content-type", "application/json")
                    .end(Json.encodeToBuffer(response));
        } catch (InterruptedException e) {
            routingContext.fail(400, e);
            Thread.currentThread().interrupt();
        } catch (IOException | TimeoutException |PrecheckStatusException | ReceiptStatusException e) {
            routingContext.fail(400, e);
            log.error(e, e);
        } catch (Exception e) {
            routingContext.fail(400, e);
            log.error(e, e);
        }
    }
}
