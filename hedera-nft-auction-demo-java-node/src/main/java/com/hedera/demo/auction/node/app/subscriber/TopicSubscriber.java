package com.hedera.demo.auction.node.app.subscriber;

import com.google.errorprone.annotations.Var;
import com.google.protobuf.ByteString;
import com.hedera.demo.auction.node.app.HederaClient;
import com.hedera.demo.auction.node.app.Utils;
import com.hedera.demo.auction.node.app.domain.Auction;
import com.hedera.demo.auction.node.auction.AuctionReadinessWatcher;
import com.hedera.demo.auction.node.app.repository.AuctionsRepository;
import com.hedera.demo.auction.node.app.repository.BidsRepository;
import com.hedera.hashgraph.sdk.AccountBalance;
import com.hedera.hashgraph.sdk.AccountBalanceQuery;
import com.hedera.hashgraph.sdk.AccountId;
import com.hedera.hashgraph.sdk.Client;
import com.hedera.hashgraph.sdk.FileContentsQuery;
import com.hedera.hashgraph.sdk.FileId;
import com.hedera.hashgraph.sdk.Hbar;
import com.hedera.hashgraph.sdk.PrecheckStatusException;
import com.hedera.hashgraph.sdk.PrivateKey;
import com.hedera.hashgraph.sdk.Status;
import com.hedera.hashgraph.sdk.SubscriptionHandle;
import com.hedera.hashgraph.sdk.TokenAssociateTransaction;
import com.hedera.hashgraph.sdk.TokenId;
import com.hedera.hashgraph.sdk.TokenInfo;
import com.hedera.hashgraph.sdk.TokenInfoQuery;
import com.hedera.hashgraph.sdk.TopicId;
import com.hedera.hashgraph.sdk.TopicMessageQuery;
import com.hedera.hashgraph.sdk.TransactionReceipt;
import com.hedera.hashgraph.sdk.TransactionResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import lombok.extern.log4j.Log4j2;
import org.jooq.tools.StringUtils;

import javax.annotation.Nullable;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Log4j2
public class TopicSubscriber implements Runnable{
    private final AuctionsRepository auctionsRepository;
    private final BidsRepository bidsRepository;
    private final TopicId topicId;
    private static Instant startTime = Instant.EPOCH;
    private final WebClient webClient;
    private final String refundKey;
    private final int mirrorQueryFrequency;
    private final HederaClient hederaClient;
    private boolean testing = false;
    private boolean skipReadinessWatcher = false;
    @Nullable
    private SubscriptionHandle subscriptionHandle = null;
    private boolean runThread = true;
    @Nullable
    private AuctionReadinessWatcher auctionReadinessWatcher = null;
    private final String masterKey;

    public TopicSubscriber(HederaClient hederaClient, AuctionsRepository auctionsRepository, BidsRepository bidsRepository, WebClient webClient, TopicId topicId, String refundKey, int mirrorQueryFrequency, String masterKey) {
        this.auctionsRepository = auctionsRepository;
        this.bidsRepository = bidsRepository;
        this.topicId = topicId;
        this.webClient = webClient;
        this.refundKey = refundKey;
        this.mirrorQueryFrequency = mirrorQueryFrequency;
        this.hederaClient = hederaClient;
        this.masterKey = masterKey;
    }

    public void stop() {
        if (subscriptionHandle != null) {
            subscriptionHandle.unsubscribe();
        }
        runThread = false;
        if (auctionReadinessWatcher != null) {
            auctionReadinessWatcher.stop();
        }
    }

    public void setTesting() {
        testing = true;
    }

    public void setSkipReadinessWatcher() {
        skipReadinessWatcher = true;
    }

    @Override
    public void run() {
        try {
            startSubscription();
        } catch (RuntimeException e) {
            log.error(e);
        }
        while (runThread) {
            Utils.sleep(5000);
        }
    }

    private void startSubscription() {
        try {
            Client client = hederaClient.client();
            log.debug("subscribing to topic id " + topicId + " from " + startTime);
            subscriptionHandle = new TopicMessageQuery()
                    .setTopicId(topicId)
                    .setStartTime(startTime)
                    .subscribe(client, topicMessage -> {
                        startTime = topicMessage.consensusTimestamp;
                        TopicMessageWrapper topicMessageWrapper = new TopicMessageWrapper(topicMessage);
                        handle(topicMessageWrapper);
                    });
        } catch (RuntimeException e) {
            log.error("Mirror subscription error " + e.getMessage());
            log.info("Attempting re-subscription after 5s");
            Utils.sleep(5000);
            startSubscription();
        }
    }

