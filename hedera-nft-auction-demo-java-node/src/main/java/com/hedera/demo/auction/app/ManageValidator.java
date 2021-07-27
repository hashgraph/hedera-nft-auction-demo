package com.hedera.demo.auction.app;

import com.hedera.hashgraph.sdk.Status;
import com.hedera.hashgraph.sdk.TopicId;
import com.hedera.hashgraph.sdk.TopicMessageSubmitTransaction;
import com.hedera.hashgraph.sdk.TransactionReceipt;
import com.hedera.hashgraph.sdk.TransactionResponse;
import io.vertx.core.json.JsonObject;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class ManageValidator extends AbstractCreate {

    public ManageValidator() throws Exception {
        super();
    }

    /**
     * Sends a JSON document containing validators' details as a HCS message
     * @param validators JSON object describing the actions to perform
     * @throws Exception in the event of an exception
     */
    public void manage(JsonObject validators) throws Exception {

        log.debug("posting validators request to topic id {}", topicId);
        TopicMessageSubmitTransaction topicMessageSubmitTransaction = new TopicMessageSubmitTransaction()
                .setTopicId(TopicId.fromString(topicId))
                .setTransactionMemo("Manage Validators")
                .setMessage(validators.encode());

        TransactionResponse transactionResponse = topicMessageSubmitTransaction.execute(hederaClient.client());
        TransactionReceipt receipt = transactionResponse.getReceipt(hederaClient.client());
        if (receipt.status != Status.SUCCESS) {
            log.error("validators request submission failed", receipt.status);
        } else {
            log.info("validators request submission successful");
        }
    }
}
