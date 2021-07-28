package com.hedera.demo.auction.app.api;

import com.hedera.demo.auction.app.Utils;
import com.hedera.hashgraph.sdk.AccountId;
import com.hedera.hashgraph.sdk.TokenId;
import com.hedera.hashgraph.sdk.TopicId;
import org.jooq.tools.StringUtils;

/**
 * Data class to map incoming REST JSON to a java object
 */
@SuppressWarnings("unused")
public class RequestCreateAuction {
    public String tokenid = "";
    public String auctionaccountid = "";
    public long reserve = 0L;
    public long minimumbid = 0L;
    public String endtimestamp = "";
    public boolean winnercanbid = false;
    public String topicid = "";
    private String description = "";
    private String title = "";
    public String createauctiontxid = "";

    public void setDescription(String description) {
        this.description = Utils.normalize(description);
    }
    public String getDescription() {
        return this.description;
    }

    public void setTitle(String title) {
        this.title = Utils.normalize(title);
    }
    public String getTitle() {
        return this.title;
    }

    public String isValid() {
        try {
            TokenId.fromString(tokenid);
        } catch (@SuppressWarnings("UnusedException") Exception e) {
            return "invalid format for tokenid, should be 0.0.1234";
        }

        try {
            AccountId.fromString(auctionaccountid);
        } catch (@SuppressWarnings("UnusedException") Exception e) {
            return "invalid format for auctionaccountid, should be 0.0.1234";
        }

        if (!StringUtils.isEmpty(topicid)) {
            try {
                TopicId.fromString(topicid);
            } catch (@SuppressWarnings("UnusedException") Exception e) {
                return "invalid format for topicid, should be 0.0.1234";
            }
        }
        return "";
    }
}
