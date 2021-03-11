package com.hedera.demo.auction.node.app.auctionwatchers;

import com.google.errorprone.annotations.Var;
import com.hedera.demo.auction.node.app.domain.Auction;
import com.hedera.demo.auction.node.app.domain.Bid;
import com.hedera.demo.auction.node.app.repository.AuctionsRepository;
import com.hedera.demo.auction.node.app.repository.BidsRepository;
import io.github.cdimascio.dotenv.Dotenv;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.codec.BodyCodec;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;

import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.Base64;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@Log4j2
public class BidsWatcher implements Runnable {

    private Auction auction;
    private final WebClient webClient;
    private final String mirrorToUse;
    private final BidsRepository bidsRepository;
    private final AuctionsRepository auctionsRepository;
    private final String refundKey;
    private final int mirrorQueryFrequency;

    public BidsWatcher(WebClient webClient, AuctionsRepository auctionsRepository, BidsRepository bidsRepository, Auction auction, Dotenv env) {
        this.webClient = webClient;
        this.bidsRepository = bidsRepository;
        this.auctionsRepository = auctionsRepository;
        this.auction = auction;
        this.mirrorToUse = Optional.ofNullable(env.get("MIRROR_NODE")).orElse("");
        this.refundKey = Optional.ofNullable(env.get("REFUND_KEY")).orElse("");
        this.mirrorQueryFrequency = Integer.parseInt(Optional.ofNullable(env.get("MIRROR_QUERY_FREQUENCY")).orElse("5000"));
    }

    @SneakyThrows
    @Override
    public void run() {

        AtomicBoolean querying = new AtomicBoolean(false);

        log.info("Watching auction account Id " + auction.getAuctionaccountid() + ", token Id " + auction.getTokenid());

        AtomicReference<String> uri = new AtomicReference<>("");
        if (mirrorToUse.toUpperCase().contains("HEDERA")) {
            uri.set("/api/v1/transactions");
        } else if (mirrorToUse.toUpperCase().contains("KABUTO")) {
            //TODO: Handle kabuto mirror
        } else if (mirrorToUse.toUpperCase().contains("DRAGONGLASS")) {
            //TODO: Handle dragonglass mirror
        }

        while (true) {
            if (!querying.get()) {
                querying.set(true);

                log.trace("Checking association for account " + auction.getAuctionaccountid() + " and token " + auction.getTokenid());

                if (mirrorToUse.toUpperCase().contains("HEDERA")) {
                    var webQuery =
                    webClient
                            .get(443, mirrorToUse, uri.get())
                            .ssl(true)
                            .addQueryParam("account.id", auction.getAuctionaccountid())
                            .addQueryParam("transactiontype", "CRYPTOTRANSFER")
                            .addQueryParam("order", "desc");

                            if (auction.getLastconsensustimestamp() != null) {
                                webQuery.addQueryParam("timestamp", "gt:".concat(auction.getLastconsensustimestamp()));
                            }

                            webQuery.as(BodyCodec.jsonObject())
                            .send()
                            .onSuccess(response -> {
                                JsonObject body = response.body();
                                try {
                                    handleHederaResponse(body);
                                } catch (Exception e) {
                                    log.error(e);
                                } finally {
                                    querying.set(false);
                                }
                            })
                            .onFailure(e -> {
                                log.error(e);
                                querying.set(false);
                            });
                } else if (mirrorToUse.toUpperCase().contains("KABUTO")) {
                    //TODO: Handle kabuto mirror
                } else if (mirrorToUse.toUpperCase().contains("DRAGONGLASS")) {
                    //TODO: Handle dragonglass mirror
                }
            }
            Thread.sleep(this.mirrorQueryFrequency);
        }
    }

