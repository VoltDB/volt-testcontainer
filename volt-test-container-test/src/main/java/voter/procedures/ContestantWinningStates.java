/*
 * Copyright (C) 2024 Volt Active Data Inc.
 *
 * Use of this source code is governed by an MIT
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/MIT.
 */

//
// Returns the N first winning states for each contestant
//

package voter.procedures;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

import org.voltdb.SQLStmt;
import org.voltdb.VoltProcedure;
import org.voltdb.VoltTable;
import org.voltdb.VoltType;

public class ContestantWinningStates extends VoltProcedure
{
    public final SQLStmt resultStmt = new SQLStmt(
            "SELECT contestant_number, state, SUM(num_votes) AS num_votes " +
            "FROM v_votes_by_contestant_number_state " +
            "GROUP BY contestant_number, state " +
            "ORDER BY 2 ASC, 3 DESC, 1 ASC;");

    static class Result {
        public final String state;
        public final long votes;

        public Result(String state, long votes) {
            this.state = state;
            this.votes = votes;
        }
    }

    static class OrderByVotesDesc implements Comparator<Result>
    {
        @Override
        public int compare(Result a, Result b) {
            long numVotesA = a.votes;
            long numVotesB = b.votes;

            if (numVotesA > numVotesB) return -1;
            if (numVotesA < numVotesB) return 1;
            return 0;
        }
    }

    public VoltTable run(int contestantNumber, int max)
    {
        ArrayList<Result> results = new ArrayList<Result>();
        voltQueueSQL(resultStmt);
        VoltTable summary = voltExecuteSQL()[0];
        String state = "";
        while(summary.advanceRow()) {
            if (!summary.getString(1).equals(state)) {
                state = summary.getString(1);
                if (summary.getLong(0) == contestantNumber)
                    results.add(new Result(state, summary.getLong(2)));
            }
        }
        Result[] resultArray = (Result[])results.toArray();
        Arrays.sort(resultArray, new OrderByVotesDesc());
        VoltTable result = new VoltTable(
                new VoltTable.ColumnInfo("state",VoltType.STRING),
                new VoltTable.ColumnInfo("num_votes",VoltType.BIGINT));
        for(int i=0;i<Math.min(resultArray.length,max);i++)
            result.addRow(new Object[] {
                    resultArray[i].state,
                    resultArray[i].votes });
        return result;
    }
}
