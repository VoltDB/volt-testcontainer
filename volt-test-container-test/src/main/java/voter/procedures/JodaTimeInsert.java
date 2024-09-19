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

public class JodaTimeInsert extends VoltProcedure {

    public final SQLStmt insertVoteStmt = new SQLStmt(
            "INSERT INTO jodatime (timestring, timevalue) VALUES (?, ?);");

    public long run(String time) {
        LocalTime localTime = LocalTime.parse(time);
        voltQueueSQL(insertVoteStmt, time, localTime.getMillisOfDay());
        voltExecuteSQL(true);
        return localTime.getMillisOfDay();
    }
}
