package com.hedera.demo.auction.app.api;

import com.hedera.demo.auction.app.Utils;

/**
 * Data class to map incoming REST JSON to a java object
 */
@SuppressWarnings("unused")
public class RequestCreateTokenMetaData {
    public String type = "";
    private String description = "";

    public void setDescription(String description) {
        this.description = Utils.normalize(description);
    }

    public String getDescription() {
        return this.description;
    }
}
