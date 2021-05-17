CREATE TABLE scheduledOperationsLog
(
    transactionType        TEXT  DEFAULT '',
    transactionTimestamp   TEXT  DEFAULT '',
    auctionId              INTEGER  DEFAULT 0,
    transactionId          TEXT  DEFAULT '',
    result                 TEXT  DEFAULT ''
);
