/*
 * Copyright (C) 2024 Volt Active Data Inc.
 *
 * Use of this source code is governed by an MIT
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/MIT.
 */


//
// Returns the heat map data (winning contestant by state) for display on nthe Live Statistics dashboard.
//

package voter.procedures;

import java.util.ArrayList;

import org.voltdb.SQLStmt;
import org.voltdb.VoltProcedure;
import org.voltdb.VoltTable;
import org.voltdb.VoltType;

/**
 * This procedure returns two VoltTables as results.  The first is a list of states,
 * the contestant numbers of the winner in that state and the total number of votes for that
 * contestant in that state.  The second is a table of contestants and all votes
 * for that contestant globally.
 */
public class GetStateHeatmap extends VoltProcedure {

    public final SQLStmt stateHeatMap = new SQLStmt(
                                                    "  SELECT state"                                                     +
                                                    "       , contestant_number"                                         +
                                                    "       , num_votes"                                                 +
                                                    "    FROM ( SELECT state"                                            +
                                                    "                , contestant_number"                                +
                                                    "                , num_votes"                                        +
                                                    "                , RANK() OVER ( PARTITION by state "                +
                                                    "                                ORDER BY num_votes DESC ) AS vrank" +
                                                    "             FROM v_votes_by_contestant_number_state ) AS sub"      +
                                                    "   WHERE sub.vrank = 1;");
    public final SQLStmt contestantTotals = new SQLStmt("   SELECT contestant_number"                                    +
                                                        "        , SUM(num_votes)"                                       +
                                                        "     FROM v_votes_by_contestant_number_state"                   +
                                                        " GROUP BY contestant_number;");

    public VoltTable[] run() {
        voltQueueSQL(stateHeatMap);
        voltQueueSQL(contestantTotals);
        VoltTable[] result = voltExecuteSQL();
        return result;
    }
}
