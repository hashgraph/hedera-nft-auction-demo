package com.hedera.demo.auction.app.api;

import com.google.errorprone.annotations.Var;
import com.hedera.demo.auction.app.domain.Auction;
import io.vertx.core.Handler;
import io.vertx.core.json.Json;
import io.vertx.ext.web.RoutingContext;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.Tuple;

/**
 * Gets the details of a particular auction
 */
public class GetAuctionHandler implements Handler<RoutingContext>  {

    private final PgPool pgPool;

    GetAuctionHandler(PgPool pgPool) {
        this.pgPool = pgPool;
    }

    /**
     * Given an auction id, query the database for its details
     *
     * @param routingContext the RoutingContext
     */
    @Override
    public void handle(RoutingContext routingContext) {
        long id = Long.parseLong(routingContext.pathParam("id"));

        String sql = "SELECT * FROM auctions WHERE id = $1";

        pgPool.preparedQuery(sql).execute(Tuple.of(id), ar -> {
            if (ar.failed()) {
                routingContext.fail(ar.cause());
                return;
            }

            @Var Auction auction = new Auction();
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
