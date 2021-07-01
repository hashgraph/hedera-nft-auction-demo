package com.hedera.demo.auction.app.api;

import com.google.errorprone.annotations.Var;
import com.hedera.demo.auction.app.HederaClient;
import com.hedera.hashgraph.sdk.Status;
import com.hedera.hashgraph.sdk.TopicId;
import com.hedera.hashgraph.sdk.TopicMessageSubmitTransaction;
import com.hedera.hashgraph.sdk.TransactionReceipt;
import com.hedera.hashgraph.sdk.TransactionResponse;
import io.github.cdimascio.dotenv.Dotenv;
import io.vertx.core.Handler;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import lombok.extern.log4j.Log4j2;
import org.jooq.tools.StringUtils;

/**
 * Creates a new token
 */
@Log4j2
public class PostValidators implements Handler<RoutingContext> {
    private final Dotenv env;
    public PostValidators(Dotenv env) {
        this.env = env;
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
        String topicId = env.get("TOPIC_ID");
        if (StringUtils.isEmpty(topicId)) {
            log.error("topicId is unknown");
            routingContext.fail(500);
            return;
        }

        @Var HederaClient hederaClient = null;
        try {
            hederaClient = new HederaClient(env);

            TopicMessageSubmitTransaction topicMessageSubmitTransaction = new TopicMessageSubmitTransaction()
                    .setTopicId(TopicId.fromString(topicId))
                    .setTransactionMemo("Manage Validators")
                    .setMessage(body.encode());

            TransactionResponse transactionResponse = topicMessageSubmitTransaction.execute(hederaClient.client());
            TransactionReceipt receipt = transactionResponse.getReceipt(hederaClient.client());
            JsonObject response = new JsonObject();
            if (receipt.status != Status.SUCCESS) {
                log.error("validator request submission failed", receipt.status);
                response.put("status", receipt.status);
            } else {
                log.info("validator request submission successful");
                response.put("status", "success");
            }

            routingContext.response()
                    .putHeader("content-type", "application/json")
                    .end(Json.encodeToBuffer(response));
        } catch (Exception e) {
            log.error(e, e);
            routingContext.fail(500);
            return;
        }
    }
}
