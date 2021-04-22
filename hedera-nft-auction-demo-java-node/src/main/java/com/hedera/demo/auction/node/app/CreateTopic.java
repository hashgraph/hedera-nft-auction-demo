package com.hedera.demo.auction.node.app;

import com.google.errorprone.annotations.Var;
import com.hedera.hashgraph.sdk.Client;
import com.hedera.hashgraph.sdk.PrecheckStatusException;
import com.hedera.hashgraph.sdk.ReceiptStatusException;
import com.hedera.hashgraph.sdk.TopicCreateTransaction;
import com.hedera.hashgraph.sdk.TopicId;
import com.hedera.hashgraph.sdk.TransactionReceipt;
import com.hedera.hashgraph.sdk.TransactionResponse;
import lombok.extern.log4j.Log4j2;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeoutException;

@Log4j2
public class CreateTopic extends AbstractCreate {

    private String dotEnvFile = ".env";

    public CreateTopic() throws Exception {
        super();
    }

    public void setTargetDotEnvFile(String dotEnvFile) {
        this.dotEnvFile = dotEnvFile;
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

    public TopicId create() throws Exception {

        Client client = hederaClient.client();

        TopicCreateTransaction topicCreateTransaction = new TopicCreateTransaction()
                .setSubmitKey(hederaClient.operatorPublicKey());
        TransactionResponse transactionResponse = topicCreateTransaction
                .execute(client);

        TransactionReceipt transactionReceipt = transactionResponse.getReceipt(client);

        TopicId topicId = Objects.requireNonNull(transactionReceipt.topicId);

        log.info("New topic created: " + topicId);

        Path dotEnvPath = Paths.get(dotEnvFile);
        if (Files.exists(dotEnvPath)) {
            Path dotEnvTempPath = Paths.get(dotEnvFile.concat(".test"));
            PrintWriter printWriter = new PrintWriter(Files.newBufferedWriter(dotEnvTempPath, StandardCharsets.UTF_8));

            List<String> dotEnvLines = Files.readAllLines(dotEnvPath);

            @Var boolean bFoundTopicId = false;
            for (@Var String line : dotEnvLines) {
                if (line.trim().startsWith("VUE_APP_TOPIC_ID")) {
                    line = "VUE_APP_TOPIC_ID=" + topicId;
                    bFoundTopicId = true;
                } else if (line.trim().startsWith("#VUE_APP_TOPIC_ID")) {
                    line = "VUE_APP_TOPIC_ID=" + topicId;
                    bFoundTopicId = true;
                }
                printWriter.println(line);
            }
            if (!bFoundTopicId) {
                String line = "VUE_APP_TOPIC_ID=" + topicId;
                printWriter.println(line);
            }
            printWriter.close();
            Files.copy(dotEnvTempPath, dotEnvPath, StandardCopyOption.REPLACE_EXISTING);
            Files.delete(dotEnvTempPath);
            log.info(".env file updated with new topic id " + topicId);
        } else {
            log.warn("no .env file to update with topic id");
        }

        return topicId;
    }

    public static void main(String[] args) throws Exception {
        CreateTopic createTopic = new CreateTopic();
        createTopic.create();
    }
}


