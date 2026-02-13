/*
 * Copyright (C) 2024 Volt Active Data Inc.
 *
 * Use of this source code is governed by an MIT
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/MIT.
 */

package voter.client1;

import org.junit.Assert;
import org.voltdb.CLIConfig;
import org.voltdb.VoltTable;
import org.voltdb.client.Client;
import org.voltdb.client.ClientResponse;
import org.voltdb.client.ClientStats;
import org.voltdb.client.ClientStatsContext;
import org.voltdb.client.NullCallback;
import org.voltdb.client.ProcCallException;
import org.voltdb.client.ProcedureCallback;
import voter.PhoneCallGenerator;
import voter.common.Constants;

import java.util.concurrent.atomic.AtomicLong;

public class VoterValidation {

    // Initialize some common constants and variables
    static final String CONTESTANT_NAMES_CSV =
            "Edwina Burnam,Tabatha Gehling,Kelly Clauss,Jessie Alloway," +
            "Alana Bregman,Jessie Eichman,Allie Rogalski,Nita Coster," +
            "Kurt Walser,Ericka Dieter,Loraine Nygren,Tania Mattioli";

    // handy, rather than typing this out several times
    static final String HORIZONTAL_RULE =
            "----------" + "----------" + "----------" + "----------" +
            "----------" + "----------" + "----------" + "----------" + "\n";

    // validated command line configuration
    final VoterConfig config;
    // Reference to the database connection we will use
    final Client client;
    // Phone number generator
    PhoneCallGenerator switchboard;
    // Statistics manager objects from the client
    final ClientStatsContext fullStatsContext;

    // voter benchmark state
    AtomicLong acceptedVotes = new AtomicLong(0);
    AtomicLong badContestantVotes = new AtomicLong(0);
    AtomicLong badVoteCountVotes = new AtomicLong(0);
    AtomicLong failedVotes = new AtomicLong(0);

    /**
     * Uses included {@link CLIConfig} class to
     * declaratively state command line options with defaults
     * and validation.
     */
    static class VoterConfig extends CLIConfig {
        int duration = 10;
        int warmup = 2;
        int contestants = 6;
        int maxvotes = 2;
        int ratelimit = Integer.MAX_VALUE;

        @Override
        public void validate() {
            if (duration <= 0) exitWithMessageAndUsage("duration must be > 0");
            if (warmup < 0) exitWithMessageAndUsage("warmup must be >= 0");
            if (contestants <= 0) exitWithMessageAndUsage("contestants must be > 0");
            if (maxvotes <= 0) exitWithMessageAndUsage("maxvotes must be > 0");
            if (ratelimit <= 0) exitWithMessageAndUsage("ratelimit must be > 0");
        }
    }

    /**
     * Constructor for benchmark instance.
     * Configures VoltDB client and prints configuration.
     *
     * @param config Parsed & validated CLI options.
     */
    public VoterValidation(Client client, VoterConfig config) {
        this.config = config;
        this.client = client;
        fullStatsContext = client.createStatsContext();

        switchboard = new PhoneCallGenerator(config.contestants);
    }

    /**
     * Prints the results of the voting simulation and statistics
     * about performance.
     *
     * @throws Exception if anything unexpected happens.
     */
    public synchronized void printResults() throws Exception {
        ClientStats stats = fullStatsContext.fetch().getStats();

        // 1. Voting Board statistics, Voting results and performance statistics
        String display = "\n" +
                         HORIZONTAL_RULE +
                         " Voting Results\n" +
                         HORIZONTAL_RULE +
                         "\nA total of %d votes were received...\n" +
                         " - %,9d Accepted\n" +
                         " - %,9d Rejected (Invalid Contestant)\n" +
                         " - %,9d Rejected (Maximum Vote Count Reached)\n" +
                         " - %,9d Failed (Transaction Error)\n\n";
        System.out.printf(display, stats.getInvocationsCompleted(),
                acceptedVotes.get(), badContestantVotes.get(),
                badVoteCountVotes.get(), failedVotes.get());

        // 2. Voting results
        VoltTable result = client.callProcedure("Results").getResults()[0];

        System.out.println("Contestant Name\t\tVotes Received");
        while(result.advanceRow()) {
            System.out.printf("%s\t\t%,14d\n", result.getString(0), result.getLong(2));
        }
        String winner = result.fetchRow(0).getString(0);
        System.out.printf("\nThe Winner is: %s\n\n", winner);
        Assert.assertTrue("Winner must be Edwina Burnam", winner.equals("Edwina Burnam"));
    }

    /**
     * Callback to handle the response to a stored procedure call.
     * Tracks response types.
     *
     */
    class VoterCallback implements ProcedureCallback {
        @Override
        public void clientCallback(ClientResponse response) throws Exception {
            if (response.getStatus() == ClientResponse.SUCCESS) {
                long resultCode = response.getResults()[0].fetchRow(0).getLong(0);
                if (resultCode == Constants.ERR_INVALID_CONTESTANT) {
                    badContestantVotes.incrementAndGet();
                }
                else if (resultCode == Constants.ERR_VOTER_OVER_VOTE_LIMIT) {
                    badVoteCountVotes.incrementAndGet();
                }
                else {
                    assert(resultCode == Constants.VOTE_SUCCESSFUL);
                    acceptedVotes.incrementAndGet();
                }
            }
            else {
                failedVotes.incrementAndGet();
            }
        }
    }

    /**
     * Core benchmark code.
     * Connect. Initialize. Run the loop. Cleanup. Print Results.
     *
     * @throws Exception if anything unexpected happens.
     */
    public void runBenchmark() throws Exception {
        // initialize using synchronous call
        System.out.println("\nRunning procedure invocations\n");
        client.callProcedure("Initialize", config.contestants, CONTESTANT_NAMES_CSV);
        // Run the benchmark loop for the requested warmup time
        // The throughput may be throttled depending on client configuration
        final long warmupEndTime = System.currentTimeMillis() + (1000l * config.warmup);
        while (warmupEndTime > System.currentTimeMillis()) {
            // Get the next phone call
            PhoneCallGenerator.PhoneCall call = switchboard.receive();

            // asynchronously call the "Vote" procedure
            client.callProcedure(new NullCallback(),
                                 "Vote",
                                 call.phoneNumber,
                                 call.contestantNumber,
                                 config.maxvotes);
        }

        // Run the benchmark loop for the requested duration
        // The throughput may be throttled depending on client configuration
        final long benchmarkEndTime = System.currentTimeMillis() + (1000l * config.duration);
        while (benchmarkEndTime > System.currentTimeMillis()) {
            // Get the next phone call
            PhoneCallGenerator.PhoneCall call = switchboard.receive();

            // asynchronously call the "Vote" procedure
            client.callProcedure(new VoterCallback(),
                                 "Vote",
                                 call.phoneNumber,
                                 call.contestantNumber,
                                 config.maxvotes);
        }

        // block until all outstanding txns return
        client.drain();

        // print the summary results
        try {
            printResults();
        } catch (ProcCallException e) {
            System.err.printf("WARNING: printResults failed, ProcCallException, did the database die?:" +e.getMessage());
        }

        // close down the client connections
        client.close();
    }

    /**
     * Main routine creates a benchmark instance and kicks off the run method.
     *
     * @throws Exception if anything goes wrong.
     * @see {@link VoterConfig}
     */
    public static void run(Client client) throws Exception {
        // create a configuration from the arguments
        VoterConfig config = new VoterConfig();

        VoterValidation benchmark = new VoterValidation(client, config);
        benchmark.runBenchmark();
    }
}