package com.hedera.demo.auction.node.test.integration.database;

import org.flywaydb.core.Flyway;
import org.testcontainers.containers.PostgreSQLContainer;

public class AbstractDatabaseTest {
    void migrate(PostgreSQLContainer postgres) {
        String postgresUrl = postgres.getJdbcUrl();
        String postgresUser = postgres.getUsername();
        String postgresPassword = postgres.getPassword();
        Flyway flyway = Flyway
                .configure()
                .dataSource(postgresUrl, postgresUser, postgresPassword)
                .locations("filesystem:./src/main/resources/migrations")
                .load();
        flyway.migrate();

    }
}
