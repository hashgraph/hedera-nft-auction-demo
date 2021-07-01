CREATE TABLE validators
(
    name                   TEXT  NOT NULL UNIQUE,
    url                    TEXT  DEFAULT '',
    publicKey              TEXT  DEFAULT ''
);
