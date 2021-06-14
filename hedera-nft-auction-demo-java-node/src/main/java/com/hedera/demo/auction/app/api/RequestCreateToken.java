package com.hedera.demo.auction.app.api;

import com.google.errorprone.annotations.Var;
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
import java.nio.file.Paths;
import java.util.Base64;

@SuppressWarnings("unused")
@Log4j2
public class RequestCreateToken {

    public String name = "Token";
    public String symbol = "TT";
    public long initialSupply = 1;
    public int decimals = 0;
    public String memo = "";
    public RequestCreateTokenMetaData description = new RequestCreateTokenMetaData();
    public RequestCreateTokenMetaData image = new RequestCreateTokenMetaData();
    public RequestCreateTokenMetaData certificate = new RequestCreateTokenMetaData();

    public boolean hasDescription() {
        return !StringUtils.isEmpty(description.description);
    }
    public boolean hasImage() {
        return !StringUtils.isEmpty(image.description);
    }
    public boolean hasCertificate() {
        return !StringUtils.isEmpty(certificate.description);
    }
    public boolean hasMetaData() {
        return hasDescription() || hasImage() || hasCertificate();
    }

    public void saveImagesToIPFS(String nftStorageKey) throws Exception {
        // does the metadata contain an image
        saveImageToIPFS(nftStorageKey, this.image);
        saveImageToIPFS(nftStorageKey, this.certificate);
    }

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
                    log.error("saving to IPFS failed ".concat(response.body()));
                    return "";
                }
            } catch (RuntimeException e) {
                log.error(e, e);
                return "";
            }
        } else {
            log.error("saving to IPFS failed status code=".concat(String.valueOf(response.statusCode())).concat(" ").concat(response.body()));
            return "";
        }
    }

    private static void saveImageToIPFS(String nftStorageKey, RequestCreateTokenMetaData requestCreateTokenMetaData) throws Exception {
        if (!StringUtils.isEmpty(requestCreateTokenMetaData.type)) {
            @Var byte[] imageBytes = new byte[0];
            if ("base64".equals(requestCreateTokenMetaData.type)) {
                imageBytes = Base64.getDecoder().decode(requestCreateTokenMetaData.description);
            } else if ("file".equals(requestCreateTokenMetaData.type)) {
                if (Files.exists(Path.of(requestCreateTokenMetaData.description))) {
                    imageBytes = Files.readAllBytes(Paths.get(requestCreateTokenMetaData.description));
                }
            }

            if (imageBytes.length > 0) {
                // push bytes to IPFS
                String response = saveBinaryToIPFS(nftStorageKey, imageBytes);

                // update the metadata
                if (response.contains("ipfs")) {
                    requestCreateTokenMetaData.description = response;
                    requestCreateTokenMetaData.type = "string";
                } else {
                    log.error("an error occurred, response doesn't contain ipfs");
                    throw new Exception("response doesn't contain ipfs");
                }
            }
        }
    }
}
