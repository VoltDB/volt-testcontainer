-- Tell sqlcmd to batch the following commands together,
-- so that the schema loads quickly.
file -inlinebatch END_OF_BATCH

-- contestants table holds the contestants numbers (for voting) and names
CREATE TABLE KEYVALUE
(
    KEYNAME integer     NOT NULL
    , VALUE   varchar(50) NOT NULL
);

PARTITION TABLE KEYVALUE ON COLUMN KEYNAME;

CREATE PROCEDURE FROM CLASS ${package}.KeyValueInsert;

END_OF_BATCH
