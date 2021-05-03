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
    private AccountId operatorId;
    private PrivateKey operatorKey;
    private String mirrorProvider;
    private String mirrorUrl = "";
    private final Client client;
    private String network;

    public HederaClient(AccountId operatorId, PrivateKey operatorKey, String network, String mirrorProvider, String mirrorUrl, String mirrorAddress) throws Exception {
        this.operatorId = operatorId;
        this.operatorKey = operatorKey;
        this.mirrorProvider = mirrorProvider.toUpperCase();
        this.client = clientForNetwork(network);
        this.mirrorUrl = mirrorUrl;
        this.network = network;

        if ( ! StringUtils.isEmpty(mirrorAddress)) {
            client.setMirrorNetwork(List.of(mirrorAddress));
        }

    }
    public HederaClient(Dotenv env) throws Exception {
        this.operatorId = AccountId.fromString(Objects.requireNonNull(env.get("OPERATOR_ID")));
        this.operatorKey = PrivateKey.fromString(Objects.requireNonNull(env.get("OPERATOR_KEY")));
        this.mirrorProvider = Optional.ofNullable(env.get("MIRROR_PROVIDER")).orElse("KABUTO");
        this.mirrorProvider = this.mirrorProvider.toUpperCase();

        this.network = Optional.ofNullable(env.get("VUE_APP_NETWORK")).orElse("");
        this.network = this.network.toUpperCase();
        this.client = clientForNetwork(this.network);
        setClientMirror(env);
    }

    public HederaClient() throws Exception {
        this(Dotenv.load());
    }

    public static HederaClient emptyTestClient() throws Exception {
        return new HederaClient(AccountId.fromString("0.0.1"), PrivateKey.generate(), "TESTNET", "hedera", "", "");
    }

    public void setTestingMirrorURL(String testUrl) {
        this.mirrorUrl = testUrl;
    }

    public void setClientMirror(Dotenv env) throws Exception {
        @Var String envVariable = "GRPC_".concat(this.mirrorProvider).concat("_")
                .concat(this.network);
        String url = env.get(envVariable);
        if (StringUtils.isBlank(url)) {
            throw new Exception("VUE_APP_NETWORK and/or MIRROR_PROVIDER environment variables not set");
        }

        this.client.setMirrorNetwork(List.of(url));

        envVariable = "REST_".concat(this.mirrorProvider).concat("_")
                .concat(this.network);
        this.mirrorUrl = env.get(envVariable);
        if (StringUtils.isBlank(this.mirrorUrl)) {
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
    public void setMirrorProvider(String mirrorProvider) {
        this.mirrorProvider = mirrorProvider.toUpperCase();
    }
    public String mirrorUrl() {return this.mirrorUrl;}
    public Client client() {return this.client;}
    public void setOperator(AccountId operatorId, PrivateKey operatorKey) {
        this.operatorId = operatorId;
        this.operatorKey = operatorKey;
        client.setOperator(this.operatorId, this.operatorKey);
    }

    private Client clientForNetwork(String network) throws Exception {
        Client client;
        switch (network) {
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
                log.error("Unknown network " + network);
                throw new Exception("Unknown network " + network);
        }

        client.setOperator(this.operatorId, this.operatorKey);

        return client;
    }
}
