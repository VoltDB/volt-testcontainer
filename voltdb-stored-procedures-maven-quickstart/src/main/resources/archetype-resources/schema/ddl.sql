--
-- Copyright (C) 2025-2026 Volt Active Data Inc.
--
-- Use of this source code is governed by an MIT
-- license that can be found in the LICENSE file or at
-- https://opensource.org/licenses/MIT.
--

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
CREATE PROCEDURE FROM CLASS ${package}.CapitalizeAndPut;

END_OF_BATCH
