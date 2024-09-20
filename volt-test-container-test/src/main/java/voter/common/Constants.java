/*
 * Copyright (C) 2024 Volt Active Data Inc.
 *
 * Use of this source code is governed by an MIT
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/MIT.
 */

package voter.common;

/**
 * The Constants class contains return code constants for the voting procedure.
 * These constants are used to indicate the result of a voting operation.
 */
public class Constants {
    /**
     * Indicates a successful vote.
     * This constant is used to signify that the voting operation was completed without any errors
     * and the vote has been successfully recorded.
     */
    public static final long VOTE_SUCCESSFUL = 0;
    /**
     * Error code indicating an invalid contestant.
     * This constant is used to signify that the provided contestant ID does not exist or is not valid
     * in the context of the current voting operation.
     */
    public static final long ERR_INVALID_CONTESTANT = 1;
    /**
     * Error code indicating the voter has exceeded their maximum allowed number of votes.
     * This constant is used to signify that the voting operation cannot be completed
     * because the voter has already cast the maximum number of votes allowed by the system.
     */
    public static final long ERR_VOTER_OVER_VOTE_LIMIT = 2;
}
