package com.hedera.demo.auction.node.test.system.app;

import com.google.protobuf.ByteString;
import com.hedera.demo.auction.node.app.CreateToken;
import com.hedera.demo.auction.node.app.HederaClient;
import com.hedera.hashgraph.sdk.*;
import io.github.cdimascio.dotenv.Dotenv;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.io.File;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TokenCreateTest {
    private static final Dotenv dotenv = Dotenv.configure().filename(".env.system").ignoreIfMissing().load();
    private static CreateToken createToken;
    private static HederaClient hederaClient;
    private static final String tokenName = "TestToken";
    private static final long initialSupply = 10;
    private static final int decimals = 2;
    private static final String symbol = "TestSymbol";

    @BeforeAll
    public void beforeAll() throws Exception {
        hederaClient = new HederaClient(dotenv);
        createToken = new CreateToken();
        createToken.setEnv(dotenv);
    }
    @Test
    public void testCreateTokenNoFile() throws Exception {
        TokenId tokenId = createToken.create(tokenName, symbol, initialSupply, decimals);

        TokenInfo tokenInfo = new TokenInfoQuery()
                .setTokenId(tokenId)
                .execute(hederaClient.client());

        assertEquals(tokenName, tokenInfo.name);
        assertEquals(symbol, tokenInfo.symbol);
        assertNull(tokenInfo.adminKey);
        assertEquals(tokenId.toString(), tokenInfo.tokenId.toString());
        assertEquals(initialSupply, tokenInfo.totalSupply);
        assertEquals(decimals, tokenInfo.decimals);
        assertEquals(hederaClient.operatorId().toString(), tokenInfo.treasuryAccountId.toString());
        assertNull(tokenInfo.freezeKey);
        assertNull(tokenInfo.kycKey);
        assertNull(tokenInfo.supplyKey);
        assertNull(tokenInfo.wipeKey);
    }

    @Test
    public void testCreateTokenWithSmallFile() throws Exception {
        // create a test file
        String fileTestData = "some test data here";
        File tempFile = File.createTempFile("test-", "");
        PrintWriter printWriter = new PrintWriter(Files.newBufferedWriter(tempFile.toPath(), StandardCharsets.UTF_8));
        printWriter.print(fileTestData);
        printWriter.close();

        TokenId tokenId = createToken.create(tokenName, tempFile.getAbsolutePath(), initialSupply, decimals);

        TokenInfo tokenInfo = new TokenInfoQuery()
                .setTokenId(tokenId)
                .execute(hederaClient.client());

        assertEquals(tokenName, tokenInfo.name);
        assertNull(tokenInfo.adminKey);
        assertEquals(tokenId.toString(), tokenInfo.tokenId.toString());
        assertEquals(initialSupply, tokenInfo.totalSupply);
        assertEquals(decimals, tokenInfo.decimals);
        assertEquals(hederaClient.operatorId().toString(), tokenInfo.treasuryAccountId.toString());
        assertNull(tokenInfo.freezeKey);
        assertNull(tokenInfo.kycKey);
        assertNull(tokenInfo.supplyKey);
        assertNull(tokenInfo.wipeKey);

        // handle symbol
        assertTrue(tokenInfo.symbol.startsWith("HEDERA://"));
        String fileIdString = tokenInfo.symbol.replace("HEDERA://", "");

        FileInfo fileInfo = new FileInfoQuery()
                .setFileId(FileId.fromString(fileIdString))
                .execute(hederaClient.client());

        assertNull(fileInfo.keys);

        ByteString fileContents = new FileContentsQuery()
                .setFileId(FileId.fromString(fileIdString))
                .execute(hederaClient.client());

        assertEquals(fileTestData, fileContents.toString(StandardCharsets.UTF_8));

        Files.deleteIfExists(tempFile.toPath());
    }

    @Test
    public void testCreateTokenWithLargeFile() throws Exception {
        // create a test file
        byte[] array = new byte[6200]; // length is bounded by 7
        new Random().nextBytes(array);
        String fileTestData = new String(array, StandardCharsets.UTF_8);

        File tempFile = File.createTempFile("test-", "");
        PrintWriter printWriter = new PrintWriter(Files.newBufferedWriter(tempFile.toPath(), StandardCharsets.UTF_8));
        printWriter.print(fileTestData);
        printWriter.close();

        TokenId tokenId = createToken.create(tokenName, tempFile.getAbsolutePath(), initialSupply, decimals);

        TokenInfo tokenInfo = new TokenInfoQuery()
                .setTokenId(tokenId)
                .execute(hederaClient.client());

        assertEquals(tokenName, tokenInfo.name);
        assertNull(tokenInfo.adminKey);
        assertEquals(tokenId.toString(), tokenInfo.tokenId.toString());
        assertEquals(initialSupply, tokenInfo.totalSupply);
        assertEquals(decimals, tokenInfo.decimals);
        assertEquals(hederaClient.operatorId().toString(), tokenInfo.treasuryAccountId.toString());
        assertNull(tokenInfo.freezeKey);
        assertNull(tokenInfo.kycKey);
        assertNull(tokenInfo.supplyKey);
        assertNull(tokenInfo.wipeKey);

        // handle symbol
        assertTrue(tokenInfo.symbol.startsWith("HEDERA://"));
        String fileIdString = tokenInfo.symbol.replace("HEDERA://", "");

        FileInfo fileInfo = new FileInfoQuery()
                .setFileId(FileId.fromString(fileIdString))
                .execute(hederaClient.client());

        assertNull(fileInfo.keys);

        ByteString fileContents = new FileContentsQuery()
                .setFileId(FileId.fromString(fileIdString))
                .execute(hederaClient.client());

        assertEquals(fileTestData, fileContents.toString(StandardCharsets.UTF_8));

        Files.deleteIfExists(tempFile.toPath());
    }
}
