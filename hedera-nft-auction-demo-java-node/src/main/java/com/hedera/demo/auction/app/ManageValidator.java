package com.hedera.demo.auction.app;

import com.hedera.hashgraph.sdk.Status;
import com.hedera.hashgraph.sdk.TopicId;
import com.hedera.hashgraph.sdk.TopicMessageSubmitTransaction;
import com.hedera.hashgraph.sdk.TransactionReceipt;
import com.hedera.hashgraph.sdk.TransactionResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import lombok.extern.log4j.Log4j2;
import org.jooq.tools.StringUtils;

import java.util.List;

@Log4j2
public class ManageValidator extends AbstractCreate {

    public ManageValidator() throws Exception {
        super();
    }

    /**
     * Sends a JSON document containing validator details as a HCS message
     * @param args array of arguments
     * @throws Exception in the event of an exception
     */
    public void manage(String[] args) throws Exception {

        List<String> options = List.of("name", "nameToUpdate", "operation", "url", "publicKey");

        JsonObject validatorRequest = new JsonObject();
        for (String arg : args) {
            for (String option : options) {
                String searchOption = "--".concat(option).concat("=");
                if (arg.startsWith(searchOption)) {
                    validatorRequest.put(option, arg.replace(searchOption, ""));
                }
            }
        }

        JsonArray validators = new JsonArray();
        validators.add(validatorRequest);
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

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            log.error("Invalid number of arguments supplied");
        } else if (StringUtils.isEmpty(topicId)) {
            log.error("No TOPIC_ID in .env file");
        } else {
            log.info("Managing validator");
            ManageValidator manageValidator = new ManageValidator();
            manageValidator.manage(args);
        }
    }
}
