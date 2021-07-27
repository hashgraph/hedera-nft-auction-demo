package com.hedera.demo.auction.app;

import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;

import javax.annotation.Nullable;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Connection manager for the database
 */
public class SqlConnectionManager {
    private final String url;
    private final String username;
    private final String password;
    @Nullable
    private Connection connection;

    /**
     * Constructor
     *
     * @param url the url to the database
     * @param username the username
     * @param password the password
     */
    public SqlConnectionManager(String url, String username, String password) {
        this.url = url.replaceAll("jdbc:", "");
        this.username = username;
        this.password = password;
    }

    /**
     * Returns a context to the database
     * @return DSLContext a database context
     * @throws SQLException in the event of a database error
     */
    public DSLContext dsl() throws SQLException {
        return DSL.using(getConnection(), SQLDialect.POSTGRES);
    }

    /**
     * Gets a connection to the database if not already established, else returns the current connection
     * @return Connection a connection to the database
     * @throws SQLException in the event of an error
     */
    public synchronized Connection getConnection() throws SQLException {
        if (connection == null) {
            connection = DriverManager.getConnection("jdbc:" + url, username, password);
        }

        return connection;
    }
}
