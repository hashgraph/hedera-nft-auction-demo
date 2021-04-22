package com.hedera.demo.auction.node.app;

import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;

import javax.annotation.Nullable;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class SqlConnectionManager {
    private final String url;

    private final String username;

    private final String password;

    @Nullable
    private Connection connection;

    // Blank SqlConnectionManager for testing
    public SqlConnectionManager(String url, String username, String password) {
        this.url = url.replaceAll("jdbc:", "");
        this.username = username;
        this.password = password;
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
