package com.hedera.demo.auction.node.app;

import com.google.errorprone.annotations.Var;
import com.hedera.hashgraph.sdk.*;
import lombok.extern.log4j.Log4j2;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeoutException;

@Log4j2
public class CreateTopic {

    private CreateTopic() {
    }

    /**
     * Creates a Topic Id with a submit key and adds it to the .env file
     * Note: This will also replace the existing value if it exists
     * @throws TimeoutException in the event of an exception
     * @throws PrecheckStatusException in the event of an exception
     * @throws ReceiptStatusException in the event of an exception
     * @throws InterruptedException in the event of an exception
     * @throws IOException in the event of an exception
     */

    public static void create() throws TimeoutException, PrecheckStatusException, IOException, ReceiptStatusException, InterruptedException {
        Client client = HederaClient.getClient();

        TopicCreateTransaction topicCreateTransaction = new TopicCreateTransaction()
                .setSubmitKey(HederaClient.getOperatorKey());
        TransactionResponse transactionResponse = topicCreateTransaction
                .execute(client);

        TransactionReceipt transactionReceipt = transactionResponse.getReceipt(client);

        TopicId topicId = Objects.requireNonNull(transactionReceipt.topicId);

        log.info("New topic created: " + topicId);

        String dotEnvFile = ".env";
        Path dotEnvPath = Paths.get(dotEnvFile);
        Path dotEnvTempPath = Paths.get(dotEnvFile.concat(".test"));
        PrintWriter printWriter = new PrintWriter(Files.newBufferedWriter(dotEnvTempPath, Charset.defaultCharset()));

        List<String> dotEnvLines = Files.readAllLines(dotEnvPath);

        @Var boolean bFoundTopicId = false;
        for (@Var String line : dotEnvLines) {
            if (line.trim().startsWith("TOPIC_ID")) {
                line = "TOPIC_ID=" + topicId;
                bFoundTopicId = true;
            } else if (line.trim().startsWith("#TOPIC_ID")) {
                line = "TOPIC_ID=" + topicId;
                bFoundTopicId = true;
            }
            printWriter.println(line);
        }
        if (! bFoundTopicId) {
            String line = "TOPIC_ID=" + topicId;
            printWriter.println(line);
        }
        printWriter.close();
        Files.copy(dotEnvTempPath, dotEnvPath, StandardCopyOption.REPLACE_EXISTING);
        Files.delete(dotEnvTempPath);
        log.info(".env file updated with new topic id " + topicId);
    }

    public static void main(String[] args) throws IOException, PrecheckStatusException, ReceiptStatusException, TimeoutException, InterruptedException {
        create();
    }
}


