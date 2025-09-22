-- This is how you batch
file -inlinebatch END_OF_BATCH

-- Create the table you need
CREATE TABLE KEYVALUE
(
    KEYNAME integer     NOT NULL
    , VALUE   varchar(5000) NOT NULL
);

-- Partition the table
PARTITION TABLE KEYVALUE ON COLUMN KEYNAME;

-- Create the procedure from classes you just wrote
CREATE PROCEDURE FROM CLASS ${package}.Put;
CREATE PROCEDURE FROM CLASS ${package}.Get;

END_OF_BATCH
