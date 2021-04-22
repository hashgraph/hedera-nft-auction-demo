CREATE TABLE auctions
(
    id                     SERIAL PRIMARY KEY,
    lastConsensusTimestamp TEXT  DEFAULT '0.0',
    winningBid             INT8  DEFAULT 0,
    winningAccount         TEXT  DEFAULT '',
    winningTimestamp       TEXT  DEFAULT '',
    winningTxId            TEXT  DEFAULT '',
    winningTxHash          TEXT  DEFAULT '',
    transferTxId           TEXT  DEFAULT '',
    transferTxHash         TEXT  DEFAULT '',
    tokenId                TEXT  DEFAULT '',
    auctionAccountId       TEXT  DEFAULT '',
    endTimeStamp           TEXT  DEFAULT '',
    reserve                INT8  DEFAULT 0,
    status                 TEXT  DEFAULT 'PENDING',
    winnerCanBid           BOOLEAN DEFAULT false,
    tokenImage             TEXT DEFAULT '',
    minimumbid             INT8  DEFAULT 0,
    startTimestamp         TEXT  DEFAULT '',
    UNIQUE (tokenId),
    UNIQUE (auctionAccountId)
);

CREATE TABLE "bids"
(
    timestamp            TEXT PRIMARY KEY UNIQUE,
    auctionId            INTEGER references auctions,
    bidderAccountId      TEXT  DEFAULT '',
    bidAmount            INT8,
    status               TEXT  DEFAULT '',
    refunded             BOOLEAN DEFAULT false,
    refundTxId           TEXT  DEFAULT '',
    refundTxHash         TEXT  DEFAULT '',
    transactionId        TEXT  DEFAULT '',
    transactionHash      TEXT  DEFAULT ''
);
