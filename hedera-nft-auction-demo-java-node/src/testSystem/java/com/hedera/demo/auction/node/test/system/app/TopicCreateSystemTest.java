package com.hedera.demo.auction.node.test.system.app;

import com.google.errorprone.annotations.Var;
import com.hedera.demo.auction.node.test.system.AbstractSystemTest;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TopicCreateSystemTest extends AbstractSystemTest {

    TopicCreateSystemTest() throws Exception {
        super();
    }

    @Test
    public void testCreateTopic() throws Exception {
        File tempFile = File.createTempFile("test-", "");

        createTopic.setTargetDotEnvFile(tempFile.getAbsolutePath());

        createTopicAndGetInfo();

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
