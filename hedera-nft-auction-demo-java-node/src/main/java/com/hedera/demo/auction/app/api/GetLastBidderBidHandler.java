package com.hedera.demo.auction.app.api;

import com.google.errorprone.annotations.Var;
import com.hedera.demo.auction.app.domain.Bid;
import io.vertx.core.Handler;
import io.vertx.core.json.Json;
import io.vertx.ext.web.RoutingContext;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.Tuple;

/**
 * Returns the last bid for a given auction and bidding account
 */
public class GetLastBidderBidHandler implements Handler<RoutingContext> {

    private final PgPool pgPool;

    GetLastBidderBidHandler(PgPool pgPool) {
        this.pgPool = pgPool;
    }

    /**
     * Query the database for the last bid for the provided auction id and bidder account id
     *
     * @param routingContext the RoutingContext
     */
    @Override
    public void handle(RoutingContext routingContext) {
        int auctionid = Integer.parseInt(routingContext.pathParam("auctionid"));
        String bidderAccountId = routingContext.pathParam("bidderaccountid");

        @Var String sql = "SELECT * FROM bids WHERE auctionid = $1 and bidderaccountid = $2";
        sql = sql.concat(" and timestamp = (SELECT MAX(timestamp) FROM bids WHERE auctionid = $1 and bidderaccountid = $2)");

        pgPool.preparedQuery(sql).execute(Tuple.tuple().addInteger(auctionid).addString(bidderAccountId), ar -> {
            if (ar.failed()) {
                routingContext.fail(ar.cause());
                return;
            }

            @Var var bid = new Bid();
            var rows = ar.result();
            for (var row : rows) {
                bid = new Bid(row);
            }

            routingContext.response()
                    .putHeader("content-type", "application/json")
                    .end(Json.encodeToBuffer(bid));
        });
    }
}
