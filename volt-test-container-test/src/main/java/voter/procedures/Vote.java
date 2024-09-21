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

/**
 * The Vote class represents a stored procedure for casting votes in a
 * voting system. The class extends VoltProcedure and includes methods
 * for checking contestant validity, voter limits, and state retrieval
 * based on the voter's phone number area code.
 */
public class Vote extends VoltProcedure {

    // potential return codes (synced with client app)

    /**
     * SQL statement to check if a contestant number exists in the `contestants` table.
     * This statement selects the contestant number from the database where the contestant number matches the provided value.
     * It is used to validate the existence of a contestant during the voting process.
     */
    public final SQLStmt checkContestantStmt = new SQLStmt(
            "SELECT contestant_number FROM contestants WHERE contestant_number = ?;");

    /**
     * SQL statement to check the number of votes associated with a particular phone number.
     * This statement selects the `num_votes` column from the `v_votes_by_phone_number` view
     * where the `phone_number` matches the provided value. It is used to determine how many
     * times a particular phone number has voted, ensuring the voter has not exceeded their
     * allowed number of votes.
     */
    public final SQLStmt checkVoterStmt = new SQLStmt(
            "SELECT num_votes FROM v_votes_by_phone_number WHERE phone_number = ?;");

    /**
     * This SQLStmt is used to check the state associated with a given area code.
     * It selects the state from the area_code_state table where the area code matches the provided parameter.
     */
    public final SQLStmt checkStateStmt = new SQLStmt(
            "SELECT state FROM area_code_state WHERE area_code = ?;");

    /**
     * The SQL statement to insert a vote into the 'votes' table.
     * This statement expects three parameters:
     * 1. phone_number: The phone number of the voter.
     * 2. state: The state from which the vote is being cast.
     * 3. contestant_number: The contestant number for which the vote is being cast.
     */
    public final SQLStmt insertVoteStmt = new SQLStmt(
            "INSERT INTO votes (phone_number, state, contestant_number) VALUES (?, ?, ?);");

    /**
     * Records a vote for a specified contestant from a given phone number.
     *
     * @param phoneNumber The phone number of the voter.
     * @param contestantNumber The ID of the contestant being voted for.
     * @param maxVotesPerPhoneNumber The maximum number of votes allowed per phone number.
     * @return A constant indicating the result of the voting operation:
     *         Constants.VOTE_SUCCESSFUL if the vote is recorded successfully,
     *         Constants.ERR_INVALID_CONTESTANT if the contestant ID is invalid,
     *         or Constants.ERR_VOTER_OVER_VOTE_LIMIT if the voter has exceeded the allowed number of votes.
     */
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
