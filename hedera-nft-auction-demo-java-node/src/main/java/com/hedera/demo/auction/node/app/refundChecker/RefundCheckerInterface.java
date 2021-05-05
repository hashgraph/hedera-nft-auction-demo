package com.hedera.demo.auction.node.app.refundChecker;

import java.sql.SQLException;

public interface RefundCheckerInterface {
    void watch();
    void stop();
}
