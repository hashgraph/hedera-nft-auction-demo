package com.hedera.demo.auction.app;

import com.hedera.demo.auction.app.api.RequestCreateAuction;
import com.hedera.hashgraph.sdk.Status;
import com.hedera.hashgraph.sdk.TopicId;
import com.hedera.hashgraph.sdk.TopicMessageSubmitTransaction;
import com.hedera.hashgraph.sdk.TransactionId;
import com.hedera.hashgraph.sdk.TransactionReceipt;
import com.hedera.hashgraph.sdk.TransactionResponse;
import io.vertx.core.json.JsonObject;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class CreateAuction extends AbstractCreate {

    public CreateAuction() throws Exception {
        super();
    }

    /**
     * Sends a JSON file containing auction details as a HCS message
     * @param requestCreateAuction the object containing the auction's details
     * @throws Exception in the event of an exception
     */
    public void create(RequestCreateAuction requestCreateAuction) throws Exception {

        log.info("Submitting auction to HCS");

        TransactionId transactionId = TransactionId.generate(hederaClient.client().getOperatorAccountId());
        requestCreateAuction.createauctiontxid = transactionId.toString();

        JsonObject auctionInitData = JsonObject.mapFrom(requestCreateAuction);
        try {
            TopicMessageSubmitTransaction topicMessageSubmitTransaction = new TopicMessageSubmitTransaction()
                    .setTopicId(TopicId.fromString(requestCreateAuction.topicid))
                    .setTransactionMemo("CreateAuction")
                    .setTransactionId(transactionId)
                    .setMessage(auctionInitData.encode());

            TransactionResponse response = topicMessageSubmitTransaction.execute(hederaClient.client());
            TransactionReceipt receipt = response.getReceipt(hederaClient.client());
            if (receipt.status != Status.SUCCESS) {
                log.error("Topic submit failed {}", receipt.status);
            } else {
                log.info("Auction submitted");
            }
        } catch (Exception e) {
            log.error(e, e);
            throw e;
        }
    }
}
