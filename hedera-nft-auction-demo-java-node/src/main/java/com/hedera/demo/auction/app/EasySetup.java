package com.hedera.demo.auction.app;

import com.hedera.demo.auction.app.api.RequestCreateAuction;
import com.hedera.demo.auction.app.api.RequestCreateAuctionAccount;
import com.hedera.demo.auction.app.api.RequestCreateAuctionAccountKey;
import com.hedera.demo.auction.app.api.RequestCreateToken;
import com.hedera.demo.auction.app.api.RequestEasySetup;
import com.hedera.demo.auction.app.api.RequestTokenTransfer;
import com.hedera.hashgraph.sdk.AccountId;
import com.hedera.hashgraph.sdk.Client;
import com.hedera.hashgraph.sdk.Status;
import com.hedera.hashgraph.sdk.TokenAssociateTransaction;
import com.hedera.hashgraph.sdk.TokenId;
import com.hedera.hashgraph.sdk.TransactionReceipt;
import com.hedera.hashgraph.sdk.TransactionResponse;
import lombok.extern.log4j.Log4j2;

import java.util.List;

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

    public String setup(RequestEasySetup requestEasySetup) throws Exception {
        Client client = hederaClient.client();

        if (requestEasySetup.clean) {
            log.info("Deleting existing auctions and bids and creating new topic");
            if (bidsRepository != null) {
                bidsRepository.deleteAllBids();
            }
            if (auctionsRepository != null) {
                auctionsRepository.deleteAllAuctions();
            }
            if (validatorsRepository != null) {
                validatorsRepository.deleteAllValidators();
            }
            CreateTopic createTopic = new CreateTopic();
            topicId = createTopic.create().toString();
        }

        CreateToken createToken = new CreateToken(filesPath);
        RequestCreateToken requestCreateToken = new RequestCreateToken();
        requestCreateToken.setName(requestEasySetup.getName());
        requestCreateToken.setSymbol(requestEasySetup.getSymbol());
        requestCreateToken.initialSupply = 1L;
        requestCreateToken.decimals = 0;
        requestCreateToken.setMemo("");

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
            requestCreateAuction.setTitle("auction title");
            requestCreateAuction.setDescription("auction description");
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
