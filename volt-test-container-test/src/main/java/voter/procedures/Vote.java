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
import voter.common.Constants;

import static voter.common.Constants.ERR_INVALID_CONTESTANT;

public class Vote extends VoltProcedure {

    // potential return codes (synced with client app)

    // Checks if the vote is for a valid contestant
    public final SQLStmt checkContestantStmt = new SQLStmt(
            "SELECT contestant_number FROM contestants WHERE contestant_number = ?;");

    // Checks if the voter has exceeded their allowed number of votes
    public final SQLStmt checkVoterStmt = new SQLStmt(
            "SELECT num_votes FROM v_votes_by_phone_number WHERE phone_number = ?;");

    // Checks an area code to retrieve the corresponding state
    public final SQLStmt checkStateStmt = new SQLStmt(
            "SELECT state FROM area_code_state WHERE area_code = ?;");

    // Records a vote
    public final SQLStmt insertVoteStmt = new SQLStmt(
            "INSERT INTO votes (phone_number, state, contestant_number) VALUES (?, ?, ?);");

    public long run(long phoneNumber, int contestantNumber, long maxVotesPerPhoneNumber) {

        // Queue up validation statements
        voltQueueSQL(checkContestantStmt, EXPECT_ZERO_OR_ONE_ROW, contestantNumber);
        voltQueueSQL(checkVoterStmt, EXPECT_ZERO_OR_ONE_ROW, phoneNumber);
        voltQueueSQL(checkStateStmt, EXPECT_ZERO_OR_ONE_ROW, (short)(phoneNumber / 10000000l));
        VoltTable validation[] = voltExecuteSQL();

        if (validation[0].getRowCount() == 0) {
            return Constants.ERR_INVALID_CONTESTANT;
        }

        if ((validation[1].getRowCount() == 1) &&
                (validation[1].asScalarLong() >= maxVotesPerPhoneNumber)) {
            return Constants.ERR_VOTER_OVER_VOTE_LIMIT;
        }

        // Some sample client libraries use the legacy random phone generation that mostly
        // created invalid phone numbers. Until refactoring, re-assign all such votes to
        // the "XX" fake state (those votes will not appear on the Live Statistics dashboard,
        // but are tracked as legitimate instead of invalid, as old clients would mostly get
        // it wrong and see all their transactions rejected).
        final String state = (validation[2].getRowCount() > 0) ? validation[2].fetchRow(0).getString(0) : "XX";

        // Post the vote
        voltQueueSQL(insertVoteStmt, EXPECT_SCALAR_MATCH(1), phoneNumber, state, contestantNumber);
        voltExecuteSQL(true);

        // Set the return value to 0: successful vote
        return Constants.VOTE_SUCCESSFUL;
    }
}
