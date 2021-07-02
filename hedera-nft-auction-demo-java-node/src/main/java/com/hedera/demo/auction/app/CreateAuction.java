package com.hedera.demo.auction.app;

import com.google.errorprone.annotations.Var;
import com.hedera.hashgraph.sdk.Status;
import com.hedera.hashgraph.sdk.TopicId;
import com.hedera.hashgraph.sdk.TopicMessageSubmitTransaction;
import com.hedera.hashgraph.sdk.TransactionReceipt;
import com.hedera.hashgraph.sdk.TransactionResponse;
import lombok.extern.log4j.Log4j2;
import org.jooq.tools.StringUtils;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

@Log4j2
public class CreateAuction extends AbstractCreate {

    public CreateAuction() throws Exception {
        super();
    }

    /**
     * Sends a JSON file containing auction details as a HCS message
     * @param auctionFile the path of the JSON file name containing the auction details
     * @throws Exception in the event of an exception
     */
    public void create(String auctionFile, String overrideTopicId) throws Exception {

        @Var String localTopicId = "";

        if (! overrideTopicId.isBlank()) {
            localTopicId = overrideTopicId;
        } else {
            localTopicId = topicId;
        }
        if (! Files.exists(Path.of(auctionFile))) {
            log.error("File {} not found", auctionFile);
        } else {
            // submit message with auction file contents
            log.info("Loading {} file", auctionFile);
            String auctionInitData = Files.readString(Path.of(auctionFile), StandardCharsets.US_ASCII);

            log.info("Submitting {} file contents to HCS on topic {}", auctionFile, localTopicId);

            try {
                TopicMessageSubmitTransaction topicMessageSubmitTransaction = new TopicMessageSubmitTransaction()
                        .setTopicId(TopicId.fromString(localTopicId))
                        .setTransactionMemo("CreateAuction")
                        .setMessage(auctionInitData);

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

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            log.error("Invalid number of arguments supplied");
        } else if (StringUtils.isEmpty(topicId)) {
            log.error("No TOPIC_ID in .env file");
        } else {
            log.info("Creating auction");
            CreateAuction createAuction = new CreateAuction();
            createAuction.create(args[0], "");
        }
    }
}
