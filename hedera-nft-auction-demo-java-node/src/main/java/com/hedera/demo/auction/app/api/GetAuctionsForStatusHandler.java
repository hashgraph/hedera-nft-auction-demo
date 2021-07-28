package com.hedera.demo.auction.app.api;

import com.hedera.demo.auction.app.domain.Auction;
import com.hedera.demo.auction.app.repository.AuctionsRepository;
import io.vertx.core.Handler;
import io.vertx.core.json.Json;
import io.vertx.ext.web.RoutingContext;

import java.sql.SQLException;
import java.util.List;

/**
 * Gets auctions that match the provided status
 */
public class GetAuctionsForStatusHandler implements Handler<RoutingContext> {
    private final AuctionsRepository auctionsRepository;
    private final String status;

    public GetAuctionsForStatusHandler(AuctionsRepository auctionsRepository, String status) {
        this.auctionsRepository = auctionsRepository;
        this.status = status;
    }

    /**
     * Query the database for all auctions which have a matching status
     *
     * @param routingContext the RoutingContext
     */
    @Override
    public void handle(RoutingContext routingContext) {

        try {
            List<Auction> auctions = auctionsRepository.getByStatus(status);
            routingContext.response()
                    .putHeader("content-type", "application/json")
                    .end(Json.encodeToBuffer(auctions));
        } catch (SQLException e) {
            routingContext.fail(500, e);
        }
    }
}
