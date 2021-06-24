package com.hedera.demo.auction.app;

import com.google.common.base.Splitter;
import com.google.errorprone.annotations.Var;
import com.hedera.demo.auction.app.mirrormapping.MirrorSchedule;
import com.hedera.demo.auction.app.mirrormapping.MirrorTransactions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.ext.web.codec.BodyCodec;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.codec.binary.Hex;
import org.jooq.tools.StringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static java.nio.charset.StandardCharsets.UTF_8;

@Log4j2
public class Utils {

    private static final WebClientOptions webClientOptions = new WebClientOptions()
            .setUserAgent("HederaAuction/1.0")
            .setKeepAlive(false);
    private static final WebClient webClient = WebClient.create(Vertx.vertx(), webClientOptions);

    private Utils() {
    }

    public static String readFileIntoString(String filePath)
    {
        @Var String content = "";
        try
        {
            content = new String ( Files.readAllBytes( Paths.get(filePath)), UTF_8);
        }
        catch (IOException e)
        {
            log.error(e, e);
        }

        return content;
    }

    public static String hederaMirrorTransactionId(String transactionId) {
        @Var String mirrorTransactionId = transactionId.replace("@",".");
        mirrorTransactionId = mirrorTransactionId.replace(".", "-");
        mirrorTransactionId = mirrorTransactionId.replace("0-0-", "0.0.");
        return mirrorTransactionId;
    }

    public static String base64toString(String base64) {
        byte[] bytes = Base64.getDecoder().decode(base64);
        return Hex.encodeHexString(bytes);

    }

    public static String stringToBase64(String stringToConvert) {
        String bytes = Base64.getEncoder().encodeToString(stringToConvert.getBytes(UTF_8));

        return bytes;
    }

    public static String addToTimestamp(String timestamp, long secondstoAdd) {
        List<String> timeStampParts = Splitter.on('.').splitToList(timestamp);
        long seconds = Long.parseLong(timeStampParts.get(0)) + secondstoAdd;
        return String.valueOf(seconds).concat(".").concat(timeStampParts.get(1));
    }

    public static String timestampToDate(String timestamp) {
        List<String> timeStampParts = Splitter.on('.').splitToList(timestamp);
        long seconds = Long.parseLong(timeStampParts.get(0));
        Instant instant = Instant.ofEpochSecond(seconds);
        return instant.toString().concat(" (UTC)");
    }

    public static Instant timestampToInstant(String timestamp) {
        List<String> timeStampParts = Splitter.on('.').splitToList(timestamp);
        long seconds = Long.parseLong(timeStampParts.get(0));
        @Var int nanos = 0;
        if (timeStampParts.size() > 1) {
            nanos = Integer.parseInt(timeStampParts.get(1));
        }
        Instant instant = Instant.ofEpochSecond(seconds, nanos);
        return instant;
    }

    public static String getTimestampFromMirrorLink(String link) {
        if (! StringUtils.isEmpty(link)) {
            // extract the timestamp from the link
            // e.g. api/v1/transactions?transactiontype=CRYPTOTRANSFER&order=asc&timestamp=gt:1598576703.187899009
            // or api/v1/transactions?transactiontype=CRYPTOTRANSFER&order=asc&timestamp=lt:1598576703.187899009

            List<String> linkData = Arrays.asList(link.split("&"));
            for (String linkToInspect : linkData) {
                if (linkToInspect.startsWith("timestamp=")) {
                    String linkToReturn = linkToInspect.replace("timestamp=gt:", "");
                    return  linkToReturn.replace("timestamp=lt:", "");
                }
            }
        }
        return "";
    }

    public static void sleep(int milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            log.error(e, e);
            Thread.currentThread().interrupt();
        }
    }

    public static Callable<JsonObject> queryMirror(HederaClient hederaClient, String url, Map<String, String> queryParameters) {
        String mirrorURL = hederaClient.mirrorUrl();

        return () -> {
            var webQuery = webClient
                    .get(80, mirrorURL, url);

            for (Map.Entry<String, String> entry : queryParameters.entrySet()) {
                webQuery.addQueryParam(entry.getKey(), entry.getValue());
            }

            CompletableFuture<JsonObject> future = new CompletableFuture<>();

            webQuery.as(BodyCodec.jsonObject())
                    .send()
                    .onSuccess(response -> {
                        try {
                            future.complete(response.body());
                        } catch (RuntimeException e) {
                            log.error(e, e);
                            future.complete(new JsonObject());
                        }
                    })
                    .onFailure(err -> {
                        log.error(err.getMessage());
                        future.complete(new JsonObject());
                    });
            return future.get();
        };
    }

    public static String getLastConsensusTimeFromMirror(HederaClient hederaClient) {
        @Var String lastTimestamp = "";
        String uri = "/api/v1/transactions";
        ExecutorService executor = Executors.newFixedThreadPool(1);
        Map<String, String> queryParameters = new HashMap<>();
        queryParameters.put("limit", "1");
        Future<JsonObject> future = executor.submit(Utils.queryMirror(hederaClient, uri, queryParameters));
        try {
            JsonObject response = future.get();
            if (response != null) {
                MirrorTransactions mirrorTransactions = response.mapTo(MirrorTransactions.class);
                if (mirrorTransactions.transactions != null) {
                    if (mirrorTransactions.transactions.size() > 0) {
                        lastTimestamp = mirrorTransactions.transactions.get(0).consensusTimestamp;
                    }
                }
            }
        } catch (InterruptedException e) {
            log.error(e, e);
            Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
            log.error(e, e);
        } finally {
            executor.shutdown();
        }
        return lastTimestamp;
    }

    public static boolean scheduleHasExecuted(HederaClient hederaClient, String scheduleId) {
        @Var boolean hasExecuted = false;
        String uri = "/api/v1/schedules/".concat(scheduleId);
        ExecutorService executor = Executors.newFixedThreadPool(1);
        Map<String, String> queryParameters = new HashMap<>();
        Future<JsonObject> future = executor.submit(Utils.queryMirror(hederaClient, uri, queryParameters));
        try {
            JsonObject response = future.get();
            if (response != null) {
                MirrorSchedule mirrorSchedule = response.mapTo(MirrorSchedule.class);
                log.debug("schedule {} contains {} signatures", scheduleId, mirrorSchedule.getSignatureCount());
                hasExecuted = ! StringUtils.isEmpty(mirrorSchedule.executedTimestamp);
            }
        } catch (InterruptedException e) {
            log.error(e, e);
            Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
            log.error(e, e);
        } finally {
            executor.shutdown();
        }
        log.debug("schedule {} has executed is ", scheduleId, hasExecuted ? "true" : "false");
        return hasExecuted;
    }
}
