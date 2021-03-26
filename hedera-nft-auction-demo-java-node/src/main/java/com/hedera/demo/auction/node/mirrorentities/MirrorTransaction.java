package com.hedera.demo.auction.node.mirrorentities;

import com.google.errorprone.annotations.Var;
import com.hedera.demo.auction.node.app.HederaClient;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.codec.binary.Hex;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Log4j2
public class MirrorTransaction {

    public final String id;
    public final String hash;
    public final String payer;
    public final String status;
    public final String consensusTimestamp;
    public final String memo;
    public final long bidAmount;

    private final String mirrorProvider = HederaClient.getMirrorProvider();

    public MirrorTransaction (JsonObject transaction, String auctionAccountId) throws Exception {
        switch (mirrorProvider) {
            case "HEDERA":
                id = transaction.getString("transaction_id");
                payer = id.substring(0, id.indexOf("-"));
                byte[] txHashBytes = Base64.getDecoder().decode(transaction.getString("transaction_hash"));
                hash = Hex.encodeHexString(txHashBytes);
                status = transaction.getString("result");
                consensusTimestamp = transaction.getString("consensus_timestamp");
                byte[] transactionMemoBytes = Base64.getDecoder().decode(transaction.getString("memo_base64"));
                memo = new String(transactionMemoBytes, StandardCharsets.UTF_8);

                @Var long amount = 0;
                // find payment amount
                JsonArray transfers = transaction.getJsonArray("transfers");
                // get the bid value which is the payment amount to the auction account
                for (Object transferObject : transfers) {
                    JsonObject transfer = JsonObject.mapFrom(transferObject);
                    if (transfer.getString("account").equals(auctionAccountId)) {
                        amount = transfer.getLong("amount");
                        log.debug("Bid amount is " + amount);
                        break;
                    }
                }
                bidAmount = amount;

                break;
            case "DRAGONGLASS":
            case "KABUTO":
                id = "";
                hash = "";
                payer = "";
                status = "";
                consensusTimestamp = "";
                memo = "";
                bidAmount = 0;

                break;

            default:
                throw new Exception("Invalid mirror operator");

        }
    }
}
