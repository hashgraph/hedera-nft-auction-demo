package com.hedera.demo.auction.test.system.app;

import com.hedera.demo.auction.test.system.AbstractSystemTest;
import com.hedera.hashgraph.sdk.TokenAssociateTransaction;
import com.hedera.hashgraph.sdk.TokenId;
import com.hedera.hashgraph.sdk.TransactionResponse;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TokenTransferSystemTest extends AbstractSystemTest {

    TokenTransferSystemTest() throws Exception {
        super();
    }

    @BeforeAll
    public void beforeEach() throws Exception {
        createTokenAndGetInfo(symbol);
        JsonObject keys = jsonThresholdKey(1, hederaClient.client().getOperatorPublicKey().toString());
        createAccountAndGetInfo(keys);

        TransactionResponse response = new TokenAssociateTransaction()
                .setAccountId(auctionAccountId)
                .setTokenIds(List.of(tokenId))
                .execute(hederaClient.client());

        response.getReceipt(hederaClient.client());
    }
    @Test
    public void testTokenTransfer() throws Exception {
        transferTokenAndGetBalance();

        assertNotNull(accountBalance);
        Map<TokenId, Long> tokenBalances = accountBalance.token;
        assertTrue(tokenBalances.containsKey(tokenId));
        assertNotEquals(0, tokenBalances.get(tokenId));
    }
}
