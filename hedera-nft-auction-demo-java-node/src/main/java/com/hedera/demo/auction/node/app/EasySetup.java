package com.hedera.demo.auction.node.app;

import com.google.errorprone.annotations.Var;
import com.hedera.demo.auction.node.app.repository.AuctionsRepository;
import com.hedera.demo.auction.node.app.repository.BidsRepository;
import com.hedera.hashgraph.sdk.AccountId;
import com.hedera.hashgraph.sdk.Client;
import com.hedera.hashgraph.sdk.TokenId;
import io.vertx.core.json.JsonObject;
import lombok.extern.log4j.Log4j2;

import java.io.FileWriter;
import java.util.Optional;

import static java.nio.charset.StandardCharsets.UTF_8;

@Log4j2
public class EasySetup extends AbstractCreate {

    public EasySetup() throws Exception {
        hederaClient = new HederaClient(env);
    }

    final static SqlConnectionManager connectionManager = new SqlConnectionManager(env);
    final static AuctionsRepository auctionsRepository = new AuctionsRepository(connectionManager);
    final static BidsRepository bidsRepository = new BidsRepository(connectionManager);
    static String topicId = Optional.ofNullable(env.get("VUE_APP_TOPIC_ID")).orElse("");

    public void setup(String[] args) throws Exception {
        Client client = hederaClient.client();

        @Var String symbol = "TT";
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
            CreateTopic createTopic = new CreateTopic();
            topicId = createTopic.create().toString();
        }

        CreateToken createToken = new CreateToken();
        TokenId tokenId = createToken.create(name, symbol, 1L, 0);
        String key = client.getOperatorPublicKey().toString();
        CreateAuctionAccount createAuctionAccount = new CreateAuctionAccount();
        AccountId auctionAccount = createAuctionAccount.create(100, key);
        CreateTokenTransfer createTokenTransfer = new CreateTokenTransfer();
        createTokenTransfer.transfer(tokenId.toString(), auctionAccount.toString());

        JsonObject auction = new JsonObject();
        auction.put("tokenid", tokenId.toString());
        auction.put("auctionaccountid", auctionAccount.toString());
        auction.put("reserve", 0);
        auction.put("minimumbid", 10);
        auction.put("winnercanbid", true);

        // store auction data in initDemo.json file
        FileWriter myWriter = new FileWriter("./sample-files/initDemo.json", UTF_8);
        myWriter.write(auction.encodePrettily());
        myWriter.close();

        log.info("*************************");
        log.info(" ./sample-files/initDemo.json file written");

        CreateAuction createAuction = new CreateAuction();
        createAuction.create("./sample-files/initDemo.json", topicId);
    }
    public void main(String[] args) throws Exception {
        EasySetup easySetup = new EasySetup();
        easySetup.setup(args);
    }
}
