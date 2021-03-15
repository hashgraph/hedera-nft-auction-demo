package com.hedera.demo.auction.web.app.api;

import com.hedera.demo.auction.web.app.domain.Bid;
import io.vertx.core.Handler;
import io.vertx.core.json.Json;
import io.vertx.ext.web.RoutingContext;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.Tuple;

public class LastBidderBidHandler implements Handler<RoutingContext> {
    private final PgPool pgPool;

    public LastBidderBidHandler(PgPool pgPool) {
        this.pgPool = pgPool;
    }

    @Override
    public void handle(RoutingContext routingContext) {
        int auctionid = Integer.parseInt(routingContext.pathParam("auctionid"));
        String bidderAccountId = routingContext.pathParam("bidderaccountid");

        String sql = "SELECT * FROM bids WHERE auctionid = $1 and bidderaccountid = $2";
        sql = sql.concat(" and timestamp = (SELECT MAX(timestamp) FROM bids WHERE auctionid = $1 and bidderaccountid = $2)");

        pgPool.preparedQuery(sql).execute(Tuple.tuple().addInteger(auctionid).addString(bidderAccountId), ar -> {
            if (ar.failed()) {
                routingContext.fail(ar.cause());
                return;
            }

            var bid = new Bid();
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
