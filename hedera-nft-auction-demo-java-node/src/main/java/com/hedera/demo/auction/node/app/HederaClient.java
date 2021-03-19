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
    private static final String VUE_APP_NETWORK = Optional.ofNullable(env.get("VUE_APP_NETWORK").toUpperCase()).orElse("");
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
    public static String getMirrorUrl() throws Exception {
        @Var String url = "";
        switch (VUE_APP_NETWORK) {
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
            default:
                throw new Exception("VUE_APP_NETWORK environment variable not set");

        }
        return url;
    }
    public static Client getClient() throws Exception {
        @Var Client client = Client.forTestnet();

        switch (VUE_APP_NETWORK) {
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
                throw new Exception("VUE_APP_NETWORK environment variable not set");
        }

        client.setOperator(OPERATOR_ID, OPERATOR_KEY);

        return client;
    }
}
