package com.hedera.demo.auction.node.app.bidwatchers;

import com.hedera.demo.auction.node.app.HederaClient;
import com.hedera.demo.auction.node.app.domain.Auction;
import com.hedera.demo.auction.node.app.refunder.Refunder;
import com.hedera.demo.auction.node.app.repository.AuctionsRepository;
import com.hedera.demo.auction.node.app.repository.BidsRepository;
import io.vertx.ext.web.client.WebClient;
import org.jooq.tools.StringUtils;

import java.util.Arrays;

public abstract class AbstractBidsWatcher {

    protected Auction auction;
    protected final WebClient webClient;
    protected final BidsRepository bidsRepository;
    protected final AuctionsRepository auctionsRepository;
    protected final String refundKey;
    protected final int mirrorQueryFrequency;
    protected String mirrorURL;

    protected AbstractBidsWatcher(WebClient webClient, AuctionsRepository auctionsRepository, BidsRepository bidsRepository, Auction auction, String refundKey, int mirrorQueryFrequency) throws Exception {
        this.webClient = webClient;
        this.bidsRepository = bidsRepository;
        this.auctionsRepository = auctionsRepository;
        this.auction = auction;
        this.refundKey = refundKey;
        this.mirrorQueryFrequency = mirrorQueryFrequency;
        this.mirrorURL = HederaClient.getMirrorUrl();
    }

    boolean checkMemos(String memo) {
        if (StringUtils.isEmpty(memo)) {
            return false;
        }
        String[] memos = new String[]{"CREATEAUCTION", "FUNDACCOUNT", "TRANSFERTOAUCTION", "ASSOCIATE", "AUCTION REFUND"};
        return Arrays.stream(memos).anyMatch(memo.toUpperCase()::equals);
    }

    void startRefundThread(long refundAmound, String refundToAccount, String timestamp, String transactionId) {
        Thread t = new Thread(new Refunder(bidsRepository, auction.getAuctionaccountid(), refundAmound, refundToAccount, timestamp, transactionId, refundKey));
        t.start();
    }
}
