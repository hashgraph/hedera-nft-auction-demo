package com.hedera.demo.auction.app.api;

import com.hedera.demo.auction.app.domain.Auction;
import com.hedera.demo.auction.app.repository.AuctionsRepository;
import io.vertx.core.Handler;
import io.vertx.core.json.Json;
import io.vertx.ext.web.RoutingContext;

import java.sql.SQLException;
import java.util.List;

/**
 * Gets all the auctions from the database
 */
public class GetAuctionsHandler implements Handler<RoutingContext> {
    private final AuctionsRepository auctionsRepository;

    public GetAuctionsHandler(AuctionsRepository auctionsRepository) {
        this.auctionsRepository = auctionsRepository;
    }

    /**
     * Query the database for all the auctions
     *
     * @param routingContext the RoutingContext
     */
    @Override
    public void handle(RoutingContext routingContext) {

        try {
            List<Auction> auctions = auctionsRepository.getAuctionsList();
            routingContext.response()
                    .putHeader("content-type", "application/json")
                    .end(Json.encodeToBuffer(auctions));
        } catch (SQLException e) {
            routingContext.fail(500, e);
        }
    }
}
