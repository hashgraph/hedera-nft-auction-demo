package com.hedera.demo.auction.node.test.system.app;

import com.hedera.demo.auction.node.app.CreateAuctionAccount;
import com.hedera.demo.auction.node.app.CreateToken;
import com.hedera.demo.auction.node.app.CreateTokenTransfer;
import com.hedera.demo.auction.node.app.HederaClient;
import com.hedera.hashgraph.sdk.AccountBalance;
import com.hedera.hashgraph.sdk.AccountBalanceQuery;
import com.hedera.hashgraph.sdk.AccountId;
import com.hedera.hashgraph.sdk.TokenAssociateTransaction;
import com.hedera.hashgraph.sdk.TokenId;
import com.hedera.hashgraph.sdk.TransactionResponse;
import io.github.cdimascio.dotenv.Dotenv;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TokenTransferSystemTest {
    private static final Dotenv dotenv = Dotenv.configure().filename(".env.system").ignoreIfMissing().load();
    private static CreateTokenTransfer createTokenTransfer;
    private static HederaClient hederaClient;
    private static final String tokenName = "TestToken";
    private static final long initialSupply = 10;
    private static final int decimals = 2;
    private static final String symbol = "TestSymbol";
    private static TokenId tokenId;
    private static AccountId auctionAccountId;

    @BeforeAll
    public void beforeAll() throws Exception {
        hederaClient = new HederaClient(dotenv);
        createTokenTransfer = new CreateTokenTransfer();
        createTokenTransfer.setEnv(dotenv);
        CreateToken createToken = new CreateToken();
        createToken.setEnv(dotenv);
        tokenId = createToken.create(tokenName, symbol, initialSupply, decimals);
        CreateAuctionAccount createAuctionAccount = new CreateAuctionAccount();
        createAuctionAccount.setEnv(dotenv);
        auctionAccountId = createAuctionAccount.create(10, "");

        TransactionResponse response = new TokenAssociateTransaction()
                .setAccountId(auctionAccountId)
                .setTokenIds(List.of(tokenId))
                .execute(hederaClient.client());

        response.getReceipt(hederaClient.client());
    }
    @Test
    public void testTokenTransfer() throws Exception {
        createTokenTransfer.transfer(tokenId.toString(), auctionAccountId.toString());

        AccountBalance accountBalance = new AccountBalanceQuery()
                .setAccountId(auctionAccountId)
                .execute(hederaClient.client());

        Map<TokenId, Long> tokenBalances = accountBalance.token;
        assertTrue(tokenBalances.containsKey(tokenId));
        assertNotEquals(0, tokenBalances.get(tokenId));
    }
}
