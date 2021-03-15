package com.hedera.demo.auction.web.app.api;

import com.hedera.demo.auction.web.app.domain.Auction;
import io.vertx.core.Handler;
import io.vertx.core.json.Json;
import io.vertx.ext.web.RoutingContext;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.Tuple;

public class AuctionHandler implements Handler<RoutingContext> {
    private final PgPool pgPool;

    public AuctionHandler(PgPool pgPool) {
        this.pgPool = pgPool;
    }

    @Override
    public void handle(RoutingContext routingContext) {
        long id = Long.parseLong(routingContext.pathParam("id"));

        String sql = "SELECT * FROM auctions WHERE id = $1";

        pgPool.preparedQuery(sql).execute(Tuple.of(id), ar -> {
            if (ar.failed()) {
                routingContext.fail(ar.cause());
                return;
            }

            var auction = new Auction();
            var rows = ar.result();
            for (var row : rows) {
                auction = new Auction(row);
            }

            routingContext.response()
                    .putHeader("content-type", "application/json")
                    .end(Json.encodeToBuffer(auction));
        });
    }
}
