CREATE TABLE scheduledOperations
(
    transactionType        TEXT  DEFAULT '',
    transactionTimestamp   TEXT  DEFAULT '' UNIQUE,
    auctionId              INTEGER  DEFAULT 0,
    transactionId          TEXT  DEFAULT '' UNIQUE,
    status                 TEXT  DEFAULT 'PENDING',
    memo                   TEXT  DEFAULT '',
    result                 TEXT  DEFAULT ''
);
