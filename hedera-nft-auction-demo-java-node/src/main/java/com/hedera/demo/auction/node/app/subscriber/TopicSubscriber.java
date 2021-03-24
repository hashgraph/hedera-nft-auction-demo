package com.hedera.demo.auction.node.app.subscriber;

import com.google.errorprone.annotations.Var;
import com.google.protobuf.ByteString;
import com.hedera.demo.auction.node.app.HederaClient;
import com.hedera.demo.auction.node.app.readinesswatchers.AuctionReadinessWatcher;
import com.hedera.demo.auction.node.app.domain.Auction;
import com.hedera.demo.auction.node.app.repository.AuctionsRepository;
import com.hedera.demo.auction.node.app.repository.BidsRepository;
import com.hedera.hashgraph.sdk.Client;
import com.hedera.hashgraph.sdk.FileContentsQuery;
import com.hedera.hashgraph.sdk.FileId;
import com.hedera.hashgraph.sdk.TokenId;
import com.hedera.hashgraph.sdk.TokenInfo;
import com.hedera.hashgraph.sdk.TokenInfoQuery;
import com.hedera.hashgraph.sdk.TopicId;
import com.hedera.hashgraph.sdk.TopicMessage;
import com.hedera.hashgraph.sdk.TopicMessageQuery;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import lombok.extern.log4j.Log4j2;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Log4j2
public class TopicSubscriber implements Runnable{
    private final AuctionsRepository auctionsRepository;
    private final BidsRepository bidsRepository;
    private final TopicId topicId;
    private static Instant startTime = Instant.EPOCH;
    private final WebClient webClient;
    private final String refundKey;
    private final int mirrorQueryFrequency;

    public TopicSubscriber(AuctionsRepository auctionsRepository, BidsRepository bidsRepository, WebClient webClient, TopicId topicId, String refundKey, int mirrorQueryFrequency) {
        this.auctionsRepository = auctionsRepository;
        this.bidsRepository = bidsRepository;
        this.topicId = topicId;
        this.webClient = webClient;
        this.refundKey = refundKey;
        this.mirrorQueryFrequency = mirrorQueryFrequency;
    }

    @Override
    public void run() {
        try {
            startSubscription();
        } catch (RuntimeException e) {
            e.printStackTrace();
            log.error(e);
        }
        while (true) {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
                log.error(e);
            }
        }
    }

    private void startSubscription() {
        try {
            Client client = HederaClient.getClient();
            log.info("Subscribing to topic " + topicId.toString());
            new TopicMessageQuery()
                    .setTopicId(topicId)
                    .setStartTime(startTime)
                    .subscribe(client, topicMessage -> {
                        startTime = topicMessage.consensusTimestamp;
                        handle(topicMessage);
                    });
        } catch (Exception e) {
            log.error("Mirror subscription error " + e.getMessage());
            log.info("Attempting re-subscription after 5s");
            try {
                Thread.sleep(5000);
            } catch (InterruptedException interruptedException) {
                interruptedException.printStackTrace();
                log.error(interruptedException);
            }
            startSubscription();
        }
    }

    private void handle(TopicMessage topicMessage) {
        try {
            String auctionData = new String(topicMessage.contents, StandardCharsets.UTF_8);
            JsonObject auctionJson = new JsonObject(auctionData);
            Auction newAuction = new Auction().fromJson(auctionJson);
            @Var String endTimeStamp = newAuction.getEndtimestamp();
            if (endTimeStamp.isEmpty()) {
                // no end timestamp, use consensus timestamp + 2 days
                Instant consensusTime = topicMessage.consensusTimestamp;
                endTimeStamp = String.valueOf(consensusTime.plus(2, ChronoUnit.DAYS).getEpochSecond());
            }
            newAuction.setEndtimestamp(endTimeStamp.concat(".000000000")); // add nanoseconds
            newAuction.setWinningbid(0L);

             Client client = HederaClient.getClient();
            // get token info
            TokenInfo tokenInfo = new TokenInfoQuery()
                    .setTokenId(TokenId.fromString(newAuction.getTokenid()))
                    .execute(client);

            // if token symbol starts with HEDERA://
            // load file from hedera
            if (tokenInfo.symbol.startsWith("HEDERA://")) {
                String fileId = tokenInfo.symbol.replace("HEDERA://", "");
                ByteString contentsQuery = new FileContentsQuery()
                        .setFileId(FileId.fromString(fileId))
                        .execute(client);
                String contents = contentsQuery.toString(StandardCharsets.UTF_8);
                // set token image data
                newAuction.setTokenimage(contents);
            }

            Auction auction = auctionsRepository.add(newAuction);
            if (auction.getId() != null) {
                log.info("Auction for token " + newAuction.getTokenid() + " added");
                // Start a thread to watch this new auction for readiness
                Thread t = new Thread(new AuctionReadinessWatcher(webClient, auctionsRepository, bidsRepository, newAuction, refundKey, mirrorQueryFrequency));
                t.start();
            }
        } catch (Exception e) {
            log.error(e);
        }
    }
}
