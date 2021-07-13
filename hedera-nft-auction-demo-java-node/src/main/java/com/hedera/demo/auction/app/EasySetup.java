package com.hedera.demo.auction.app;

import com.hedera.demo.auction.app.api.RequestCreateAuction;
import com.hedera.demo.auction.app.api.RequestCreateAuctionAccount;
import com.hedera.demo.auction.app.api.RequestCreateAuctionAccountKey;
import com.hedera.demo.auction.app.api.RequestCreateToken;
import com.hedera.demo.auction.app.api.RequestEasySetup;
import com.hedera.demo.auction.app.api.RequestTokenTransfer;
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
import lombok.extern.log4j.Log4j2;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

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
    @SuppressWarnings("FieldMissingNullable")
    private final String filesPath = Optional.ofNullable(env.get("FILES_LOCATION")).orElse("./sample-files");

    public String setup(RequestEasySetup requestEasySetup) throws Exception {
        Client client = hederaClient.client();

        if (requestEasySetup.clean) {
            log.info("Deleting existing auctions and bids and creating new topic");
            bidsRepository.deleteAllBids();
            auctionsRepository.deleteAllAuctions();
            validatorsRepository.deleteAllValidators();
            CreateTopic createTopic = new CreateTopic();
            topicId = createTopic.create().toString();
        }

        CreateToken createToken = new CreateToken(filesPath);
        RequestCreateToken requestCreateToken = new RequestCreateToken();
        requestCreateToken.name = requestEasySetup.name;
        requestCreateToken.symbol = requestEasySetup.symbol;
        requestCreateToken.initialSupply = 1L;
        requestCreateToken.decimals = 0;
        requestCreateToken.memo = "";

        TokenId tokenId = createToken.create(requestCreateToken);

        CreateAuctionAccount createAuctionAccount = new CreateAuctionAccount();
        RequestCreateAuctionAccount requestCreateAuctionAccount = new RequestCreateAuctionAccount();
        requestCreateAuctionAccount.initialBalance = 100;
        requestCreateAuctionAccount.keylist.threshold = 1;
        RequestCreateAuctionAccountKey requestCreateAuctionAccountKey = new RequestCreateAuctionAccountKey();
        requestCreateAuctionAccountKey.key = hederaClient.operatorPublicKey().toString();
        requestCreateAuctionAccount.keylist.keys.add(requestCreateAuctionAccountKey);
        AccountId auctionAccount = createAuctionAccount.create(requestCreateAuctionAccount);
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
            RequestTokenTransfer requestTokenTransfer = new RequestTokenTransfer();
            requestTokenTransfer.tokenid = tokenId.toString();
            requestTokenTransfer.auctionaccountid = auctionAccount.toString();
            createTokenTransfer.transfer(requestTokenTransfer);

            RequestCreateAuction requestCreateAuction = new RequestCreateAuction();
            requestCreateAuction.tokenid = tokenId.toString();
            requestCreateAuction.auctionaccountid = auctionAccount.toString();
            requestCreateAuction.reserve = 0;
            requestCreateAuction.minimumbid = 1000000;
            requestCreateAuction.winnercanbid = true;
            requestCreateAuction.title = "auction title";
            requestCreateAuction.description = "auction description";
            requestCreateAuction.topicid = topicId;

            CreateAuction createAuction = new CreateAuction();
            createAuction.create(requestCreateAuction);
        } catch (Exception e) {
            log.error(e, e);
            throw e;
        }
        return topicId;
    }
}
