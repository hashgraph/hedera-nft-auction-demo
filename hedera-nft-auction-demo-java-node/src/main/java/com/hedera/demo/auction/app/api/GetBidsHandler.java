package com.hedera.demo.auction.app.api;

import com.hedera.demo.auction.app.domain.Bid;
import com.hedera.demo.auction.app.repository.BidsRepository;
import io.vertx.core.Handler;
import io.vertx.core.json.Json;
import io.vertx.ext.web.RoutingContext;
import lombok.extern.log4j.Log4j2;

import java.sql.SQLException;
import java.util.List;

/**
 * Gets all the bids for a given auction id
 */
@Log4j2
public class GetBidsHandler implements Handler<RoutingContext> {

    private final BidsRepository bidsRepository;
    private final int bidsToReturn;

    GetBidsHandler(BidsRepository bidsRepository, int bidsToReturn) {
        this.bidsRepository = bidsRepository;
        this.bidsToReturn = bidsToReturn;
    }

    /**
     * Given an auction id, get the last 50 bids from the database
     *
     * @param routingContext the RoutingContext
     */
    @Override
    public void handle(RoutingContext routingContext) {

        int auctionId = Integer.parseInt(routingContext.pathParam("auctionid"));
        try {
            List<Bid> bids = bidsRepository.getLastBids(auctionId, bidsToReturn);
            routingContext.response()
                    .putHeader("content-type", "application/json")
                    .end(Json.encodeToBuffer(bids));
        } catch (SQLException e) {
            log.error(e, e);
            routingContext.fail(500, e);
        }
    }
}
