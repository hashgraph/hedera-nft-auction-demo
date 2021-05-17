package com.hedera.demo.auction.node.app.repository;

import com.hedera.demo.auction.node.app.SqlConnectionManager;
import org.jooq.DSLContext;

import java.sql.SQLException;

import static com.hedera.demo.auction.node.app.db.Tables.SCHEDULEDOPERATIONSLOG;

public class ScheduledOperationsLogRepository {
    private final SqlConnectionManager connectionManager;

    public ScheduledOperationsLogRepository(SqlConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    public void deleteAllScheduledLogOperations() throws SQLException {
        DSLContext cx = connectionManager.dsl();
        cx.deleteFrom(SCHEDULEDOPERATIONSLOG)
            .execute();
        cx.close();
    }

}
