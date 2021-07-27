package com.hedera.demo.auction.app.api;

import com.hedera.demo.auction.app.domain.Bid;
import com.hedera.demo.auction.app.repository.BidsRepository;
import io.vertx.core.Handler;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

import java.sql.SQLException;

/**
 * Returns the last bid for a given auction and bidding account
 */
public class GetLastBidderBidHandler implements Handler<RoutingContext> {

    private final BidsRepository bidsRepository;

    GetLastBidderBidHandler(BidsRepository bidsRepository) {
        this.bidsRepository = bidsRepository;
    }

    /**
     * Query the database for the last bid for the provided auction id and bidder account id
     *
     * @param routingContext the RoutingContext
     */
    @Override
    public void handle(RoutingContext routingContext) {
        int auctionId = Integer.parseInt(routingContext.pathParam("auctionid"));
        String bidderAccountId = routingContext.pathParam("bidderaccountid");

        try {
            Bid bid = bidsRepository.getBidderLastBid(auctionId, bidderAccountId);
            if (bid == null) {
                routingContext.response()
                        .putHeader("content-type", "application/json")
                        .end(Json.encodeToBuffer(new JsonObject()));
            } else {
                routingContext.response()
                        .putHeader("content-type", "application/json")
                        .end(Json.encodeToBuffer(bid));
            }
        } catch (SQLException e) {
            routingContext.fail(e.getCause());
            return;
        }
    }
}
