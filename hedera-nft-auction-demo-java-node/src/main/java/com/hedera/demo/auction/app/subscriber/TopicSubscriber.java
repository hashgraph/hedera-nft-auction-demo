package com.hedera.demo.auction.app.subscriber;

import com.google.errorprone.annotations.Var;
import com.hedera.demo.auction.AuctionReadinessWatcher;
import com.hedera.demo.auction.app.HederaClient;
import com.hedera.demo.auction.app.Utils;
import com.hedera.demo.auction.app.domain.Auction;
import com.hedera.demo.auction.app.mirrormapping.MirrorTopicMessage;
import com.hedera.demo.auction.app.mirrormapping.MirrorTopicMessages;
import com.hedera.demo.auction.app.repository.AuctionsRepository;
import com.hedera.hashgraph.sdk.AccountBalance;
import com.hedera.hashgraph.sdk.AccountBalanceQuery;
import com.hedera.hashgraph.sdk.AccountId;
import com.hedera.hashgraph.sdk.AccountInfo;
import com.hedera.hashgraph.sdk.AccountInfoQuery;
import com.hedera.hashgraph.sdk.Client;
import com.hedera.hashgraph.sdk.Hbar;
import com.hedera.hashgraph.sdk.PrecheckStatusException;
import com.hedera.hashgraph.sdk.PrivateKey;
import com.hedera.hashgraph.sdk.Status;
import com.hedera.hashgraph.sdk.TokenAssociateTransaction;
import com.hedera.hashgraph.sdk.TokenId;
import com.hedera.hashgraph.sdk.TokenInfo;
import com.hedera.hashgraph.sdk.TokenInfoQuery;
import com.hedera.hashgraph.sdk.TopicId;
import com.hedera.hashgraph.sdk.TransactionReceipt;
import com.hedera.hashgraph.sdk.TransactionResponse;
import io.vertx.core.json.JsonObject;
import lombok.extern.log4j.Log4j2;
import org.jooq.tools.StringUtils;

import javax.annotation.Nullable;
import java.sql.SQLException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Log4j2
public class TopicSubscriber implements Runnable{
    private final AuctionsRepository auctionsRepository;
    @Nullable
    private final TopicId topicId;
    private final int mirrorQueryFrequency;
    private final HederaClient hederaClient;
    private boolean testing = false;
    private boolean skipReadinessWatcher = false;
    private boolean runThread = true;
    @Nullable
    private AuctionReadinessWatcher auctionReadinessWatcher = null;
    private final String masterKey;
    private boolean runOnce = false;

    public TopicSubscriber(HederaClient hederaClient, AuctionsRepository auctionsRepository, TopicId topicId, int mirrorQueryFrequency, String masterKey) {
        this.auctionsRepository = auctionsRepository;
        this.topicId = topicId;
        this.mirrorQueryFrequency = mirrorQueryFrequency;
        this.hederaClient = hederaClient;
        this.masterKey = masterKey;
    }

    public TopicSubscriber(HederaClient hederaClient, AuctionsRepository auctionsRepository, TopicId topicId, int mirrorQueryFrequency, String masterKey, boolean runOnce) {
        this(hederaClient, auctionsRepository, topicId, mirrorQueryFrequency, masterKey);
        this.runOnce = runOnce;
    }

    public void stop() {
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
        if (this.topicId == null) {
            log.warn("topicId is null, exiting");
            return;
        }
        @Var String nextTimestamp = "0.0";
        String uri = "/api/v1/topics/".concat(this.topicId.toString()).concat("/messages");
        ExecutorService executor = Executors.newFixedThreadPool(1);
        while (this.runThread) {
            try {

                log.debug("Checking for messages on topic id {} {}", this.topicId.toString());

                Map<String, String> queryParameters = new HashMap<>();
                queryParameters.put("timestamp", "gt:".concat(nextTimestamp));

                Future<JsonObject> future = executor.submit(Utils.queryMirror(hederaClient, uri, queryParameters));

                try {
                    JsonObject response = future.get();
                    if (response != null) {
                        MirrorTopicMessages mirrorTopicMessages = response.mapTo(MirrorTopicMessages.class);
                        int messagesCount = mirrorTopicMessages.messages.size();
                        if (messagesCount > 0) {
                            nextTimestamp = mirrorTopicMessages.messages.get(messagesCount - 1).consensusTimestamp;
                        }
                        handle(mirrorTopicMessages);

                        if (messagesCount == 0 && runOnce) {
                            // no new messages, running once, exit
                            this.runThread = false;
                        }
                    }
                } catch (InterruptedException e) {
                    log.error(e, e);
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    log.error(e, e);
                }
            } catch (RuntimeException e) {
                log.error(e, e);
            }

            if (this.runThread) {
                // only sleep if necessary
                Utils.sleep(this.mirrorQueryFrequency);
            }

        }
        executor.shutdown();
    }

