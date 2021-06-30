package com.hedera.demo.auction.app;

import com.hedera.demo.auction.app.domain.Auction;
import com.hedera.hashgraph.sdk.AccountId;
import com.hedera.hashgraph.sdk.Client;
import com.hedera.hashgraph.sdk.PrivateKey;
import com.hedera.hashgraph.sdk.PublicKey;
import io.github.cdimascio.dotenv.Dotenv;
import lombok.extern.log4j.Log4j2;
import org.jooq.tools.StringUtils;

import java.util.Objects;
import java.util.Optional;

/**
 * Sets up a client for the Hedera network
 */
@Log4j2
public class HederaClient {
    private AccountId operatorId;
    private PrivateKey operatorKey;
    private String mirrorProvider;
    private String mirrorUrl = "";
    private final Client client;
    private String network;

    /** Constructor
     *
     * @param operatorId the account id of the operator
     * @param operatorKey the private key of the operator
     * @param network the network to use
     * @param mirrorProvider the mirror provider to use
     * @param mirrorUrl the base url to the mirror node to use
     * @throws Exception in the event of an error
     */
    public HederaClient(AccountId operatorId, PrivateKey operatorKey, String network, String mirrorProvider, String mirrorUrl) throws Exception {
        this.operatorId = operatorId;
        this.operatorKey = operatorKey;
        this.mirrorProvider = mirrorProvider.toUpperCase();
        this.client = clientForNetwork(network);
        this.mirrorUrl = mirrorUrl;
        this.network = network;
    }

    /**
     * Constructor from environment variables
     *
     * @param env the container for the environment variables
     * @throws Exception in the event of an error
     */
    public HederaClient(Dotenv env) throws Exception {
        this.operatorId = AccountId.fromString(Objects.requireNonNull(env.get("OPERATOR_ID")));
        this.operatorKey = PrivateKey.fromString(Objects.requireNonNull(env.get("OPERATOR_KEY")));
        this.mirrorProvider = Optional.ofNullable(env.get("MIRROR_PROVIDER")).orElse("KABUTO");
        this.mirrorProvider = this.mirrorProvider.toUpperCase();

        this.network = Optional.ofNullable(env.get("NEXT_PUBLIC_NETWORK")).orElse("");
        this.network = this.network.toUpperCase();
        this.client = clientForNetwork(this.network);
        String envVariable = "REST_".concat(this.mirrorProvider.toUpperCase()).concat("_")
                .concat(this.network);
        this.mirrorUrl = "";
        if (env != null) {
            this.mirrorUrl = env.get(envVariable);
        }
        if (StringUtils.isBlank(this.mirrorUrl)) {
            throw new Exception("NEXT_PUBLIC_NETWORK and/or MIRROR_PROVIDER environment variables not set");
        }
    }

    /**
     * Default constructor
     *
     * @throws Exception in the event of an error
     */
    public HederaClient() throws Exception {
        this(Dotenv.load());
    }

    /**
     * Creates a client with dummy operator information for testing purposes
     * @return a HederaClient
     * @throws Exception in the event of an error
     */
    public static HederaClient emptyTestClient() throws Exception {
        return new HederaClient(AccountId.fromString("0.0.1"), PrivateKey.generate(), "TESTNET", "hedera", "");
    }

    /**
     * Creates a client for the auction's auction account
     *
     * @param auction the auction to create the client for
     * @param operatorKey a private key for the account
     * @return Client a client to the Hedera network
     * @throws Exception in the event of an error
     */
    public Client auctionClient(Auction auction, PrivateKey operatorKey) throws Exception {
        Client newClient = clientForNetwork(this.network);
        newClient.setOperator(AccountId.fromString(auction.getAuctionaccountid()), operatorKey);

        return newClient;
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

    /**
     * Sets up a client for the given network
     *
     * @param network the network to use
     * @return Client a client for Hedera
     * @throws Exception in the event of an error
     */
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
                log.error("Unknown network {}", network);
                throw new Exception("Unknown network ".concat(network));
        }

        client.setOperator(this.operatorId, this.operatorKey);

        return client;
    }
}
