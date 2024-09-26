/*
 * Copyright (C) 2024 Volt Active Data Inc.
 *
 * Use of this source code is governed by an MIT
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/MIT.
 */

package voter.procedures;

import org.joda.time.LocalTime;
import org.joda.time.format.ISODateTimeFormat;
import org.voltdb.SQLStmt;
import org.voltdb.VoltProcedure;

/**
 * The JodaTimeInsert class extends VoltProcedure and provides a method to insert
 * a time string and its corresponding millisecond value into a database table.
 * This class uses the Joda-Time library to parse the input time string.
 */
public class JodaTimeInsert extends VoltProcedure {

    /**
     * SQL statement for inserting a time string and its corresponding millisecond value
     * into the 'jodatime' table. This statement expects two parameters: a timestring
     * and a timevalue, which are bound to the SQL statement at execution time.
     */
    public final SQLStmt insertVoteStmt = new SQLStmt(
            "INSERT INTO jodatime (timestring, timevalue) VALUES (?, ?);");

    /**
     * Parses an input time string, inserts it with its corresponding millisecond value
     * into a database table, and returns the millisecond value.
     *
     * @param time the time string to be parsed and inserted.
     * @return the millisecond value of the parsed time.
     */
    public long run(String time) {
        LocalTime localTime = LocalTime.parse(time);
        voltQueueSQL(insertVoteStmt, time, localTime.getMillisOfDay());
        voltExecuteSQL(true);
        return localTime.getMillisOfDay();
    }
}
