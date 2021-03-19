package com.hedera.demo.auction.node.app.auctionwatchers;

import com.google.errorprone.annotations.Var;
import com.hedera.demo.auction.node.app.HederaClient;
import com.hedera.demo.auction.node.app.domain.Auction;
import com.hedera.demo.auction.node.app.domain.Bid;
import com.hedera.demo.auction.node.app.refunder.Refunder;
import com.hedera.demo.auction.node.app.repository.AuctionsRepository;
import com.hedera.demo.auction.node.app.repository.BidsRepository;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.codec.BodyCodec;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.codec.binary.Hex;

import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@Log4j2
public class BidsWatcher implements Runnable {

    private Auction auction;
    private final WebClient webClient;
    private final BidsRepository bidsRepository;
    private final AuctionsRepository auctionsRepository;
    private final String refundKey;
    private final int mirrorQueryFrequency;
    private String mirrorURL = "";
    private final String mirrorProvider = HederaClient.getMirrorProvider();

    public BidsWatcher(WebClient webClient, AuctionsRepository auctionsRepository, BidsRepository bidsRepository, Auction auction, String refundKey, int mirrorQueryFrequency) throws Exception {
        this.webClient = webClient;
        this.bidsRepository = bidsRepository;
        this.auctionsRepository = auctionsRepository;
        this.auction = auction;
        this.refundKey = refundKey;
        this.mirrorQueryFrequency = mirrorQueryFrequency;
        this.mirrorURL = HederaClient.getMirrorUrl();
    }

    @Override
    public void run() {

        log.info("Watching auction account Id " + auction.getAuctionaccountid() + ", token Id " + auction.getTokenid());

        AtomicBoolean querying = new AtomicBoolean(false);
        AtomicReference<String> uri = new AtomicReference<>("");

        switch (mirrorProvider) {
            case "HEDERA":
                uri.set("/api/v1/transactions");
                break;
            case "DRAGONGLASS":
                //TODO: Handle dragonglass mirror
                uri.set("");
                break;
            default:
                //TODO: Handle kabuto mirror
                uri.set("");
                break;
        }

        while (true) {
            if (!querying.get()) {
                querying.set(true);

                log.debug("Checking for bids on account " + auction.getAuctionaccountid() + " and token " + auction.getTokenid());

                if (mirrorProvider.equals("HEDERA")) {
                    var webQuery =
                    webClient
                            .get(443, this.mirrorURL, uri.get())
                            .ssl(true)
                            .addQueryParam("account.id", auction.getAuctionaccountid())
                            .addQueryParam("transactiontype", "CRYPTOTRANSFER")
                            .addQueryParam("order", "asc");

                            if (auction.getLastconsensustimestamp() != null) {
                                webQuery.addQueryParam("timestamp", "gt:".concat(auction.getLastconsensustimestamp()));
                            }

                            webQuery.as(BodyCodec.jsonObject())
                            .send(response -> {
                                if (response.succeeded()) {
                                    JsonObject body = response.result().body();
                                    try {
                                        handleHederaResponse(body);
                                    } catch (RuntimeException e) {
                                        log.error(e);
                                    } finally {
                                        querying.set(false);
                                    }
                                } else {
                                    log.error(response.cause().getMessage());
                                    querying.set(false);
                                }
                            });
                } else if (mirrorProvider.equals("KABUTO")) {
                    //TODO: Handle kabuto mirror
                } else if (mirrorProvider.equals("DRAGONGLASS")) {
                    //TODO: Handle dragonglass mirror
                }
            }
            try {
                Thread.sleep(this.mirrorQueryFrequency);
            } catch (InterruptedException e) {
                e.printStackTrace();
                log.error(e);
            }
        }
    }

    private void handleHederaResponse(JsonObject response) {
        try {
            JsonArray transactions = response.getJsonArray("transactions");
            for (Object transactionObject : transactions) {
                JsonObject transaction = JsonObject.mapFrom(transactionObject);
                handleHederaTransaction(transaction);
                this.auction.setLastconsensustimestamp(transaction.getString("consensus_timestamp"));
                auctionsRepository.save(this.auction);
            }
        } catch (Exception e) {
            log.error(e);
        }
    }

