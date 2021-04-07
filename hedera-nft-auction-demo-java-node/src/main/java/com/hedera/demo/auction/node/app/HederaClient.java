package com.hedera.demo.auction.node.app;

import com.google.errorprone.annotations.Var;
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
    private final AccountId operatorId;
    private final PrivateKey operatorKey;
    private final String mirrorProvider;
    private final String mirrorUrl;
    private final Client client;

    public HederaClient(Dotenv env) throws Exception {
        this.operatorId = AccountId.fromString(Objects.requireNonNull(env.get("OPERATOR_ID")));
        this.operatorKey = PrivateKey.fromString(Objects.requireNonNull(env.get("OPERATOR_KEY")));
        this.mirrorProvider = Optional.ofNullable(env.get("MIRROR_PROVIDER").toUpperCase()).orElse("KABUTO");

        String vueAppNetwork = Optional.ofNullable(env.get("VUE_APP_NETWORK").toUpperCase()).orElse("");

        switch (vueAppNetwork) {
            case "PREVIEWNET":
                this.client = Client.forPreviewnet();
                break;
            case "TESTNET":
                this.client = Client.forTestnet();
                break;
            case "MAINNET":
                this.client = Client.forMainnet();
                break;
            default:
                log.error(".env configuration missing.");
                throw new Exception("VUE_APP_NETWORK environment variable not set");
        }

        @Var String envVariable = "GRPC_".concat(this.mirrorProvider).concat("_")
                .concat(vueAppNetwork);
        String url = env.get(envVariable);
        if (StringUtils.isBlank(url)) {
            throw new Exception("VUE_APP_NETWORK and/or MIRROR_PROVIDER environment variables not set");
        }

        this.client.setMirrorNetwork(List.of(url));

        this.client.setOperator(this.operatorId, this.operatorKey);

        envVariable = "REST_".concat(this.mirrorProvider).concat("_")
                .concat(vueAppNetwork);
        this.mirrorUrl = env.get(envVariable);
        if (StringUtils.isBlank(url)) {
            throw new Exception("VUE_APP_NETWORK and/or MIRROR_PROVIDER environment variables not set");
        }
    }

    public PrivateKey operatorPrivateKey() {
        return this.operatorKey;
    }
    public PublicKey operatorPublicKey() { return this.operatorKey.getPublicKey(); }
    public AccountId operatorId() {
        return this.operatorId;
    }
    public String mirrorProvider() {return this.mirrorProvider;}
    public String mirrorUrl() {return this.mirrorUrl;}
    public Client client() {return this.client;}
}
