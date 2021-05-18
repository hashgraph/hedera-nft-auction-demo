package com.hedera.demo.auction.node.app;

import com.google.errorprone.annotations.Var;
import com.hedera.demo.auction.node.app.repository.AuctionsRepository;
import com.hedera.demo.auction.node.app.repository.BidsRepository;
import com.hedera.demo.auction.node.app.repository.ScheduledOperationsLogRepository;
import com.hedera.demo.auction.node.app.repository.ScheduledOperationsRepository;
import com.hedera.hashgraph.sdk.AccountId;
import com.hedera.hashgraph.sdk.Client;
import com.hedera.hashgraph.sdk.Status;
import com.hedera.hashgraph.sdk.TokenAssociateTransaction;
import com.hedera.hashgraph.sdk.TokenId;
import com.hedera.hashgraph.sdk.TransactionReceipt;
import com.hedera.hashgraph.sdk.TransactionResponse;
import io.vertx.core.json.JsonObject;
import lombok.extern.log4j.Log4j2;

import java.io.FileWriter;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static java.nio.charset.StandardCharsets.UTF_8;

@Log4j2
public class EasySetup extends AbstractCreate {

    public EasySetup() throws Exception {
        super();
    }

    final static String url = Objects.requireNonNull(env.get("DATABASE_URL"), "missing environment variable DATABASE_URL");
    final static String username = Objects.requireNonNull(
            env.get("DATABASE_USERNAME"), "missing environment variable DATABASE_USERNAME");
    final static String password = Objects.requireNonNull(
            env.get("DATABASE_PASSWORD"), "missing environment variable DATABASE_PASSWORD");

    final static SqlConnectionManager connectionManager = new SqlConnectionManager(url, username, password);
    final static AuctionsRepository auctionsRepository = new AuctionsRepository(connectionManager);
    final static BidsRepository bidsRepository = new BidsRepository(connectionManager);
    final static ScheduledOperationsRepository scheduledOperationsRepository = new ScheduledOperationsRepository(connectionManager);
    final static ScheduledOperationsLogRepository scheduledOperationsLogRepository = new ScheduledOperationsLogRepository(connectionManager);

    static String topicId = Optional.ofNullable(env.get("VUE_APP_TOPIC_ID")).orElse("");

    public String setup(String[] args) throws Exception {
        Client client = hederaClient.client();

        @Var String symbol = "./sample-files/hedera-logo-base64.txt";
        @Var boolean clean = true;
        @Var String name = "Test Token";

        for (String arg : args) {
            if (arg.startsWith("--symbol")) {
                symbol = arg.replace("--symbol=","");
            }
            if (arg.startsWith("--no-clean")) {
                clean = false;
            }
            if (arg.startsWith("--name")) {
                name = arg.replace("--name=","");
            }
        }
        if (clean) {
            log.info("Deleting existing auctions and bids and creating new topic");
            bidsRepository.deleteAllBids();
            auctionsRepository.deleteAllAuctions();
            scheduledOperationsRepository.deleteAllScheduledOperations();
            scheduledOperationsLogRepository.deleteAllScheduledLogOperations();
            CreateTopic createTopic = new CreateTopic();
            topicId = createTopic.create().toString();
        }

        CreateToken createToken = new CreateToken();
        TokenId tokenId = createToken.create(name, symbol, 1L, 0, "");
//        String key = client.getOperatorPublicKey().toString();
        CreateAuctionAccount createAuctionAccount = new CreateAuctionAccount();
        AccountId auctionAccount = createAuctionAccount.create(100, "");
        // associate auction account with token
        try {
            TransactionResponse response = new TokenAssociateTransaction()
                    .setAccountId(auctionAccount)
                    .setTokenIds(List.of(tokenId))
                    .execute(client);

            TransactionReceipt receipt = response.getReceipt(client);

            if (receipt.status != Status.SUCCESS) {
                log.error("error associating with token");
            }
            CreateTokenTransfer createTokenTransfer = new CreateTokenTransfer();
            createTokenTransfer.transfer(tokenId.toString(), auctionAccount.toString());

            JsonObject auction = new JsonObject();
            auction.put("tokenid", tokenId.toString());
            auction.put("auctionaccountid", auctionAccount.toString());
            auction.put("reserve", 0);
            auction.put("minimumbid", 1000000);
            auction.put("winnercanbid", true);
            auction.put("title", "auction title");
            auction.put("description", "auction description");

            // store auction data in initDemo.json file
            FileWriter myWriter = new FileWriter("./sample-files/initDemo.json", UTF_8);
            myWriter.write(auction.encodePrettily());
            myWriter.close();

            log.info("*************************");
            log.info(" ./sample-files/initDemo.json file written");

            CreateAuction createAuction = new CreateAuction();
            createAuction.create("./sample-files/initDemo.json", topicId);
        } catch (Exception e) {
            log.error(e);
            throw e;
        }
        return topicId;
    }
    public static void main(String[] args) throws Exception {
        EasySetup easySetup = new EasySetup();
        easySetup.setup(args);
    }
}
