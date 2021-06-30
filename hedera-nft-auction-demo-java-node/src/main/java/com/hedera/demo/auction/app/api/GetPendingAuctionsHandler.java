package com.hedera.demo.auction.app.api;

import com.hedera.demo.auction.app.domain.Auction;
import io.vertx.core.Handler;
import io.vertx.core.json.Json;
import io.vertx.ext.web.RoutingContext;
import io.vertx.pgclient.PgPool;

import java.util.ArrayList;

/**
 * Gets the list of auctions that are pending
 */
public class GetPendingAuctionsHandler implements Handler<RoutingContext> {
    private final PgPool pgPool;

    public GetPendingAuctionsHandler(PgPool pgPool) {
        this.pgPool = pgPool;
    }

    /**
     * Query the database for auctions which have a status of PENDING
     *
     * @param routingContext the RoutingContext
     */
    @Override
    public void handle(RoutingContext routingContext) {

        String sql = "SELECT * FROM auctions WHERE status='".concat(Auction.PENDING).concat("' ORDER BY id");

        pgPool.preparedQuery(sql).execute(ar -> {
            if (ar.failed()) {
                routingContext.fail(ar.cause());
                return;
            }

            var rows = ar.result();
            var auctions = new ArrayList<Auction>(rows.rowCount());

            for (var row : rows) {
                var item = new Auction(row);

                auctions.add(item);
            }

            routingContext.response()
                    .putHeader("content-type", "application/json")
                    .end(Json.encodeToBuffer(auctions));
        });
    }
}
