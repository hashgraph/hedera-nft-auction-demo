package com.hedera.demo.auction.app.api;

import com.hedera.demo.auction.app.domain.Auction;
import com.hedera.demo.auction.app.repository.AuctionsRepository;
import io.vertx.core.Handler;
import io.vertx.core.json.Json;
import io.vertx.ext.web.RoutingContext;

/**
 * Gets the details of a particular auction
 */
public class GetAuctionHandler implements Handler<RoutingContext>  {

    private final AuctionsRepository auctionsRepository;

    GetAuctionHandler(AuctionsRepository auctionsRepository) {
        this.auctionsRepository = auctionsRepository;
    }

    /**
     * Given an auction id, query the database for its details
     *
     * @param routingContext the RoutingContext
     */
    @Override
    public void handle(RoutingContext routingContext) {
        int id = Integer.parseInt(routingContext.pathParam("id"));
        try {
            Auction auction = auctionsRepository.getAuction(id);
            routingContext.response()
                    .putHeader("content-type", "application/json")
                    .end(Json.encodeToBuffer(auction));
        } catch (Exception e) {
            if (e.getMessage() != null) {
                if (e.getMessage().contains("No auction id")) {
                    routingContext.fail(404);
                    return;
                }
            }
            routingContext.fail(500, e);
        }
    }
}
