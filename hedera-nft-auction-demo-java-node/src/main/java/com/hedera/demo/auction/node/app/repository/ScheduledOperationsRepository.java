package com.hedera.demo.auction.node.app.repository;

import com.google.errorprone.annotations.Var;
import com.hedera.demo.auction.node.app.SqlConnectionManager;
import com.hedera.demo.auction.node.app.domain.ScheduledOperation;
import lombok.extern.log4j.Log4j2;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.exception.DataAccessException;

import javax.annotation.Nullable;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static com.hedera.demo.auction.node.app.db.Tables.SCHEDULEDOPERATIONS;
import static com.hedera.demo.auction.node.app.db.Tables.SCHEDULEDOPERATIONSLOG;

@Log4j2
public class ScheduledOperationsRepository {
    private final SqlConnectionManager connectionManager;

    public ScheduledOperationsRepository(SqlConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    @Nullable
    private Result<Record> getOperations (String status) throws SQLException {
        DSLContext cx = connectionManager.dsl();

        Result<Record> rows = cx.selectFrom(SCHEDULEDOPERATIONS)
                .where(SCHEDULEDOPERATIONS.STATUS.eq(status))
                .orderBy(SCHEDULEDOPERATIONS.TRANSACTIONTIMESTAMP)
                .fetch();
        cx.close();
        return rows;
    }

    public List<ScheduledOperation> getPendingOperationsList() throws SQLException {
        List<ScheduledOperation> operations = new ArrayList<>();
        Result<Record> operationsData = getOperations(ScheduledOperation.PENDING);
        if (operationsData != null) {
            for (Record record : operationsData) {
                ScheduledOperation scheduledOperation = new ScheduledOperation(record);
                operations.add(scheduledOperation);
            }
        }
        return operations;
    }

    public List<ScheduledOperation> getExecutingOperationsList() throws SQLException {
        List<ScheduledOperation> operations = new ArrayList<>();
        Result<Record> operationsData = getOperations(ScheduledOperation.EXECUTING);
        if (operationsData != null) {
            for (Record record : operationsData) {
                ScheduledOperation scheduledOperation = new ScheduledOperation(record);
                operations.add(scheduledOperation);
            }
        }
        return operations;
    }

    public void deleteAllScheduledOperations() throws SQLException {
        DSLContext cx = connectionManager.dsl();
        cx.deleteFrom(SCHEDULEDOPERATIONS)
            .execute();
        cx.close();
    }

    public boolean setSuccessful(String transactionId) throws SQLException {
        DSLContext cx = connectionManager.dsl();
        cx.update(SCHEDULEDOPERATIONS)
                .set(SCHEDULEDOPERATIONS.STATUS, ScheduledOperation.SUCCESSFUL)
                .set(SCHEDULEDOPERATIONS.RESULT, "")
                .where(SCHEDULEDOPERATIONS.TRANSACTIONID.eq(transactionId))
                .execute();

        createLogForTransactionId(cx, transactionId);
        return true;
    }

    public boolean setStatus(String timestamp, String status, String result) throws SQLException {
        DSLContext cx = connectionManager.dsl();
        cx.update(SCHEDULEDOPERATIONS)
                .set(SCHEDULEDOPERATIONS.STATUS, status)
                .set(SCHEDULEDOPERATIONS.RESULT, result)
                .where(SCHEDULEDOPERATIONS.TRANSACTIONTIMESTAMP.eq(timestamp))
                .execute();
        createLogForTimestamp(cx, timestamp);
        return true;
    }

    public boolean setTimestamp(String originalTimestamp, String newTimestamp) throws SQLException {
        DSLContext cx = connectionManager.dsl();
        cx.update(SCHEDULEDOPERATIONS)
                .set(SCHEDULEDOPERATIONS.STATUS, ScheduledOperation.PENDING)
                .set(SCHEDULEDOPERATIONS.TRANSACTIONTIMESTAMP, newTimestamp)
                .set(SCHEDULEDOPERATIONS.RESULT, "delayed")
                .where(SCHEDULEDOPERATIONS.TRANSACTIONTIMESTAMP.eq(originalTimestamp))
                .execute();
        createLogForTimestamp(cx, newTimestamp);
        return true;
    }

    public boolean add(ScheduledOperation scheduledOperation) throws SQLException {
        @Var DSLContext cx = null;
        @Var boolean result = false;
        try {
            cx = connectionManager.dsl();
            cx.insertInto(SCHEDULEDOPERATIONS,
                    SCHEDULEDOPERATIONS.AUCTIONID,
                    SCHEDULEDOPERATIONS.STATUS,
                    SCHEDULEDOPERATIONS.TRANSACTIONID,
                    SCHEDULEDOPERATIONS.TRANSACTIONTIMESTAMP,
                    SCHEDULEDOPERATIONS.MEMO,
                    SCHEDULEDOPERATIONS.TRANSACTIONTYPE
            ).values(
                    scheduledOperation.getAuctionid(),
                    ScheduledOperation.PENDING,
                    scheduledOperation.getTransactionid(),
                    scheduledOperation.getTransactiontimestamp(),
                    scheduledOperation.getMemo(),
                    scheduledOperation.getTransactiontype()
            ).execute();
            result = true;
        } catch (DataAccessException e) {
            log.info("scheduled operation already in database");
        } finally {
            if (cx != null) {
                cx.close();
            }
        }
        return result;
    }

    private void createLogForTransactionId(DSLContext cx, String transactionId) {
        Result<Record> operation = cx.selectFrom(SCHEDULEDOPERATIONS)
                .where(SCHEDULEDOPERATIONS.TRANSACTIONID.eq(transactionId))
                .fetch();

        cx.insertInto(SCHEDULEDOPERATIONSLOG,
                SCHEDULEDOPERATIONSLOG.AUCTIONID,
                SCHEDULEDOPERATIONSLOG.TRANSACTIONID,
                SCHEDULEDOPERATIONSLOG.TRANSACTIONTIMESTAMP,
                SCHEDULEDOPERATIONSLOG.TRANSACTIONTYPE,
                SCHEDULEDOPERATIONSLOG.RESULT
        ).values(
                operation.getValue(0, SCHEDULEDOPERATIONS.AUCTIONID),
                operation.getValue(0, SCHEDULEDOPERATIONS.TRANSACTIONID),
                operation.getValue(0, SCHEDULEDOPERATIONS.TRANSACTIONTIMESTAMP),
                operation.getValue(0, SCHEDULEDOPERATIONS.TRANSACTIONTYPE),
                operation.getValue(0, SCHEDULEDOPERATIONS.RESULT)
        ).execute();
    }

    private void createLogForTimestamp(DSLContext cx, String transactionTimestamp) {
        Result<Record> operation = cx.selectFrom(SCHEDULEDOPERATIONS)
                .where(SCHEDULEDOPERATIONS.TRANSACTIONTIMESTAMP.eq(transactionTimestamp))
                .fetch();

        cx.insertInto(SCHEDULEDOPERATIONSLOG,
                SCHEDULEDOPERATIONSLOG.AUCTIONID,
                SCHEDULEDOPERATIONSLOG.TRANSACTIONID,
                SCHEDULEDOPERATIONSLOG.TRANSACTIONTIMESTAMP,
                SCHEDULEDOPERATIONSLOG.TRANSACTIONTYPE,
                SCHEDULEDOPERATIONSLOG.RESULT
        ).values(
                operation.getValue(0, SCHEDULEDOPERATIONS.AUCTIONID),
                operation.getValue(0, SCHEDULEDOPERATIONS.TRANSACTIONID),
                operation.getValue(0, SCHEDULEDOPERATIONS.TRANSACTIONTIMESTAMP),
                operation.getValue(0, SCHEDULEDOPERATIONS.TRANSACTIONTYPE),
                operation.getValue(0, SCHEDULEDOPERATIONS.RESULT)
        ).execute();
    }
}
