/* This file is part of VoltDB.
 * Copyright (C) 2024 Volt Active Data Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS BE LIABLE FOR ANY CLAIM, DAMAGES OR
 * OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */



//
// Accepts a vote, enforcing business logic: make sure the vote is for a valid
// contestant and that the voter (phone number of the caller) is not above the
// number of allowed votes.
//

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