    public void handle(MirrorTopicMessages mirrorTopicMessages) {
        try {
            for (MirrorTopicMessage mirrorTopicMessage : mirrorTopicMessages.messages) {
                log.debug("Got HCS Message");
                String auctionData = mirrorTopicMessage.message();
                JsonObject auctionJson = new JsonObject(auctionData);
                Auction newAuction = new Auction().fromJson(auctionJson);
                Instant consensusTime = Utils.timestampToInstant(mirrorTopicMessage.consensusTimestamp);
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
                newAuction.setProcessrefunds(false);

                @Var Auction auction;
                try {
                    log.debug("Adding auction to database");
                    auction = auctionsRepository.getAuction(newAuction.getAuctionaccountid());
                    // auction doesn't exist, create it
                    if (auction == null) {
                        if (!testing) {
                            // get token info
                            Client client = hederaClient.client();
                            try {
                                log.debug("Getting token info");
                                TokenInfo tokenInfo = new TokenInfoQuery()
                                        .setTokenId(TokenId.fromString(newAuction.getTokenid()))
                                        .execute(client);

                                // store the IPFS url against the auction
                                if (tokenInfo.symbol.contains("ipfs")) {
                                    // set token image data
                                    newAuction.setTokenmetadata(tokenInfo.symbol);
                                }
                            } catch (Exception e) {
                                log.error("error getting token info", e);
                                throw e;
                            }
                            // get auction account info
                            log.debug("getting auction account info");
                            AccountInfo accountInfo = new AccountInfoQuery()
                                    .setAccountId(AccountId.fromString(newAuction.getAuctionaccountid()))
                                    .execute(client);

                            // only process refunds for this auction if the auction's key contains the operator's public key
                            newAuction.setProcessrefunds(accountInfo.key.toString().toUpperCase().contains(hederaClient.operatorPublicKey().toString().toUpperCase()));
                        }

                        auction = auctionsRepository.add(newAuction);

                        if ((auction.getId() != 0) && !testing) {
                            log.info("Auction for token {} added", newAuction.getTokenid());
                        }

                        if (!skipReadinessWatcher) {
                            auctionReadinessWatcher = new AuctionReadinessWatcher(hederaClient, auctionsRepository, auction, mirrorQueryFrequency, runOnce);
                            if (this.runOnce) {
                                // don't run as a thread
                                auctionReadinessWatcher.run();
                            } else {
                                // Start a thread to watch this new auction for readiness
                                Thread t = new Thread(auctionReadinessWatcher);
                                t.start();
                            }
                        }

                    }

                    // do we need to associate
                    // If refund key and master node, associate with the token
                    //TODO: Currently only available to the master node, this feature should eventually
                    // transition to use scheduled transactions
                    if (!StringUtils.isEmpty(masterKey)) {

                        Client client = hederaClient.auctionClient(auction, PrivateKey.fromString(masterKey));

                        AccountBalance accountBalance = new AccountBalanceQuery()
                                .setAccountId(AccountId.fromString(auction.getAuctionaccountid()))
                                .execute(client);

                        Map<TokenId, Long> tokens = accountBalance.token;
                        if (!tokens.containsKey(TokenId.fromString(auction.getTokenid()))) {
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
                                    log.error("Token association failed {}", receipt.status);
                                } else {
                                    log.info("Scheduled transaction to associate token {} with auction account {}", auction.getTokenid(), auction.getAuctionaccountid());
                                }
                            } catch (PrecheckStatusException e) {
                                if (e.status != Status.TOKEN_ALREADY_ASSOCIATED_TO_ACCOUNT) {
                                    log.error("Error during association", e);
                                }
                            }
                        }
                    }

                } catch (SQLException e) {
                    log.error("unable to determine if auction already exists", e);
                }
            }
        } catch (Exception e) {
            log.error(e, e);
        }

    }
}
