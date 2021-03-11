package com.hedera.demo.auction.node.app;

import com.google.errorprone.annotations.Var;
import com.hedera.hashgraph.sdk.AccountId;
import com.hedera.hashgraph.sdk.Client;
import com.hedera.hashgraph.sdk.PrivateKey;
import com.hedera.hashgraph.sdk.PublicKey;
import io.github.cdimascio.dotenv.Dotenv;
import lombok.extern.log4j.Log4j2;

import java.util.Objects;

@Log4j2
public class HederaClient {
    private static final Dotenv env = Dotenv.configure().ignoreIfMissing().load();
    private static final AccountId OPERATOR_ID = AccountId.fromString(Objects.requireNonNull(env.get("OPERATOR_ID")));
    private static final PrivateKey OPERATOR_KEY = PrivateKey.fromString(Objects.requireNonNull(env.get("OPERATOR_KEY")));
    private static final String HEDERA_NETWORK = env.get("HEDERA_NETWORK");
    private static final String CONFIG_FILE = env.get("CONFIG_FILE");

    private HederaClient() {
    }
    public static PrivateKey getOperatorKey() {
        return OPERATOR_KEY;
    }
    public static AccountId getOperatorId() {
        return OPERATOR_ID;
    }
    public static PublicKey getOperatorPublicKey() {
        return OPERATOR_KEY.getPublicKey();
    }
    public static Client getClient() {
        @Var Client client = Client.forTestnet();

        if (HEDERA_NETWORK != null && HEDERA_NETWORK.equals("previewnet")) {
            client = Client.forPreviewnet();
        } else if (HEDERA_NETWORK != null && HEDERA_NETWORK.equals("testnet")) {
            client = Client.forTestnet();
        } else if (HEDERA_NETWORK != null && HEDERA_NETWORK.equals("mainnet")) {
            client = Client.forMainnet();
        } else {
            try {
                client = Client.fromConfigFile(CONFIG_FILE != null ? CONFIG_FILE : "");
            } catch (Exception e) {
                log.error(".env configuration missing or invalid HEDERA_NETWORK or CONFIG_FILE.");
                System.exit(0);
            }
        }

        client.setOperator(OPERATOR_ID, OPERATOR_KEY);

        return client;
    }
}