    private void handleHederaTransaction (JsonObject transaction) throws SQLException {
        @Var String rejectReason = "";
        @Var boolean refund = false;
        @Var long bidAmount = 0;
        String transactionId = transaction.getString("transaction_id");
        byte[] txHashBytes = Base64.getDecoder().decode(transaction.getString("transaction_hash"));
        String transactionHash = Hex.encodeHexString(txHashBytes);
        String transactionPayer = transactionId.substring(0, transactionId.indexOf("-"));
        if (transaction.getString("result").equals("SUCCESS")) {
            String consensusTimestamp = transaction.getString("consensus_timestamp");
            byte[] transactionMemoBytes = Base64.getDecoder().decode(transaction.getString("memo_base64"));
            String transactionMemo = new String(transactionMemoBytes, StandardCharsets.UTF_8);
            //Handle memo on transfer and create to allow for transactions that aren't bids
            if (checkMemos(transactionMemo)) {
                return;
            }

            // check the timestamp to verify if auction should end
            if (consensusTimestamp.compareTo(this.auction.getEndtimestamp()) > 0) {
                // payment past auctions end, close it, but continue processing
                if (!this.auction.isClosed()) {
                    this.auction = auctionsRepository.setClosed(this.auction);
                }
                // find payment amount
                bidAmount = hederaBidAmount(transaction);
                refund = true;
                rejectReason = "Auction is closed";
            }

            if ( ! refund) {
                // check if paying account is different to the current winner (and that of the auction)
                if (transactionPayer.equals(this.auction.getAuctionaccountid())) {
                    log.debug("Skipping auction account refund transaction");
                } else if (transactionPayer.equals(this.auction.getWinningaccount())) {
                    if (! this.auction.getWinnerCanBid()) {
                        // same account as winner, not allowed
                        rejectReason = "Winner can't bid again";
                        refund = true;
                    }
                }
            }

            if ( ! refund) {
                // find payment amount
                bidAmount = hederaBidAmount(transaction);
            }

            long bidDelta = (bidAmount - this.auction.getWinningbid()) / 100000000;
            if ((bidDelta > 0) && (bidDelta < this.auction.getMinimumbid())) {
                rejectReason = "Bid increase too small";
                refund = true;
            }
            if (bidAmount != 0) { // if bid !=0, no refund is expected at this stage
                // we have a bid, check it against bidding rules
                if (bidAmount < this.auction.getReserve()) {
                    rejectReason = "Bid below reserve";
                } else if (bidAmount <= this.auction.getWinningbid()) {
                    rejectReason = "Under bid";
                    refund = true;
                }
            }

            //TODO: update auction and bid in a single tx
            if (rejectReason.isEmpty()) {
                // we have a winner
                // refund previous bid
                if (this.auction.getWinningaccount() != null) {
                    // do not refund the very first bid !!!
                    refundBid(this.auction.getWinningbid(), this.auction.getWinningaccount(), this.auction.getWinningtimestamp());
                    refund = false;
                }
                // update prior winning bid
                Bid priorBid = new Bid();
                priorBid.setTimestamp(this.auction.getWinningtimestamp());
                priorBid.setStatus("Higher bid received");
                bidsRepository.setStatus(priorBid);

                // update the auction
                this.auction.setWinningtimestamp(consensusTimestamp);
                this.auction.setWinningaccount(transactionPayer);
                this.auction.setWinningbid(bidAmount);
                this.auction.setWinningtxid(transactionId);
                this.auction.setWinningtxhash(transactionHash);
            }

            // store the bid
            Bid winningBid = new Bid();
            winningBid.setStatus(rejectReason);
            winningBid.setBidamount(bidAmount);
            winningBid.setAuctionid(this.auction.getId());
            winningBid.setBidderaccountid(transactionPayer);
            winningBid.setTimestamp(consensusTimestamp);
            winningBid.setStatus(rejectReason);
            winningBid.setTransactionid(transactionId);
            winningBid.setTransactionhash(transactionHash);
            bidsRepository.add(winningBid);

            if (refund) {
                // refund this transaction
                refundBid(bidAmount, transactionPayer, consensusTimestamp);
            }

        } else {
            log.debug("Transaction Id " + transactionId + " status not SUCCESS.");
        }
    }

    private long hederaBidAmount(JsonObject transaction) {
        @Var long bidAmount = 0;
        // find payment amount
        JsonArray transfers = transaction.getJsonArray("transfers");
        // get the bid value which is the payment amount to the auction account
        for (Object transferObject : transfers) {
            JsonObject transfer = JsonObject.mapFrom(transferObject);
            if (transfer.getString("account").equals(this.auction.getAuctionaccountid())) {
                bidAmount = transfer.getLong("amount");
                log.debug("Bid amount is " + bidAmount);
                break;
            }
        }
        return bidAmount;
    }

    private void refundBid(long amount, String accountId, String consensusTimestamp) {
        if (this.refundKey.isBlank()) {
            return;
        }
        Thread t = new Thread(new Refunder(bidsRepository, auction.getAuctionaccountid(), amount, accountId, consensusTimestamp, refundKey));
        t.start();
    }

    private static boolean checkMemos(String memo) {
        if (memo.isEmpty()) {
            return false;
        }
        String[] memos = new String[]{"CREATEAUCTION", "FUNDACCOUNT", "TRANSFERTOAUCTION", "ASSOCIATE", "AUCTION REFUND"};
        return Arrays.stream(memos).anyMatch(memo.toUpperCase()::equals);
    }
}
