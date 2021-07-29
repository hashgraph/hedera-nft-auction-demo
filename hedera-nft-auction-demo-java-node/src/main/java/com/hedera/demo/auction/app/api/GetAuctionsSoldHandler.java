package com.hedera.demo.auction.app.api;

import com.hedera.demo.auction.app.domain.Auction;
import com.hedera.demo.auction.app.repository.AuctionsRepository;
import io.vertx.core.Handler;
import io.vertx.core.json.Json;
import io.vertx.ext.web.RoutingContext;

import java.sql.SQLException;
import java.util.List;

/**
 * Gets auctions for which the reserve was not met regardless of status
 */
public class GetAuctionsSoldHandler implements Handler<RoutingContext> {
    private final AuctionsRepository auctionsRepository;

    public GetAuctionsSoldHandler(AuctionsRepository auctionsRepository) {
        this.auctionsRepository = auctionsRepository;
    }

    /**
     * Query the database for all auctions where the winning bid is below reserve
     *
     * @param routingContext the RoutingContext
     */
    @Override
    public void handle(RoutingContext routingContext) {

        try {
            List<Auction> auctions = auctionsRepository.getAuctionsSold();
            routingContext.response()
                    .putHeader("content-type", "application/json")
                    .end(Json.encodeToBuffer(auctions));
        } catch (SQLException e) {
            routingContext.fail(500, e);
        }
    }
}
