package com.hedera.demo.auction.node.app;

import io.github.cdimascio.dotenv.Dotenv;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.PoolOptions;

import java.util.Objects;
import java.util.Optional;

public class PGPool {
    private final String url;
    private final String username;
    private final String password;
    private final int poolSize;

    public PGPool(Dotenv env) {
        this.url = Objects.requireNonNull(env.get("DATABASE_URL"), "missing environment variable DATABASE_URL");

        this.username = Objects.requireNonNull(
            env.get("DATABASE_USERNAME"), "missing environment variable DATABASE_USERNAME");

        this.password = Objects.requireNonNull(
            env.get("DATABASE_PASSWORD"), "missing environment variable DATABASE_PASSWORD");

        this.poolSize = Integer.parseInt(Optional.ofNullable(env.get("POOL_SIZE")).orElse("10"));

    }
    public PgPool createPgPool() {
        try {
            PgPool pgPool = PgPool.pool(
                    PgConnectOptions
                        .fromUri(url)
                        .setUser(username)
                        .setPassword(password),
                    new PoolOptions()
                            .setMaxSize(this.poolSize)
            );
            return pgPool;
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return null;
        }
    }
}
