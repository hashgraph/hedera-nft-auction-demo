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
import io.vertx.json.schema.common.dsl.NumberSchemaBuilder;
import io.vertx.json.schema.common.dsl.StringSchemaBuilder;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.codec.binary.Hex;
import org.jooq.tools.StringUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
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

import static io.vertx.json.schema.common.dsl.Keywords.maxLength;
import static io.vertx.json.schema.common.dsl.Schemas.numberSchema;
import static io.vertx.json.schema.common.dsl.Schemas.stringSchema;
import static io.vertx.json.schema.draft7.dsl.Keywords.maximum;
import static io.vertx.json.schema.draft7.dsl.Keywords.minimum;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Utilities used throughout the application
 */

@Log4j2
public class Utils {

    /**
     * Web client for use to make REST API calls
     */
    private static final WebClientOptions webClientOptions = new WebClientOptions()
            .setUserAgent("HederaAuction/1.0")
            .setKeepAlive(false);
    private static final WebClient webClient = WebClient.create(Vertx.vertx(), webClientOptions);

    public static final StringSchemaBuilder LONG_STRING_MAX_SCHEMA = stringSchema().with(maxLength(65535));
    public static final StringSchemaBuilder KEY_STRING_MAX_SCHEMA = stringSchema().with(maxLength(88));
    public static final StringSchemaBuilder HEDERA_STRING_MAX_SCHEMA = stringSchema().with(maxLength(100));
    public static final StringSchemaBuilder OPERATION_STRING_SCHEMA = stringSchema().with(maxLength(6));
    public static final StringSchemaBuilder SHORT_STRING_SCHEMA = stringSchema().with(maxLength(20));
    public static final NumberSchemaBuilder LONG_NUMBER_SCHEMA = numberSchema().with(minimum(0)).with(maximum(Long.MAX_VALUE));

    public enum ScheduledStatus {
        EXECUTED,
        NOT_EXECUTED,
        UNKNOWN
    }

    private Utils() {
    }

    /**
     * Reads a file and returns a string containing the file contents
     *
     * @param filePath the path to the file
     * @return String the contents of the file in a string
     */
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

    /**
     * Given a transaction id, return a modified string which is compatible with the mirror REST API
     *
     * @param transactionId the transaction id to transform
     * @return String the transformed transaction id
     */
    public static String hederaMirrorTransactionId(String transactionId) {
        @Var String mirrorTransactionId = transactionId.replace("@",".");
        mirrorTransactionId = mirrorTransactionId.replace(".", "-");
        mirrorTransactionId = mirrorTransactionId.replace("0-0-", "0.0.");
        return mirrorTransactionId;
    }

