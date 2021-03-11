package com.hedera.demo.auction.web.app;

import io.github.cdimascio.dotenv.Dotenv;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;

import javax.annotation.Nullable;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Objects;

public class SqlConnectionManager {
    private final String url;

    private final String username;

    private final String password;

    @Nullable
    private Connection connection;

    public SqlConnectionManager(Dotenv env) {
        url = Objects.requireNonNull(env.get("DATABASE_URL"), "missing environment variable DATABASE_URL");

        username = Objects.requireNonNull(
            env.get("DATABASE_USERNAME"), "missing environment variable DATABASE_USERNAME");

        password = Objects.requireNonNull(
            env.get("DATABASE_PASSWORD"), "missing environment variable DATABASE_PASSWORD");
    }

    // Blank SqlConnectionManager for testing
    public SqlConnectionManager() {
        url = "";
        username = "";
        password = "";
    }

    public DSLContext dsl() throws SQLException {
        return DSL.using(getConnection(), SQLDialect.POSTGRES);
    }

    public synchronized Connection getConnection() throws SQLException {
        if (connection == null) {
            connection = newConnection();
        }

        return connection;
    }

    private Connection newConnection() throws SQLException {
        return DriverManager.getConnection("jdbc:" + url, username, password);
    }
}
