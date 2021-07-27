package com.hedera.demo.auction.test.system.app;

import com.hedera.demo.auction.app.SqlConnectionManager;
import com.hedera.demo.auction.app.repository.AuctionsRepository;
import com.hedera.demo.auction.app.repository.BidsRepository;
import com.hedera.demo.auction.test.system.AbstractSystemTest;
import com.hedera.hashgraph.sdk.Key;
import com.hedera.hashgraph.sdk.KeyList;
import com.hedera.hashgraph.sdk.PrivateKey;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.testcontainers.containers.PostgreSQLContainer;

import java.util.Arrays;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class AuctionAccountCreateSystemTest extends AbstractSystemTest {

    String pk01 = PrivateKey.generate().getPublicKey().toString();
    String pk02 = PrivateKey.generate().getPublicKey().toString();

    AuctionAccountCreateSystemTest() throws Exception {
        super();
    }

    @BeforeAll
    public void beforeAll() {
        postgres = new PostgreSQLContainer("postgres:12.6");
        postgres.start();
        migrate(postgres);
        SqlConnectionManager connectionManager = new SqlConnectionManager(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
        auctionsRepository = new AuctionsRepository(connectionManager);
        bidsRepository = new BidsRepository(connectionManager);
    }

    @AfterAll
    public void afterAll() {
        this.postgres.close();
    }

    @Test
    public void testCreateAccountTwoThresholdLists() throws Exception {

        JsonObject keysCreate = jsonThresholdKey(2, pk01, pk02);

        createAccountAndGetInfo(keysCreate);

        assertNotNull(accountInfo);
        KeyList accountKeyList = (KeyList) accountInfo.key;

        assertEquals(2, accountKeyList.size());
        assertEquals(1, accountKeyList.threshold);

        Object[] keyListArray = accountKeyList.toArray();

        Key accountKey = (Key)keyListArray[1];
        assertEquals(masterKey.getPublicKey().toString(), accountKey.toString());

        KeyList accountKeys = (KeyList)keyListArray[0];
        assertEquals(2, accountKeys.size());
        assertEquals(2, accountKeys.threshold);

        String[] pubKeys = keylistToStringArray(accountKeys);
        assertTrue(Arrays.asList(pubKeys).contains(pk01));
        assertTrue(Arrays.asList(pubKeys).contains(pk02));

        assertEquals(initialBalance * 100000000, accountInfo.balance.toTinybars());
        assertFalse(accountInfo.isReceiverSignatureRequired);
    }
}
