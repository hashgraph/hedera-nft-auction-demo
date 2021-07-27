package com.hedera.demo.auction.app.api;

import com.google.errorprone.annotations.Var;
import com.hedera.demo.auction.app.Utils;
import io.vertx.core.json.JsonObject;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.validator.routines.UrlValidator;
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
import java.util.Base64;

/**
 * Data class to map incoming REST JSON to a java object
 */
@SuppressWarnings("unused")
@Log4j2
public class RequestCreateToken {

    private String name = "Token";
    private String symbol = "TT";
    public long initialSupply = 1;
    public int decimals = 0;
    private String memo = "";
    public RequestCreateTokenMetaData description = new RequestCreateTokenMetaData();
    public RequestCreateTokenMetaData image = new RequestCreateTokenMetaData();
    public RequestCreateTokenMetaData certificate = new RequestCreateTokenMetaData();

    public void setName(String name) {
        this.name = Utils.normalize(name);
    }
    public String getName() {
        return this.name;
    }

    public void setSymbol(String symbol) {
        this.symbol = Utils.normalize(symbol);
    }
    public String getSymbol() {
        return this.symbol;
    }

    public void setMemo(String memo) {
        this.memo = Utils.normalize(memo);
    }
    public String getMemo() {
        return this.memo;
    }

    public boolean hasDescription() {
        return !StringUtils.isEmpty(description.getDescription());
    }
    public boolean hasImage() {
        return !StringUtils.isEmpty(image.getDescription());
    }
    public boolean hasCertificate() {
        return !StringUtils.isEmpty(certificate.getDescription());
    }
    public boolean hasMetaData() {
        return hasDescription() || hasImage() || hasCertificate();
    }

    public void saveImagesToIPFS(String nftStorageKey, String filesPath) throws Exception {
        // does the metadata contain an image
        saveImageToIPFS(nftStorageKey, this.image, filesPath);
        try {
            saveImageToIPFS(nftStorageKey, this.certificate, filesPath);
        } catch (Exception e) {
            // attempt to delete first image
            String cidToDelete = image.getDescription().replace("https://cloudflare-ipfs.com/ipfs/", "");
            if (! StringUtils.isEmpty(cidToDelete)) {
                removeImageFromIPFS(nftStorageKey, cidToDelete);
            }
        }
    }

    /**
     * Removes binary data from IPFS using fileCoin
     *
     * @param nftStorageKey the fileCoin authentication key
     * @param cid the cid to remove
     *
     * @throws IOException in the event of an error
     * @throws InterruptedException in the event of an error
     */
    private static void removeImageFromIPFS(String nftStorageKey, String cid) throws IOException, InterruptedException {

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.nft.storage/".concat(cid)))
                .header("Content-Type", "text/plain;charset=UTF-8")
                .header("Authorization", "Bearer ".concat(nftStorageKey))
                .DELETE()
                .build();

        HttpClient client = HttpClient.newHttpClient();
        HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
        if (response.statusCode() == 200) {
            try {
                JsonObject body = new JsonObject(response.body());
                if (! "true".equals(body.getString("ok"))) {
                    log.error("removing from IPFS failed {}", response.body());
                }
            } catch (RuntimeException e) {
                log.error(e, e);
            }
        } else {
            log.error("removing from IPFS failed status code={} response body={}", response.statusCode(), response.body());
        }
    }

