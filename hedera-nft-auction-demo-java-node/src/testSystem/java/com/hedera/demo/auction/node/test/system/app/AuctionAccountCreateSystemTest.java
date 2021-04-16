package com.hedera.demo.auction.node.test.system.app;

import com.google.errorprone.annotations.Var;
import com.hedera.demo.auction.node.app.CreateAuctionAccount;
import com.hedera.demo.auction.node.app.HederaClient;
import com.hedera.hashgraph.sdk.AccountId;
import com.hedera.hashgraph.sdk.AccountInfo;
import com.hedera.hashgraph.sdk.AccountInfoQuery;
import com.hedera.hashgraph.sdk.Key;
import com.hedera.hashgraph.sdk.KeyList;
import com.hedera.hashgraph.sdk.PrivateKey;
import io.github.cdimascio.dotenv.Dotenv;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.Arrays;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class AuctionAccountCreateSystemTest {
    private static final Dotenv dotenv = Dotenv.configure().filename(".env.system").ignoreIfMissing().load();
    private static CreateAuctionAccount createAuctionAccount;
    private static HederaClient hederaClient;
    private static final long initialBalance = 10;

    @BeforeAll
    public void beforeAll() throws Exception {
        hederaClient = new HederaClient(dotenv);
        createAuctionAccount = new CreateAuctionAccount();
        createAuctionAccount.setEnv(dotenv);
    }

    private static JsonObject jsonThresholdKey(int threshold, PrivateKey pk1, PrivateKey pk2) {
        JsonObject thresholdKey = new JsonObject();
        if (threshold != 0) {
            thresholdKey.put("threshold", threshold);
        }
        JsonArray keyList = new JsonArray();
        keyList.add(new JsonObject().put("key", pk1.getPublicKey().toString()));
        keyList.add(new JsonObject().put("key", pk2.getPublicKey().toString()));
        thresholdKey.put("keys", keyList);

        return thresholdKey;
    }

    private static String[] keylistToStringArray(KeyList keyList) {
        Object[] accountKeysWithin = keyList.toArray();
        String[] pubKeys = new String[keyList.size()];

        for (int i=0; i < keyList.size(); i++) {
            Key pubKey = (Key)accountKeysWithin[i];
            pubKeys[i] = pubKey.toString();
        }

        return pubKeys;
    }

    @Test
    public void testCreateAccountTwoLists() throws Exception {

        PrivateKey pk01 = PrivateKey.generate();
        PrivateKey pk02 = PrivateKey.generate();

        JsonObject thresholdKey1 = jsonThresholdKey(1, pk01, pk02);

        PrivateKey pk11 = PrivateKey.generate();
        PrivateKey pk12 = PrivateKey.generate();

        JsonObject thresholdKey2 = jsonThresholdKey(0, pk11, pk12);

        JsonObject keysCreate = new JsonObject().put("keylist", new JsonArray().add(thresholdKey1).add(thresholdKey2));

        AccountId accountId = createAuctionAccount.create(initialBalance, keysCreate.toString());

        AccountInfo accountInfo = new AccountInfoQuery()
                .setAccountId(accountId)
                .execute(hederaClient.client());
        System.out.println(accountInfo.key.toString());

        KeyList accountKeyList = (KeyList) accountInfo.key;

        assertEquals(2, accountKeyList.size());
        assertNull(accountKeyList.threshold);

        Object[] keyListArray = accountKeyList.toArray();

        @Var KeyList accountKeys = (KeyList)keyListArray[0];
        assertEquals(2, accountKeys.size());
        assertEquals(1, accountKeys.threshold);

        @Var String[] pubKeys = keylistToStringArray(accountKeys);
        assertTrue(Arrays.asList(pubKeys).contains(pk01.getPublicKey().toString()));
        assertTrue(Arrays.asList(pubKeys).contains(pk02.getPublicKey().toString()));

        accountKeys = (KeyList)keyListArray[1];
        assertEquals(2, accountKeys.size());
        assertNull(accountKeys.threshold);

        pubKeys = keylistToStringArray(accountKeys);
        assertTrue(Arrays.asList(pubKeys).contains(pk11.getPublicKey().toString()));
        assertTrue(Arrays.asList(pubKeys).contains(pk12.getPublicKey().toString()));

        assertEquals(initialBalance * 100000000, accountInfo.balance.toTinybars());
        assertFalse(accountInfo.isReceiverSignatureRequired);
    }

    @Test
    public void testCreateAccountOneThresholdList() throws Exception {

        PrivateKey pk01 = PrivateKey.generate();
        PrivateKey pk02 = PrivateKey.generate();

        JsonObject thresholdKey1 = jsonThresholdKey(1, pk01, pk02);

        JsonObject keysCreate = new JsonObject().put("keylist", new JsonArray().add(thresholdKey1));

        AccountId accountId = createAuctionAccount.create(initialBalance, keysCreate.toString());

        AccountInfo accountInfo = new AccountInfoQuery()
                .setAccountId(accountId)
                .execute(hederaClient.client());
        System.out.println(accountInfo.key.toString());

        KeyList accountKeyList = (KeyList) accountInfo.key;

        assertEquals(1, accountKeyList.size());
        assertNull(accountKeyList.threshold);

        Object[] keyListArray = accountKeyList.toArray();

        KeyList accountKeys = (KeyList)keyListArray[0];
        assertEquals(2, accountKeys.size());
        assertEquals(1, accountKeys.threshold);

        String[] pubKeys = keylistToStringArray(accountKeys);
        assertTrue(Arrays.asList(pubKeys).contains(pk01.getPublicKey().toString()));
        assertTrue(Arrays.asList(pubKeys).contains(pk02.getPublicKey().toString()));

        assertEquals(initialBalance * 100000000, accountInfo.balance.toTinybars());
        assertFalse(accountInfo.isReceiverSignatureRequired);
    }
    @Test
    public void testCreateAccountOneList() throws Exception {

        PrivateKey pk01 = PrivateKey.generate();
        PrivateKey pk02 = PrivateKey.generate();

        JsonObject thresholdKey1 = jsonThresholdKey(0, pk01, pk02);

        JsonObject keysCreate = new JsonObject().put("keylist", new JsonArray().add(thresholdKey1));

        AccountId accountId = createAuctionAccount.create(initialBalance, keysCreate.toString());

        AccountInfo accountInfo = new AccountInfoQuery()
                .setAccountId(accountId)
                .execute(hederaClient.client());
        System.out.println(accountInfo.key.toString());

        KeyList accountKeyList = (KeyList) accountInfo.key;

        assertEquals(1, accountKeyList.size());
        assertNull(accountKeyList.threshold);

        Object[] keyListArray = accountKeyList.toArray();

        KeyList accountKeys = (KeyList)keyListArray[0];
        assertEquals(2, accountKeys.size());
        assertNull(accountKeys.threshold);

        String[] pubKeys = keylistToStringArray(accountKeys);
        assertTrue(Arrays.asList(pubKeys).contains(pk01.getPublicKey().toString()));
        assertTrue(Arrays.asList(pubKeys).contains(pk02.getPublicKey().toString()));

        assertEquals(initialBalance * 100000000, accountInfo.balance.toTinybars());
        assertFalse(accountInfo.isReceiverSignatureRequired);
    }
}
