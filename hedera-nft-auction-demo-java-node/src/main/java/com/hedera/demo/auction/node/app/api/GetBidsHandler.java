package com.hedera.demo.auction.node.app.api;

import com.hedera.demo.auction.node.app.domain.Bid;
import io.vertx.core.Handler;
import io.vertx.core.json.Json;
import io.vertx.ext.web.RoutingContext;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.Tuple;

import java.util.ArrayList;

public class GetBidsHandler implements Handler<RoutingContext> {

    private final PgPool pgPool;

    GetBidsHandler(PgPool pgPool) {
        this.pgPool = pgPool;
    }

    @Override
    public void handle(RoutingContext routingContext) {

        int auctionId = Integer.parseInt(routingContext.pathParam("auctionid"));
        String sql = "SELECT * FROM bids WHERE auctionid = $1 ORDER BY timestamp desc limit 50";

        pgPool.preparedQuery(sql).execute(Tuple.of(auctionId), ar -> {
            if (ar.failed()) {
                routingContext.fail(ar.cause());
                return;
            }

            var rows = ar.result();
            var bids = new ArrayList<Bid>(rows.rowCount());

            for (var row : rows) {
                var bid = new Bid(row);
                bids.add(bid);
            }

            routingContext.response()
                    .putHeader("content-type", "application/json")
                    .end(Json.encodeToBuffer(bids));
        });
    }
}