    /**
     * Given Base64 data, return a string
     *
     * @param base64 the base64 data to decode
     * @return String the decoded base64
     */
    public static String base64toString(String base64) {
        byte[] bytes = Base64.getDecoder().decode(base64);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    /**
     * Given Base64 data, return a string encoded in Hex
     *
     * @param base64 the base64 string to decode
     * @return String the decoded base64
     */
    public static String base64toStringHex(String base64) {
        byte[] bytes = Base64.getDecoder().decode(base64);
        return Hex.encodeHexString(bytes);
    }

    /**
     * Encode a string to base64
     *
     * @param stringToConvert the string to encode to base64
     * @return String containing base64 encoded data
     */
    public static String stringToBase64(String stringToConvert) {
        String bytes = Base64.getEncoder().encodeToString(stringToConvert.getBytes(UTF_8));

        return bytes;
    }

    /**
     * Adds a number of seconds to a timestamp expressed as a string
     *
     * @param timestamp the timestamp to add to
     * @param secondstoAdd the number of seconds to add to the timestamp
     * @return String the provided timestamp plus the specified number of seconds
     */
    public static String addToTimestamp(String timestamp, long secondstoAdd) {
        List<String> timeStampParts = Splitter.on('.').splitToList(timestamp);
        long seconds = Long.parseLong(timeStampParts.get(0)) + secondstoAdd;
        return String.valueOf(seconds).concat(".").concat(timeStampParts.get(1));
    }

    /**
     * Converts a timestamp expressed as a string to a printable date string
     * @param timestamp the timestamp to convert
     * @return String formatted date from the timestamp
     */
    public static String timestampToDate(String timestamp) {
        List<String> timeStampParts = Splitter.on('.').splitToList(timestamp);
        long seconds = Long.parseLong(timeStampParts.get(0));
        Instant instant = Instant.ofEpochSecond(seconds);
        return instant.toString().concat(" (UTC)");
    }

    /**
     * Converts a string timestamp to an Instant
     *
     * @param timestamp the timestamp to convert
     * @return Instant the timestamp converted to an Instant
     */
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

    /**
     * Given a link from a mirror node response, return the timestamp part of the link
     *
     * @param link the link to analyze
     * @return String containing the timestamp part of the link
     */
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

    /**
     * Pauses a thread for a give number of milliseconds
     *
     * @param milliseconds the number of milliseconds to sleep
     */
    public static void sleep(int milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            log.error(e, e);
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Generic method for calling a mirror node REST api
     *
     * @param hederaClient the HederaClient
     * @param url the url of the mirror node to query
     * @param queryParameters the map of parameters to supply to the query
     * @return JsonObject containing the response from the mirror node
     */
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

    /**
     * Method to query a mirror node for the latest consensus timestamp
     *
     * @param hederaClient the HederaClient
     * @return String containing the consensus timestamp of the very last transaction known to the mirror node
     */
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

    /**
     * Queries a mirror node to check if a particular ScheduleId has executed
     * First checks the schedule is more than 40 minutes old, otherwise it may still execute (schedules last 30 minutes max)
     * Then, if the schedule's executedTimestamp is empty, it means the schedule has not executed
     *
     * @param hederaClient the HederaClient
     * @param scheduleId the ScheduleId to query against
     * @param lastMirrorTimeStamp the last timestamp from the mirror node in seconds since epoch (latest transaction timestamp)
     * @return ScheduledStatus determining if the schedule executed
     */
    public static ScheduledStatus scheduleHasExecuted(HederaClient hederaClient, String scheduleId, long lastMirrorTimeStamp) {
        long FOURTY_MINUTES = 40 * 60;
        @Var ScheduledStatus scheduledStatus = ScheduledStatus.UNKNOWN;
        String uri = "/api/v1/schedules/".concat(scheduleId);
        ExecutorService executor = Executors.newFixedThreadPool(1);
        Map<String, String> queryParameters = new HashMap<>();
        Future<JsonObject> future = executor.submit(Utils.queryMirror(hederaClient, uri, queryParameters));
        try {
            JsonObject response = future.get();
            if (response != null) {
                MirrorSchedule mirrorSchedule = response.mapTo(MirrorSchedule.class);

                List<String> timeStampParts = Splitter.on('.').splitToList(mirrorSchedule.consensusTimestamp);
                if (timeStampParts.size() > 0) {
                    // get seconds since epoch
                    long scheduleStart = Long.parseLong(timeStampParts.get(0));
                    if (lastMirrorTimeStamp - scheduleStart > FOURTY_MINUTES) {
                        if (StringUtils.isEmpty(mirrorSchedule.executedTimestamp)) {
                            scheduledStatus = ScheduledStatus.NOT_EXECUTED;
                        } else {
                            scheduledStatus = ScheduledStatus.EXECUTED;
                        }
                    }
                    log.debug("schedule {} contains {} signatures", scheduleId, mirrorSchedule.getSignatureCount());
                } else {
                    log.error("schedule consensus timestamp {} cannot be decoded to seconds.nanos", mirrorSchedule.consensusTimestamp);
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
        log.debug("schedule {} has executed is {}", scheduleId, scheduledStatus);
        return scheduledStatus;
    }

    /**
     * Validates a string is valid base64
     *
     * @param base64String the base64 to validate
     * @return true of false depending on validation result
     */
    public static boolean isBase64(String base64String) {
        String regex = "^([A-Za-z0-9+/]{4})*([A-Za-z0-9+/]{4}|[A-Za-z0-9+/]{3}=|[A-Za-z0-9+/]{2}==)$";
        return base64String.matches(regex);
    }

    /**
     * verifies a file doesn't include a path
     *
     * @param fileToCheck the name of the file to verify
     * @return true if the file is "just a file"
     */
    public static boolean fileIsAFile(String fileToCheck) {
        Path tempPath = Path.of(fileToCheck);
        return fileToCheck.equals(tempPath.getFileName().toString());
    }
}
