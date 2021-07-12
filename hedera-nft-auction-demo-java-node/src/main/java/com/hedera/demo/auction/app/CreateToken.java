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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

@Log4j2
public class CreateToken extends AbstractCreate {

    public CreateToken() throws Exception {
        super();
    }

    /**
     * Creates a simple token (no kyc, freeze, supply, etc...)
     * @param tokenSpec Json Describing the token to create or name of a file containing the json
     * @throws Exception in the event of an exception
     */
    public TokenId create(String tokenSpec) throws Exception {

        String filesPath = Utils.filesPath(env);
        Path filePath = Path.of(filesPath, tokenSpec);

        RequestCreateToken tokenData;
        if (Files.exists(filePath)) {
            // the spec is a valid file name, let's load it
            String contents = Utils.readFileIntoString(filesPath);
            JsonObject contentsJson = new JsonObject(contents);
            tokenData = contentsJson.mapTo(RequestCreateToken.class);
        } else {
            JsonObject contentsJson = new JsonObject(tokenSpec);
            tokenData = contentsJson.mapTo(RequestCreateToken.class);
        }
        tokenData.checkIsValid(filesPath);

        if (tokenData.hasMetaData()) {
            String nftStorageKey = Optional.ofNullable(env.get("NFT_STORAGE_API_KEY")).orElse("");
            if (StringUtils.isEmpty(nftStorageKey)) {
                log.error("empty NFT_STORAGE_API_KEY, unable to store metadata");
                throw new Exception("Empty NFT_STORAGE_API_KEY, unable to store metadata");
            }
            tokenData.saveImagesToIPFS(nftStorageKey, filesPath);

            // store the token Data on IPFS
            String response = storeTokenOnIPFS(nftStorageKey, JsonObject.mapFrom(tokenData));

            if (response.contains("ipfs")) {
                // create the token
                return createToken(tokenData.name, response, tokenData.initialSupply, tokenData.decimals, tokenData.memo);
            } else {
                throw new Exception(response);
            }
        } else {
            return createToken(tokenData.name, tokenData.symbol, tokenData.initialSupply, tokenData.decimals, tokenData.memo);
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
     * @param name the name of the token
     * @param symbol the symbol for the token
     * @param initialSupply the token's initial supply
     * @param decimals the number of decimals for the token
     * @param memo the memo for the token
     * @return TokenId the unique identifier for the token
     * @throws Exception in the event of an error
     */
    private TokenId createToken(String name, String symbol, long initialSupply, int decimals, String memo) throws Exception {

        try {
            Client client = hederaClient.client();
            client.setMaxTransactionFee(Hbar.from(100));

            TokenCreateTransaction tokenCreateTransaction = new TokenCreateTransaction();
            tokenCreateTransaction.setTokenName(name);
            tokenCreateTransaction.setTokenSymbol(symbol);
            tokenCreateTransaction.setInitialSupply(initialSupply);
            tokenCreateTransaction.setDecimals(decimals);
            tokenCreateTransaction.setTreasuryAccountId(hederaClient.operatorId());
            tokenCreateTransaction.setTokenMemo(memo);

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

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            log.error("Invalid number of arguments supplied - should be one");
        } else {
            log.info("Creating token");
            CreateToken createToken = new CreateToken();
            createToken.create(args[0]);
        }
    }
}
