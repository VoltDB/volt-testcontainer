/*
 * Copyright (C) 2024 Volt Active Data Inc.
 *
 * Use of this source code is governed by an MIT
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/MIT.
 */

package voter.procedures;

import org.voltdb.SQLStmt;
import org.voltdb.VoltProcedure;
import org.voltdb.VoltTable;

public class Results extends VoltProcedure
{
    // Gets the results
    public final SQLStmt resultStmt = new SQLStmt( "   SELECT a.contestant_name   AS contestant_name"
                                                 + "        , a.contestant_number AS contestant_number"
                                                 + "        , SUM(b.num_votes)    AS total_votes"
                                                 + "     FROM v_votes_by_contestant_number_state AS b"
                                                 + "        , contestants AS a"
                                                 + "    WHERE a.contestant_number = b.contestant_number"
                                                 + " GROUP BY a.contestant_name"
                                                 + "        , a.contestant_number"
                                                 + " ORDER BY total_votes DESC"
                                                 + "        , contestant_number ASC"
                                                 + "        , contestant_name ASC;" );
    public VoltTable[] run() {
        voltQueueSQL(resultStmt);
        return voltExecuteSQL(true);
    }
}