    public void handle(TopicMessageWrapper topicMessageWrapper) {
        try {
            log.debug("Got HCS Message");
            String auctionData = new String(topicMessageWrapper.contents, StandardCharsets.UTF_8);
            JsonObject auctionJson = new JsonObject(auctionData);
            Auction newAuction = new Auction().fromJson(auctionJson);
            Instant consensusTime = topicMessageWrapper.consensusTimestamp;
            @Var String endTimeStamp = newAuction.getEndtimestamp().toLowerCase();

            if (endTimeStamp.contains("m")) {
                int timeDelta = Integer.parseInt(endTimeStamp.replace("m", ""));
                endTimeStamp = String.valueOf(consensusTime.plus(timeDelta, ChronoUnit.MINUTES).getEpochSecond());
            } else if (endTimeStamp.contains("h")) {
                int timeDelta = Integer.parseInt(endTimeStamp.replace("h", ""));
                endTimeStamp = String.valueOf(consensusTime.plus(timeDelta, ChronoUnit.HOURS).getEpochSecond());
            } else if (endTimeStamp.contains("d")) {
                int timeDelta = Integer.parseInt(endTimeStamp.replace("d", ""));
                endTimeStamp = String.valueOf(consensusTime.plus(timeDelta, ChronoUnit.DAYS).getEpochSecond());
            } else if (StringUtils.isEmpty(endTimeStamp)) {
                // no end timestamp, use consensus timestamp + 2 days
                endTimeStamp = String.valueOf(consensusTime.plus(2, ChronoUnit.DAYS).getEpochSecond());
            }

            newAuction.setEndtimestamp(endTimeStamp.concat(".000000000")); // add nanoseconds
            newAuction.setWinningbid(0L);

            @Var Auction auction;
            try {
                log.debug("Adding auction to database");
                auction = auctionsRepository.getAuction(newAuction.getAuctionaccountid());
                // auction doesn't exist, create it
                if (auction == null) {
                    if (!testing) {
                        // get token info
                        try {
                            log.debug("Getting token info");
                            Client client = hederaClient.client();
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
                        } catch (Exception e) {
                            log.error(e);
                            throw e;
                        }
                    }

                    auction = auctionsRepository.add(newAuction);

                    if ((auction.getId() != 0) && !testing) {
                        log.info("Auction for token " + newAuction.getTokenid() + " added");
                    }

                    if (!skipReadinessWatcher && (webClient != null)) {
                        // Start a thread to watch this new auction for readiness
                        auctionReadinessWatcher = new AuctionReadinessWatcher(hederaClient, webClient, auctionsRepository, bidsRepository, auction, refundKey, mirrorQueryFrequency);
                        Thread t = new Thread(auctionReadinessWatcher);
                        t.start();
                    }

                }

                // do we need to associate
                // If refund key and master node, associate with the token
                //TODO: Currently only available to the master node, this feature should eventually
                // transition to use scheduled transactions
                if (!StringUtils.isEmpty(refundKey) && !StringUtils.isEmpty(masterKey)) {

                    Client client = hederaClient.auctionClient(auction, PrivateKey.fromString(masterKey));

                    AccountBalance accountBalance = new AccountBalanceQuery()
                            .setAccountId(AccountId.fromString(auction.getAuctionaccountid()))
                            .execute(client);

                    Map<TokenId, Long> tokens = accountBalance.token;
                    if ( ! tokens.containsKey(TokenId.fromString(auction.getTokenid()))) {
                        // not associated yet, try association
                        client.setMaxTransactionFee(Hbar.from(100));
                        TokenAssociateTransaction tokenAssociateTransaction = new TokenAssociateTransaction();
                        List<TokenId> tokenIds = new ArrayList<>();
                        tokenIds.add(TokenId.fromString(auction.getTokenid()));
                        tokenAssociateTransaction.setTokenIds(tokenIds);
                        tokenAssociateTransaction.setTransactionMemo("Associate");
                        tokenAssociateTransaction.setAccountId(AccountId.fromString(auction.getAuctionaccountid()));

                        try {
                            TransactionResponse response = tokenAssociateTransaction.execute(client);

                            TransactionReceipt receipt = response.getReceipt(client);
                            if (receipt.status != Status.SUCCESS) {
                                log.error("Token association failed " + receipt.status);
                            } else {
                                log.info("Scheduled transaction to associate token " + auction.getTokenid() + " with auction account " + auction.getAuctionaccountid());
                            }
                        } catch (PrecheckStatusException e) {
                            if (e.status != Status.TOKEN_ALREADY_ASSOCIATED_TO_ACCOUNT) {
                                log.error("Error during association " + e.getMessage());
                            }
                        }
                    }
                }

            } catch (SQLException e) {
                log.error("unable to determine if auction already exists");
                log.error(e);
            }
        } catch (Exception e) {
            log.error(e);
        }
    }
}
