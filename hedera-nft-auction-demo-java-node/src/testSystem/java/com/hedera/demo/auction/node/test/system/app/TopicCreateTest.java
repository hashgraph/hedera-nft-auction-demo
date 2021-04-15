package com.hedera.demo.auction.node.test.system.app;

import com.google.errorprone.annotations.Var;
import com.hedera.demo.auction.node.app.CreateTopic;
import com.hedera.demo.auction.node.app.HederaClient;
import com.hedera.hashgraph.sdk.TopicId;
import com.hedera.hashgraph.sdk.TopicInfo;
import com.hedera.hashgraph.sdk.TopicInfoQuery;
import io.github.cdimascio.dotenv.Dotenv;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TopicCreateTest {
    Dotenv dotenv = Dotenv.configure().filename(".env.system").ignoreIfMissing().load();

    @Test
    public void testCreateTopic() throws Exception {
        File tempFile = File.createTempFile("test-", "");

        HederaClient hederaClient = new HederaClient(dotenv);
        CreateTopic createTopic = new CreateTopic();
        createTopic.setTargetDotEnvFile(tempFile.getAbsolutePath());

        createTopic.setEnv(dotenv);

        TopicId topicId = createTopic.create();
        // check topic Id exists on Hedera
        TopicInfo topicInfo = new TopicInfoQuery()
                .setTopicId(topicId)
                .execute(hederaClient.client());

        assertEquals(topicId.toString(), topicInfo.topicId.toString());
        assertEquals(hederaClient.operatorPublicKey().toString(), topicInfo.submitKey.toString());
        assertNull(topicInfo.adminKey);
        // check env file updated
        List<String> dotEnvLines = Files.readAllLines(tempFile.toPath());

        @Var boolean bFoundTopicId = false;
        for (String line : dotEnvLines) {
            if (line.trim().startsWith("VUE_APP_TOPIC_ID")) {
                if (line.contains(topicInfo.topicId.toString())) {
                    bFoundTopicId = true;
                }
            }
        }
        assertTrue(bFoundTopicId);
        Files.deleteIfExists(tempFile.toPath());
    }
}
