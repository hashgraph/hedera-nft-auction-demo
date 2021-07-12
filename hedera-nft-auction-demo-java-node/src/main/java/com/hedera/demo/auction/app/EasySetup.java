package com.hedera.demo.auction.app;

import com.google.errorprone.annotations.Var;
import com.hedera.demo.auction.app.repository.AuctionsRepository;
import com.hedera.demo.auction.app.repository.BidsRepository;
import com.hedera.demo.auction.app.repository.ValidatorsRepository;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Automates the creation of test data including
 * - Topic
 * - Auction account
 * - Token
 * - Transfer of token to the auction account
 *
 * Optionally deletes the contents of the database (for testing purposes)
 */
@Log4j2
public class EasySetup extends AbstractCreate {

    public EasySetup() throws Exception {
        super();
    }
    @SuppressWarnings("FieldMissingNullable")
    final static String url = Objects.requireNonNull(env.get("DATABASE_URL"), "missing environment variable DATABASE_URL");
    @SuppressWarnings("FieldMissingNullable")
    final static String username = Objects.requireNonNull(
            env.get("DATABASE_USERNAME"), "missing environment variable DATABASE_USERNAME");
    @SuppressWarnings("FieldMissingNullable")
    final static String password = Objects.requireNonNull(
            env.get("DATABASE_PASSWORD"), "missing environment variable DATABASE_PASSWORD");

    final static SqlConnectionManager connectionManager = new SqlConnectionManager(url, username, password);
    final static AuctionsRepository auctionsRepository = new AuctionsRepository(connectionManager);
    final static BidsRepository bidsRepository = new BidsRepository(connectionManager);
    final static ValidatorsRepository validatorsRepository = new ValidatorsRepository(connectionManager);
    @SuppressWarnings("FieldMissingNullable")
    static String topicId = Optional.ofNullable(env.get("TOPIC_ID")).orElse("");

    public String setup(String[] args) throws Exception {
        Client client = hederaClient.client();

        @Var String symbol = "./sample-files/hedera-logo.jpg";
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
            validatorsRepository.deleteAllValidators();
            CreateTopic createTopic = new CreateTopic();
            topicId = createTopic.create().toString();
        }

        CreateToken createToken = new CreateToken();
        JsonObject tokenData = new JsonObject();
        tokenData.put("name", name);
        tokenData.put("symbol", symbol);
        tokenData.put("initialSupply", 1L);
        tokenData.put("decimals", 0);
        tokenData.put("memo", "");

        String filesPath = Utils.filesPath(env);
        Path filePath = Path.of(filesPath, symbol);

        if (Files.exists(filePath)) {
            JsonObject meta = new JsonObject();
            meta.put("type", "file");
            meta.put("description", symbol);
            tokenData.put("image", meta);
        }

        TokenId tokenId = createToken.create(tokenData.toString());

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
            Path initPath = Path.of(filesPath, "initDemo.json");

            FileWriter myWriter = new FileWriter(initPath.toFile(), UTF_8);
            myWriter.write(auction.encodePrettily());
            myWriter.close();

            log.info("*************************");
            log.info(" {} file written", initPath.toString());

            CreateAuction createAuction = new CreateAuction();
            createAuction.create("initDemo.json", topicId);
        } catch (Exception e) {
            log.error(e, e);
            throw e;
        }
        return topicId;
    }
    public static void main(String[] args) throws Exception {
        EasySetup easySetup = new EasySetup();
        easySetup.setup(args);
    }
}
