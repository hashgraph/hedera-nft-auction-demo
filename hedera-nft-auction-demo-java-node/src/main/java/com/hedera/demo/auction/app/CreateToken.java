package com.hedera.demo.auction.app;

import com.hedera.demo.auction.app.api.RequestCreateToken;
import com.hedera.hashgraph.sdk.Client;
import com.hedera.hashgraph.sdk.Hbar;
import com.hedera.hashgraph.sdk.Status;
import com.hedera.hashgraph.sdk.TokenCreateTransaction;
import com.hedera.hashgraph.sdk.TokenId;
import com.hedera.hashgraph.sdk.TransactionReceipt;
import com.hedera.hashgraph.sdk.TransactionResponse;
import io.vertx.core.json.JsonObject;
import lombok.extern.log4j.Log4j2;
import org.jooq.tools.StringUtils;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.Optional;

@Log4j2
public class CreateToken extends AbstractCreate {

    private final String filesPath;

    /**
     * constructor
     * @param filesPath location of token images, etc...
     * @throws Exception in the event of an exception
     */
    public CreateToken(String filesPath) throws Exception {
        super();
        this.filesPath = filesPath;
    }

    /**
     * Creates a simple token (no kyc, freeze, supply, etc...)
     * @param requestCreateToken object describing the token to create
     * @throws Exception in the event of an exception
     */
    public TokenId create(RequestCreateToken requestCreateToken) throws Exception {

        if (requestCreateToken.hasMetaData()) {
            String nftStorageKey = Optional.ofNullable(env.get("NFT_STORAGE_API_KEY")).orElse("");
            if (StringUtils.isEmpty(nftStorageKey)) {
                log.error("empty NFT_STORAGE_API_KEY, unable to store metadata");
                throw new Exception("Empty NFT_STORAGE_API_KEY, unable to store metadata");
            }
            requestCreateToken.saveImagesToIPFS(nftStorageKey, filesPath);

            // store the token Data on IPFS
            String response = storeTokenOnIPFS(nftStorageKey, JsonObject.mapFrom(requestCreateToken));

            if (response.contains("ipfs")) {
                // create the token
                requestCreateToken.setSymbol(response);
                return createToken(requestCreateToken);
            } else {
                throw new Exception("error saving token metadata to ipfs");
            }
        } else {
            return createToken(requestCreateToken);
        }
    }

    /**
     * Stores json data on ipfs via fileCoin
     *
     * @param nftStorageKey the storage key to use to authenticate with fileCoin
     * @param imageJson the json to store
     *
     * @return String the fileCoin reference to the stored file
     * @throws IOException in the event of an exception
     * @throws InterruptedException in the event of an exception
     */
    protected String storeTokenOnIPFS(String nftStorageKey, JsonObject imageJson) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.nft.storage/upload"))
                .header("Content-Type", "text/plain;charset=UTF-8")
                .header("Authorization", "Bearer ".concat(nftStorageKey))
                .POST(BodyPublishers.ofString(imageJson.encode()))
                .build();

        HttpClient client = HttpClient.newHttpClient();
        HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
        if (response.statusCode() == 200) {
            try {
                JsonObject body = new JsonObject(response.body());
                if ("true".equals(body.getString("ok"))) {
                    return "https://cloudflare-ipfs.com/ipfs/".concat(body.getJsonObject("value").getString("cid"));
                } else {
                    log.error("saving to IPFS failed {}", response.body());
                    return "";
                }
            } catch (RuntimeException e) {
                log.error(e, e);
                return "";
            }
        } else {
            log.error("saving to IPFS failed status code={} response body={}", response.statusCode(), response.body());
            return "";
        }
    }

    /**
     * Creates the token on Hedera
     *
     * @param requestCreateToken the object containing the token properties
     * @return TokenId the unique identifier for the token
     * @throws Exception in the event of an error
     */
    private TokenId createToken(RequestCreateToken requestCreateToken) throws Exception {

        try {
            Client client = hederaClient.client();
            client.setMaxTransactionFee(Hbar.from(100));

            TokenCreateTransaction tokenCreateTransaction = new TokenCreateTransaction();
            tokenCreateTransaction.setTokenName(requestCreateToken.getName());
            tokenCreateTransaction.setTokenSymbol(requestCreateToken.getSymbol());
            tokenCreateTransaction.setInitialSupply(requestCreateToken.initialSupply);
            tokenCreateTransaction.setDecimals(requestCreateToken.decimals);
            tokenCreateTransaction.setTreasuryAccountId(hederaClient.operatorId());
            tokenCreateTransaction.setTokenMemo(requestCreateToken.getMemo());

            TransactionResponse response = tokenCreateTransaction.execute(client);

            TransactionReceipt receipt = response.getReceipt(client);
            if (receipt.status != Status.SUCCESS) {
                log.error("Token creation failed {}",receipt.status);
                throw new Exception("Token creation failed ".concat(receipt.status.toString()));
            } else {
                log.info("Token created {}", receipt.tokenId.toString());
            }
            return receipt.tokenId;
        } catch (Exception e) {
            log.error(e, e);
            throw e;
        }
    }
}
