package com.hedera.demo.auction.node.app;

import com.hedera.hashgraph.sdk.*;
import io.github.cdimascio.dotenv.Dotenv;
import lombok.extern.log4j.Log4j2;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.TimeoutException;

@Log4j2
public class CreateAuction {

    private final static Dotenv env = Dotenv.configure().ignoreIfMissing().load();
    private final static String topicId = Optional.ofNullable(env.get("TOPIC_ID")).orElse("");

    private CreateAuction() {
    }

    /**
     * Sends a JSON file containing auction details as a HCS message
     * @param auctionFile the path of the JSON file name containing the auction details
     * @throws TimeoutException in the event of an exception
     * @throws ReceiptStatusException in the event of an exception
     * @throws PrecheckStatusException in the event of an exception
     * @throws InterruptedException in the event of an exception
     * @throws IOException in the event of an exception
     */
    public static void create(String auctionFile) throws TimeoutException, PrecheckStatusException, ReceiptStatusException, IOException, InterruptedException {
        if (! Files.exists(Path.of(auctionFile))) {
            log.error("File " + auctionFile + " not found");
        } else {
            // submit message with auction file contents
            log.info("Loading " + auctionFile + " file");
            String auctionInitData = Files.readString(Path.of(auctionFile), StandardCharsets.US_ASCII);

            log.info("Submitting " + auctionFile + " file contents to HCS on topic " + topicId);
            Client client = HederaClient.getClient();
            TopicMessageSubmitTransaction topicMessageSubmitTransaction = new TopicMessageSubmitTransaction()
                    .setTopicId(TopicId.fromString(topicId))
                    .setTransactionMemo("CreateAuction")
                    .setMessage(auctionInitData);
            TransactionResponse response = topicMessageSubmitTransaction.execute(client);

            TransactionReceipt receipt = response.getReceipt(client);
            if (receipt.status != Status.SUCCESS) {
                log.error("Topic submit failed " + receipt.status);
            } else {
                log.info("Auction submitted");
            }
        }
    }

    public static void main(String[] args) throws IOException, PrecheckStatusException, ReceiptStatusException, TimeoutException, InterruptedException {
        if (args.length != 1) {
            log.error("Invalid number of arguments supplied");
        } else if (topicId.isEmpty()) {
            log.error("No TOPIC_ID in .env file");
        } else {
            log.info("Creating auction");
            create(args[0]);
        }
    }
}
