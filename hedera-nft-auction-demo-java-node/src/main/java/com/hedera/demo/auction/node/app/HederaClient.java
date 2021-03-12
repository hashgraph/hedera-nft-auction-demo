package com.hedera.demo.auction.node.app;

import com.google.errorprone.annotations.Var;
import com.hedera.hashgraph.sdk.AccountId;
import com.hedera.hashgraph.sdk.Client;
import com.hedera.hashgraph.sdk.PrivateKey;
import com.hedera.hashgraph.sdk.PublicKey;
import io.github.cdimascio.dotenv.Dotenv;
import lombok.extern.log4j.Log4j2;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Log4j2
public class HederaClient {
    private static final Dotenv env = Dotenv.configure().ignoreIfMissing().load();
    private static final AccountId OPERATOR_ID = AccountId.fromString(Objects.requireNonNull(env.get("OPERATOR_ID")));
    private static final PrivateKey OPERATOR_KEY = PrivateKey.fromString(Objects.requireNonNull(env.get("OPERATOR_KEY")));
    private static final String HEDERA_NETWORK = Optional.ofNullable(env.get("HEDERA_NETWORK").toUpperCase()).orElse("");
    private static final String CONFIG_FILE = env.get("CONFIG_FILE");
    private static final String MIRROR_PROVIDER = Optional.ofNullable(env.get("MIRROR_PROVIDER")).orElse("kabuto").toUpperCase();

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
    public static String getMirrorProvider() {return MIRROR_PROVIDER;}
    public static String getMirrorUrl() {
        String url = "";
        switch (HEDERA_NETWORK) {
            case "PREVIEWNET":
                switch (MIRROR_PROVIDER) {
                    case "DRAGONGLASS":
                        //TODO: Dragonglass API endpoint
                        break;
                    default:
                        url = "previewnet.mirrornode.hedera.com";
                        break;
                }
                break;
            case "TESTNET":
                switch (MIRROR_PROVIDER) {
                    case "HEDERA":
                        url = "testnet.mirrornode.hedera.com";
                        break;
                    case "DRAGONGLASS":
                        //TODO: Dragonglass API endpoint
                        break;
                    default:
                        url = "api.testnet.kabuto.sh/v1/";
                        break;
                }
                break;
            case "MAINNET":
                switch (MIRROR_PROVIDER) {
                    case "HEDERA":
                        url = "mainnet.mirrornode.hedera.com";
                        break;
                    case "DRAGONGLASS":
                        //TODO: Dragonglass API endpoint
                        break;
                    default:
                        url = "api.kabuto.sh/v1";
                        break;
                }
                break;
        }
        return url;
    }
    public static Client getClient() throws InterruptedException {
        @Var Client client = Client.forTestnet();

        switch (HEDERA_NETWORK) {
            case "PREVIEWNET":
                client = Client.forPreviewnet();
                break;
            case "TESTNET":
                client = Client.forTestnet();
                if ( ! MIRROR_PROVIDER.equals("HEDERA")) {
                    // the SDK doesn't support mirror subscriptions to dragonglass, default to kabuto
                    client.setMirrorNetwork(List.of("api.testnet.kabuto.sh:50211"));
                }
                break;
            case "MAINNET":
                client = Client.forMainnet();
                if ( ! MIRROR_PROVIDER.equals("HEDERA")) {
                    // the SDK doesn't support mirror subscriptions to dragonglass, default to kabuto
                    client.setMirrorNetwork(List.of("api.kabuto.sh:50211"));
                }
                break;
            default:
                log.error(".env configuration missing.");
                System.exit(0);
        }

        client.setOperator(OPERATOR_ID, OPERATOR_KEY);

        return client;
    }
}
