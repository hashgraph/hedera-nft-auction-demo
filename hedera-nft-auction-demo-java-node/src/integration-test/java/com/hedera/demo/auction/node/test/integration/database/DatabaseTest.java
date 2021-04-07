package com.hedera.demo.auction.node.test.integration.database;

import com.hedera.demo.auction.node.app.SqlConnectionManager;
import com.hedera.demo.auction.node.app.domain.Auction;
import com.hedera.demo.auction.node.app.repository.AuctionsRepository;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

@Testcontainers
class DatabaseTest extends AbstractDatabaseTest {

    public DatabaseTest() {
    }

    private void migrate(PostgreSQLContainer postgres) {
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
    @Test
    public void addAuctionTest() throws Exception {

        try (PostgreSQLContainer<?> postgres = new PostgreSQLContainer("postgres:12.6")) {
            postgres.start();
            migrate(postgres);

            SqlConnectionManager connectionManager = new SqlConnectionManager(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
            AuctionsRepository auctionsRepository = new AuctionsRepository(connectionManager);

            Auction auction = new Auction();

            auction.setTokenid("tokenId");
            auction.setAuctionaccountid("auctionAccountId");
            auction.setEndtimestamp("end");
            auction.setReserve(20L);
            auction.setWinnercanbid(true);
            auction.setTokenimage("image");
            auction.setWinningbid(30L);
            auction.setMinimumbid(10L);

            Auction newAuction = auctionsRepository.add(auction);

            assertNotEquals(0, newAuction.getId());

            Auction getAuction = auctionsRepository.getAuction(newAuction.getId());

            assertEquals(auction.getTokenid(), getAuction.getTokenid());
            assertEquals("0.0", getAuction.getLastconsensustimestamp());
        }
    }

    @Test
    public void testAuctions() {
    }
}
