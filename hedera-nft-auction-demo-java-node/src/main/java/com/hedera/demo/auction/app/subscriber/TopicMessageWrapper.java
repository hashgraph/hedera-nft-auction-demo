package com.hedera.demo.auction.app.subscriber;

import com.hedera.hashgraph.sdk.TopicMessage;
import com.hedera.hashgraph.sdk.TransactionId;

import javax.annotation.Nullable;
import java.time.Instant;

public class TopicMessageWrapper {
    public final Instant consensusTimestamp;
    public final byte[] contents;
    public final byte[] runningHash;
    public final long sequenceNumber;
    @Nullable
    public final TransactionId transactionId;

    TopicMessageWrapper(TopicMessage topicMessage) {
        this.consensusTimestamp = topicMessage.consensusTimestamp;
        this.contents = topicMessage.contents;
        this.runningHash = topicMessage.runningHash;
        this.sequenceNumber = topicMessage.sequenceNumber;
        this.transactionId = topicMessage.transactionId;
    }
    public TopicMessageWrapper(Instant consensusTimestamp, byte[] contents, byte[] runningHash, long sequenceNumber, @Nullable TransactionId transactionId) {
        this.consensusTimestamp = consensusTimestamp;
        this.contents = contents;
        this.runningHash = runningHash;
        this.sequenceNumber = sequenceNumber;
        this.transactionId = transactionId;
    }

}
