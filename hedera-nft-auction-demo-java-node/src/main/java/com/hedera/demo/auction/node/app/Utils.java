package com.hedera.demo.auction.node.app;

import com.google.common.base.Splitter;
import com.google.errorprone.annotations.Var;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.codec.binary.Hex;
import org.jooq.tools.StringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Base64;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;

@Log4j2
public class Utils {

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
            log.error(e);
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
        Long seconds = Long.parseLong(timeStampParts.get(0)) + secondstoAdd;
        return String.valueOf(seconds).concat(".").concat(timeStampParts.get(1));
    }

    public static String instantToTimestamp(Instant timestamp) {
        long seconds = timestamp.getEpochSecond();
        long nanos = timestamp.getNano();

        String secondString = String.valueOf(seconds);
        String nanoString = StringUtils.leftPad(String.valueOf(nanos), 9, "0");
        String consensusTimestamp = secondString.concat(".").concat(nanoString);

        return consensusTimestamp;
    }
}
