package com.hedera.demo.auction.node.app;

import com.hedera.hashgraph.sdk.AccountId;
import com.hedera.hashgraph.sdk.Client;
import com.hedera.hashgraph.sdk.PrivateKey;
import com.hedera.hashgraph.sdk.PublicKey;
import io.github.cdimascio.dotenv.Dotenv;
import lombok.extern.log4j.Log4j2;
import org.jooq.tools.StringUtils;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Log4j2
public class HederaClient {
    private static final Dotenv env = Dotenv.configure().ignoreIfMissing().load();
    private static final AccountId OPERATOR_ID = AccountId.fromString(Objects.requireNonNull(env.get("OPERATOR_ID")));
    private static final PrivateKey OPERATOR_KEY = PrivateKey.fromString(Objects.requireNonNull(env.get("OPERATOR_KEY")));
    private static final String VUE_APP_NETWORK = Optional.ofNullable(env.get("VUE_APP_NETWORK").toUpperCase()).orElse("");
    private static final String MIRROR_PROVIDER = Optional.ofNullable(env.get("MIRROR_PROVIDER").toUpperCase()).orElse("KABUTO");

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
        String envVariable = "REST_".concat(MIRROR_PROVIDER).concat("_")
                .concat(VUE_APP_NETWORK);
        String url = env.get(envVariable);
        if (StringUtils.isBlank(url)) {
            throw new Exception("VUE_APP_NETWORK and/or MIRROR_PROVIDER environment variables not set");
        }
        return url;
    }

    public static Client getClient() throws Exception {
        Client client;

        switch (VUE_APP_NETWORK) {
            case "PREVIEWNET":
                client = Client.forPreviewnet();
                break;
            case "TESTNET":
                client = Client.forTestnet();
                break;
            case "MAINNET":
                client = Client.forMainnet();
                break;
            default:
                log.error(".env configuration missing.");
                throw new Exception("VUE_APP_NETWORK environment variable not set");
        }

        String envVariable = "GRPC_".concat(MIRROR_PROVIDER).concat("_")
                .concat(VUE_APP_NETWORK);
        String url = env.get(envVariable);
        if (StringUtils.isBlank(url)) {
            throw new Exception("VUE_APP_NETWORK and/or MIRROR_PROVIDER environment variables not set");
        }

        client.setMirrorNetwork(List.of(url));

        client.setOperator(OPERATOR_ID, OPERATOR_KEY);

        return client;
    }
}
