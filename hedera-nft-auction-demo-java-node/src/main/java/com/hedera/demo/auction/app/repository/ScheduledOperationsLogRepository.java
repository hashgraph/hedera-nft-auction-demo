package com.hedera.demo.auction.app.repository;

import com.hedera.demo.auction.app.db.Tables;
import com.hedera.demo.auction.app.SqlConnectionManager;
import org.jooq.DSLContext;

import java.sql.SQLException;

public class ScheduledOperationsLogRepository {
    private final SqlConnectionManager connectionManager;

    public ScheduledOperationsLogRepository(SqlConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    public void deleteAllScheduledLogOperations() throws SQLException {
        DSLContext cx = connectionManager.dsl();
        cx.deleteFrom(Tables.SCHEDULEDOPERATIONSLOG)
            .execute();
//        cx.close();
    }

}
