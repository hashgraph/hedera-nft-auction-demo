package com.hedera.demo.auction.test.system.app;

import com.hedera.demo.auction.app.CreateToken;
import com.hedera.demo.auction.app.api.RequestCreateToken;
import com.hedera.demo.auction.app.api.RequestCreateTokenMetaData;
import com.hedera.demo.auction.test.system.AbstractSystemTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TokenCreateSystemTest extends AbstractSystemTest {

    TokenCreateSystemTest() throws Exception {
        super();
    }

    @Test
    public void testCreateToken() throws Exception {
        CreateToken createToken = new CreateToken(filesPath);
        RequestCreateToken requestCreateToken = new RequestCreateToken();
        requestCreateToken.setName(tokenName);
        requestCreateToken.setSymbol(symbol);
        requestCreateToken.initialSupply = initialSupply;
        requestCreateToken.decimals = decimals;
        requestCreateToken.setMemo(tokenMemo);

        tokenId = createToken.create(requestCreateToken);

        getTokenInfo();

        assertNotNull(tokenInfo);
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

    @Test
    public void testCreateTokenWithRequest() throws Exception {

        RequestCreateToken requestCreateToken = new RequestCreateToken();
        requestCreateToken.setName(tokenName);
        requestCreateToken.setSymbol(symbol);
        requestCreateToken.initialSupply = initialSupply;
        requestCreateToken.decimals = decimals;
        requestCreateToken.setMemo(tokenMemo);

        RequestCreateTokenMetaData image = new RequestCreateTokenMetaData();
        image.setDescription("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");
        image.type = "base64";
        requestCreateToken.image = image;

        RequestCreateTokenMetaData certificate = new RequestCreateTokenMetaData();
        certificate.type = "base64";
        certificate.setDescription("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");

        RequestCreateTokenMetaData description = new RequestCreateTokenMetaData();
        description.type = "string";
        description.setDescription("Describes the asset to which this token represents.");

        tokenId = createToken.create(requestCreateToken);

        getTokenInfo();

        assertNotNull(tokenInfo);
        assertEquals(tokenName, tokenInfo.name);
        assertTrue(tokenInfo.symbol.startsWith("https://cloudflare-ipfs.com/ipfs/"));
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