    private void handleHederaResponse(JsonObject response) {
        try {
            JsonArray transactions = response.getJsonArray("transactions");
            for (Object transactionObject : transactions) {
                JsonObject transaction = JsonObject.mapFrom(transactionObject);
                handleHederaTransaction(transaction);
                System.out.println(this.auction.getLastconsensustimestamp());
                this.auction.setLastconsensustimestamp(transaction.getString("consensus_timestamp"));
                System.out.println(this.auction.getLastconsensustimestamp());
                auctionsRepository.save(this.auction);
            }
        } catch (Exception e) {
            log.error(e);
        }
    }

    private void handleHederaTransaction (JsonObject transaction) throws SQLException {
        @Var String rejectReason = "";
        @Var boolean refund = false;
        @Var long bid = 0;
        String transactionId = transaction.getString("transaction_id");
        byte[] txHashBytes = Base64.getDecoder().decode(transaction.getString("transaction_hash"));
        String transactionHash = new String(txHashBytes, StandardCharsets.UTF_8);
        String transactionPayer = transactionId.substring(0, transactionId.indexOf("-"));
        if (transaction.getString("result").equals("SUCCESS")) {
            String consensusTimestamp = transaction.getString("consensus_timestamp");

            //TODO: Handle memo on transfer to allow for account funding which aren't bids
            //TODO: Check bid delta is greater than minimum bid

            // check the timestamp to verify if auction should end
            if (consensusTimestamp.compareTo(this.auction.getEndtimestamp()) > 0) {
                // payment past auctions end, close it, but continue processing
                if (!this.auction.isClosed()) {
                    this.auction = auctionsRepository.setClosed(this.auction);
                }
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
                JsonArray transfers = transaction.getJsonArray("transfers");
                // get the bid value which is the payment amount to the auction account
                for (Object transferObject : transfers) {
                    JsonObject transfer = JsonObject.mapFrom(transferObject);
                    if (transfer.getString("account").equals(this.auction.getAuctionaccountid())) {
                        bid = transfer.getLong("amount");
                        log.debug("Bid amount is " + bid);
                        break;
                    }
                }
            }

            System.out.println(bid);
            System.out.println(this.auction.getWinningbid());
            System.out.println((bid <= this.auction.getWinningbid()));
            if (bid != 0) { // if bid !=0, no refund is expected at this stage
                // we have a bid, check it against bidding rules
                if (bid < this.auction.getReserve()) {
                    rejectReason = "Bid below reserve";
                } else if (bid <= this.auction.getWinningbid()) {
                    rejectReason = "Under bid";
                    refund = true;
                }
            }

            if (refund && ! this.refundKey.isBlank()) {
                // refund this transaction
                refund(bid, transactionPayer);
            }

            //TODO: update auction and bid in a single tx
            if (rejectReason.isEmpty()) {
                // we have a winner
                // refund previous bid
                refund(bid, this.auction.getWinningaccount());

                // update prior winning bid
                Bid priorBid = new Bid();
                priorBid.setTimestamp(this.auction.getWinningtimestamp());
                priorBid.setStatus("Higher bid received");
                bidsRepository.setStatus(priorBid);

                // update the auction
                this.auction.setWinningtimestamp(consensusTimestamp);
                this.auction.setWinningaccount(transactionPayer);
                this.auction.setWinningbid(bid);
                this.auction.setWinningtxid(transactionId);
                this.auction.setWinningtxhash(transactionHash);
            }

            // store the bid
            Bid winningBid = new Bid();
            winningBid.setStatus(rejectReason);
            winningBid.setBidamount(bid);
            winningBid.setAuctionid(this.auction.getId());
            winningBid.setBidderaccountid(transactionPayer);
            winningBid.setTimestamp(consensusTimestamp);
            winningBid.setStatus(rejectReason);
            winningBid.setTransactionid(transactionId);
            winningBid.setTransactionhash(transactionHash);
            bidsRepository.add(winningBid);
        } else {
            log.debug("Transaction Id " + transactionId + " status not SUCCESS.");
        }
    }

    private void refund(long amount, String accountId) {
        //TODO:
        //TODO: Check accountId is not null (Should not happen !).
        log.info("Refunding " + amount + " to " + accountId + " from " + this.auction.getAuctionaccountid());
    }
}
