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

/**
 * The Results class extends the VoltProcedure class to implement a procedure for
 * retrieving contest results in terms of total votes received by each contestant.
 *
 * The resultStmt field is an SQL statement that joins the `v_votes_by_contestant_number_state`
 * and `contestants` tables to fetch the contestant's name, contestant number, and
 * the sum of votes received by each contestant. The results are aggregated by contestant
 * name and number, then ordered by total votes in descending order and by contestant number
 * and name in ascending order.
 *
 * The run method queues the resultStmt SQL statement for execution and returns the
 * execution results as an array of VoltTable objects.
 */
public class Results extends VoltProcedure
{
    /**
     * The SQL statement used to retrieve contest results, including the contestant's name,
     * contestant number, and total votes received. The statement joins the `v_votes_by_contestant_number_state`
     * table with the `contestants` table, groups the results by contestant name and number,
     * and orders the results by total votes in descending order, then by contestant number
     * and name in ascending order.
     */
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
    /**
     * Executes the predefined SQL statement to retrieve the contest results
     * in terms of total votes received by each contestant.
     *
     * @return an array of VoltTable objects containing the results of the executed SQL statement
     */
    public VoltTable[] run() {
        voltQueueSQL(resultStmt);
        return voltExecuteSQL(true);
    }
}
