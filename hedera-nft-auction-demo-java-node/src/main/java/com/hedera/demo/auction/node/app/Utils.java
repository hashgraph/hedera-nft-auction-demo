package com.hedera.demo.auction.node.app;

import com.google.errorprone.annotations.Var;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.codec.binary.Hex;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;

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
}
