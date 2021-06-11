package com.hedera.demo.auction.test.system.app;

import com.hedera.demo.auction.app.CreateToken;
import com.hedera.demo.auction.test.system.AbstractSystemTest;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TokenCreateSystemTest extends AbstractSystemTest {

    TokenCreateSystemTest() throws Exception {
        super();
    }

    @Test
    public void testCreateTokenNoFile() throws Exception {
        CreateToken createToken = new CreateToken();
        JsonObject tokenData = new JsonObject();
        tokenData.put("name", tokenName);
        tokenData.put("symbol", symbol);
        tokenData.put("initialSupply", initialSupply);
        tokenData.put("decimals", decimals);
        tokenData.put("memo", tokenMemo);

        tokenId = createToken.create(tokenData.toString());

        getTokenInfo();

        assertEquals(tokenName, tokenInfo.name);
        assertEquals(symbol, tokenInfo.symbol);
        assertNull(tokenInfo.adminKey);
        assertEquals(tokenId.toString(), tokenInfo.tokenId.toString());
        assertEquals(initialSupply, tokenInfo.totalSupply);
        assertEquals(decimals, tokenInfo.decimals);
        assertEquals(tokenMemo, tokenInfo.tokenMemo);
        assertEquals(hederaClient.operatorId().toString(), tokenInfo.treasuryAccountId.toString());
        assertNull(tokenInfo.freezeKey);
        assertNull(tokenInfo.kycKey);
        assertNull(tokenInfo.supplyKey);
        assertNull(tokenInfo.wipeKey);
    }
}
