package com.hedera.demo.auction.app;

import com.hedera.demo.auction.app.api.RequestPostValidator;
import com.hedera.hashgraph.sdk.Status;
import com.hedera.hashgraph.sdk.TopicId;
import com.hedera.hashgraph.sdk.TopicMessageSubmitTransaction;
import com.hedera.hashgraph.sdk.TransactionReceipt;
import com.hedera.hashgraph.sdk.TransactionResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class ManageValidator extends AbstractCreate {

    public ManageValidator() throws Exception {
        super();
    }

    /**
     * Sends a JSON document containing validator details as a HCS message
     * @param requestPostValidator object describing the action to perform
     * @throws Exception in the event of an exception
     */
    public void manage(RequestPostValidator requestPostValidator) throws Exception {

        JsonArray validators = new JsonArray();
        validators.add(JsonObject.mapFrom(requestPostValidator));
        JsonObject request = new JsonObject();
        request.put("validators", validators);

        log.debug("posting validator request to topic id {}", topicId);
        TopicMessageSubmitTransaction topicMessageSubmitTransaction = new TopicMessageSubmitTransaction()
                .setTopicId(TopicId.fromString(topicId))
                .setTransactionMemo("Manage Validators")
                .setMessage(request.encode());

        TransactionResponse transactionResponse = topicMessageSubmitTransaction.execute(hederaClient.client());
        TransactionReceipt receipt = transactionResponse.getReceipt(hederaClient.client());
        if (receipt.status != Status.SUCCESS) {
            log.error("validator request submission failed", receipt.status);
        } else {
            log.info("validator request submission successful");
        }
    }
}