    /**
     * Stores binary data to IPFS using fileCoin
     *
     * @param nftStorageKey the fileCoin authentication key
     * @param data the data to store
     *
     * @return String reference to the stored data on fileCoin
     * @throws IOException in the event of an error
     * @throws InterruptedException in the event of an error
     */
    private static String saveBinaryToIPFS(String nftStorageKey, byte[] data) throws IOException, InterruptedException {

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.nft.storage/upload"))
                .header("Content-Type", "text/plain;charset=UTF-8")
                .header("Authorization", "Bearer ".concat(nftStorageKey))
                .POST(BodyPublishers.ofByteArray(data))
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
     * Given metadata about the token, optionally store it on IPFS using fileCoin
     *
     * @param nftStorageKey the authentication key for fileCoin
     * @param requestCreateTokenMetaData the metdata to store
     * @throws Exception in the event of an error
     */
    private static void saveImageToIPFS(String nftStorageKey, RequestCreateTokenMetaData requestCreateTokenMetaData, String filesPath) throws Exception {
        if (!StringUtils.isEmpty(requestCreateTokenMetaData.type)) {
            @Var byte[] imageBytes = new byte[0];
            if ("base64".equals(requestCreateTokenMetaData.type)) {
                imageBytes = Base64.getDecoder().decode(requestCreateTokenMetaData.getDescription());
            } else if ("file".equals(requestCreateTokenMetaData.type)) {
                Path thisFile = Path.of(filesPath, requestCreateTokenMetaData.getDescription());
                if (Files.exists(thisFile)) {
                    imageBytes = Files.readAllBytes(thisFile);
                }
            }

            if (imageBytes.length > 0) {
                // push bytes to IPFS
                String response = saveBinaryToIPFS(nftStorageKey, imageBytes);

                // update the metadata
                if (response.contains("ipfs")) {
                    requestCreateTokenMetaData.setDescription(response);
                    requestCreateTokenMetaData.type = "string";
                } else {
                    log.error("an error occurred, response doesn't contain ipfs");
                    throw new Exception("response doesn't contain ipfs");
                }
            }
        }
    }

    public void checkIsValid(String filesPath) throws Exception {
        String response = "";
        String[] schemes = {"http","https"};
        UrlValidator urlValidator = new UrlValidator(schemes);
        if (hasDescription()) {
            if (! "string".equals(description.type)) {
                throw new Exception("description type should be string");
            }
        }
        if (hasCertificate()) {
            if ("base64".equals(certificate.type)) {
                if (!Utils.isBase64(certificate.getDescription())) {
                    throw new Exception("certificate.getDescription() is not valid base64");
                }
            } else if ("file".equals(certificate.type)) {
                if (StringUtils.isEmpty(certificate.getDescription())) {
                    throw new Exception("certificate.getDescription() does not contain a file name");
                }

                if (! Utils.fileIsAFile(certificate.getDescription())) {
                    throw new Exception("certificate.getDescription() contains a path element");
                }

                Path thisFile = Path.of(filesPath, certificate.getDescription());
                if (!Files.exists(thisFile)) {
                    throw new Exception("certificate.getDescription() does not exist or is not accessible");
                }
            } else if ("url".equals(certificate.type)) {
                if (! urlValidator.isValid(certificate.getDescription())) {
                    throw new Exception("certificate.getDescription() is not a valid url");
                }
            } else {
                throw new Exception("certificate.type is not valid, expecting string, base64 or file");
            }
        }

        if (hasImage()) {
            if ("base64".equals(image.type)) {
                if (! Utils.isBase64(image.getDescription())) {
                    throw new Exception("image.getDescription() is not valid base64");
                }
            } else if ("file".equals(image.type)) {
                if (StringUtils.isEmpty(image.getDescription())) {
                    throw new Exception("image.getDescription() does not contain a file name");
                }
                if (! Utils.fileIsAFile(image.getDescription())) {
                    throw new Exception("image.getDescription() contains a path element");
                }

                Path thisFile = Path.of(filesPath, image.getDescription());
                if (! Files.exists(thisFile)) {
                    throw new Exception("image.getDescription() does not exist or is not accessible");
                }
            } else if ("string".equals(image.type)) {
                if (! urlValidator.isValid(image.getDescription())) {
                    throw new Exception("image.getDescription() is not a valid url");
                }
            } else {
                throw new Exception("image.type is not valid, expecting string, base64 or file");
            }
        }
    }
}
