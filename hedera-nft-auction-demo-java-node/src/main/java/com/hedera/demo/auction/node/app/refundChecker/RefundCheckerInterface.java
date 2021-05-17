package com.hedera.demo.auction.node.app.refundChecker;

public interface RefundCheckerInterface {
    void watch();
    void stop();
    void watchOnce();
}
