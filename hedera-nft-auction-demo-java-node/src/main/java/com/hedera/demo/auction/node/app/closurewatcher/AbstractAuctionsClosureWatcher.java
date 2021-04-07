package com.hedera.demo.auction.node.app.closurewatcher;

import com.hedera.demo.auction.node.app.HederaClient;
import com.hedera.demo.auction.node.app.domain.Auction;
import com.hedera.demo.auction.node.app.repository.AuctionsRepository;
import com.hedera.demo.auction.node.app.mirrormapping.MirrorTransactions;
import com.hedera.hashgraph.sdk.AccountId;
import com.hedera.hashgraph.sdk.AccountUpdateTransaction;
import com.hedera.hashgraph.sdk.Client;
import com.hedera.hashgraph.sdk.PrivateKey;
import com.hedera.hashgraph.sdk.Status;
import com.hedera.hashgraph.sdk.TransactionId;
import com.hedera.hashgraph.sdk.TransactionReceipt;
import com.hedera.hashgraph.sdk.TransactionResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import lombok.extern.log4j.Log4j2;

import java.sql.SQLException;
import java.util.Map;

@Log4j2
public abstract class AbstractAuctionsClosureWatcher {

    protected final WebClient webClient;
    protected final AuctionsRepository auctionsRepository;
    protected final int mirrorQueryFrequency;
    protected String mirrorURL;
    protected boolean transferOnWin;
    private final String refundKey;

    protected AbstractAuctionsClosureWatcher(WebClient webClient, AuctionsRepository auctionsRepository, int mirrorQueryFrequency, boolean transferOnWin, String refundKey) throws Exception {
        this.webClient = webClient;
        this.auctionsRepository = auctionsRepository;
        this.mirrorQueryFrequency = mirrorQueryFrequency;
        this.mirrorURL = HederaClient.getMirrorUrl();
        this.transferOnWin = transferOnWin;
        this.refundKey = refundKey;
    }

    void handleResponse(JsonObject response) throws Exception {
        if (response != null) {
            MirrorTransactions mirrorTransactions = response.mapTo(MirrorTransactions.class);

            if (mirrorTransactions.transactions != null) {
                if (mirrorTransactions.transactions.size() > 0) {
                    closeAuctionIfPastEnd(mirrorTransactions.transactions.get(0).consensusTimestamp);
                }
            }
        }
    }

    protected void closeAuctionIfPastEnd(String consensusTimestamp) throws Exception {
        for (Map.Entry<String, Integer> auctions : auctionsRepository.openAndPendingAuctions().entrySet()) {
            String endTimestamp = auctions.getKey();
            int auctionId = auctions.getValue();

            if (consensusTimestamp.compareTo(endTimestamp) > 0) {
                // latest transaction past auctions end, close it
                log.info("Closing auction id " + auctionId);
                try {
                    if (transferOnWin) {
                        // if the auction transfers the token on winning, set the auction to closed (no more bids)
                        auctionsRepository.setClosed(auctionId);
                    } else {
                        // if the auction does not transfer the token on winning, set the auction to ended
                        auctionsRepository.setEnded(auctionId, "");
                    }
                    if ( ! this.refundKey.isBlank()) {
                        setSignatureRequiredOnAuctionAccount(auctionId);
                    }

                } catch (SQLException e) {
                    log.error(e);
                }
            }
        }
    }

    protected void setSignatureRequiredOnAuctionAccount(int auctionId) throws Exception {
        Client client = HederaClient.getClient();

        Auction auction = auctionsRepository.getAuction(auctionId);
        PrivateKey refundKeyPrivate = PrivateKey.fromString(this.refundKey);
        client.setOperator(AccountId.fromString(auction.getAuctionaccountid()), refundKeyPrivate);
        log.info("Setting signature required key on account " + auction.getAuctionaccountid());

//            //TODO: Scheduled transaction here
//            // create a deterministic transaction id from the consensus timestamp of the payment transaction
//            // note: this assumes the scheduled transaction occurs quickly after the payment
//            String deterministicTxId = this.auctionAccountId.concat("@").concat(this.consensusTimestamp);
//            TransactionId transactionId = TransactionId.fromString(deterministicTxId);
//            // Schedule the transaction
//            ScheduleCreateTransaction scheduleCreateTransaction = transferTransaction.schedule();
//
//            TransactionResponse response = scheduleCreateTransaction.execute(client);
//            TransactionReceipt receipt = response.getReceipt(client);
//

        TransactionId transactionId = TransactionId.generate(AccountId.fromString(auction.getAuctionaccountid()));

        AccountUpdateTransaction accountUpdateTransaction = new AccountUpdateTransaction();
        accountUpdateTransaction.setTransactionId(transactionId);
        accountUpdateTransaction.setAccountId(AccountId.fromString(auction.getAuctionaccountid()));
        accountUpdateTransaction.setReceiverSignatureRequired(true);

        TransactionResponse response = accountUpdateTransaction.execute(client);
        // check for receipt
        TransactionReceipt receipt = response.getReceipt(client);
        if (receipt.status != Status.SUCCESS) {
            log.error("Setting receiver signature required on account " + auction.getAuctionaccountid() + " failed with " + receipt.status);
            return;
        }
    }
}
