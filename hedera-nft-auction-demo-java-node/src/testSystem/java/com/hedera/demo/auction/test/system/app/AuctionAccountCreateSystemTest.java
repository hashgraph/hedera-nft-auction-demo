package com.hedera.demo.auction.test.system.app;

import com.hedera.demo.auction.app.SqlConnectionManager;
import com.hedera.demo.auction.app.repository.AuctionsRepository;
import com.hedera.demo.auction.app.repository.BidsRepository;
import com.hedera.demo.auction.test.system.AbstractSystemTest;
import com.hedera.hashgraph.sdk.Key;
import com.hedera.hashgraph.sdk.KeyList;
import com.hedera.hashgraph.sdk.PrivateKey;
import jakarta.json.JsonObject;
import org.junit.jupiter.api.*;
import org.testcontainers.containers.PostgreSQLContainer;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class AuctionAccountCreateSystemTest extends AbstractSystemTest {

    String pk01 = PrivateKey.generateED25519().getPublicKey().toString();
    String pk02 = PrivateKey.generateED25519().getPublicKey().toString();

    AuctionAccountCreateSystemTest() throws Exception {
        super();
    }

    @BeforeAll
    public void beforeAll() {
        postgres = new PostgreSQLContainer<>("POSTGRES_CONTAINER_VERSION");
        postgres.start();
        migrate(postgres);
        var connectionManager = new SqlConnectionManager(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
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

        Assertions.assertNotNull(accountInfo);
        var accountKeyList = (KeyList) accountInfo.key;

        assertEquals(2, accountKeyList.size());
        assertEquals(1, accountKeyList.threshold);

        Object[] keyListArray = accountKeyList.toArray();

        var accountKey = (Key)keyListArray[1];
        assertEquals(masterKey.getPublicKey().toString(), accountKey.toString());

        var accountKeys = (KeyList)keyListArray[0];
        assertEquals(2, accountKeys.size());
        assertEquals(2, accountKeys.threshold);

        String[] pubKeys = keylistToStringArray(accountKeys);
        Assertions.assertTrue(Arrays.asList(pubKeys).contains(pk01));
        Assertions.assertTrue(Arrays.asList(pubKeys).contains(pk02));

        assertEquals(initialBalance * 100000000, accountInfo.balance.toTinybars());
        assertFalse(accountInfo.isReceiverSignatureRequired);
    }
}
