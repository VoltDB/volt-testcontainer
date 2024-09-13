/* This file is part of VoltDB.
 * Copyright (C) 2008-2024 Volt Active Data Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with VoltDB.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.voltdbtest.testcontainer;

import org.voltdb.VoltTable;
import org.voltdb.VoltType;
import org.voltdb.client.BatchTimeoutOverrideType;
import org.voltdb.client.Client;
import org.voltdb.client.ClientConfig;
import org.voltdb.client.ClientFactory;
import org.voltdb.client.ClientResponse;
import org.voltdb.client.ProcCallException;
import org.voltdb.parser.SQLLexer;
import org.voltdb.parser.SQLParser;
import org.voltdb.parser.SQLParser.FileInfo;
import org.voltdb.parser.SQLParser.FileOption;
import org.voltdb.utils.SplitStmtResults;

import java.io.EOFException;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.StringReader;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class SQLLoader {
    private static final String USER_HOME = userHome();

    private boolean m_hasBatchTimeout = true;
    private int m_batchTimeout = BatchTimeoutOverrideType.DEFAULT_TIMEOUT;
    private Charset m_charset = StandardCharsets.UTF_8;

    private final Client m_client;

    /**
     * <p>Constructor for SQLLoader.</p>
     *
     * @param client a {@link org.voltdb.client.Client} object
     */
    public SQLLoader(Client client) {
        this.m_client = client;
    }


    private static void printCatalogHeader(String name) {
        System.out.println("--- " + name + " " +
                String.join("", Collections.nCopies(57 - name.length(), "-")));
    }

    /**
     * Reads a script file (from a "file" directive) and executes
     * its content. Note that the "script file" could be an inline
     * batch, i.e., a "here document" that is coming from the same
     * input stream as the "file" directive.
     *
     * Called recursively for nested "file" directives.
     *
     * Package access for unit tests.
     */
    void executeScriptFiles(List<FileInfo> filesInfo, SQLCommandLineReader parentLineReader) {

        // In non-interactive code, except if we're called from the
        // DDL parser, echo the 'file' command.
        StringBuilder commandString = new StringBuilder();
        commandString.append('\n').append(filesInfo.get(0).toString());
        for (int ii = 1; ii < filesInfo.size(); ii++) {
            commandString.append(' ').append(filesInfo.get(ii).getFile().toString());
        }
        System.out.println(commandString.toString());

        // Loop through files. Operation depends on whether we're
        // in 'batch mode' (in which case we collect all file content
        // prior to any execution) or not (in which case we execute
        // statements from each file as we find it).
        StringBuilder batchStmts = new StringBuilder();
        for (int ii = 0; ii < filesInfo.size(); ii++) {
            FileInfo fileInfo = filesInfo.get(ii);
            LineReaderAdapter adapter = null;
            SQLCommandLineReader reader = null;

            try {
                if (fileInfo.getOption() == FileOption.INLINEBATCH) {
                    // File command is a "here document" so pass in the current
                    // input stream.
                    reader = parentLineReader;
                }
                else {
                    // Get a reader for this file
                    FileInputStream fis = new FileInputStream(fileInfo.getFile());
                    reader = adapter = new LineReaderAdapter(new InputStreamReader(fis, m_charset));

                    // Batch mode: collect content of this file, and add to the batch
                    if (fileInfo.getOption() == FileOption.BATCH) {
                        String line;
                        while ((line = reader.readBatchLine()) != null) {
                            batchStmts.append(line).append('\n');
                        }
                        adapter.close();
                        adapter = null;
                        reader = null; // prevent immediate execution

                        // If this is the last file, create a reader to read from the
                        // collected batch of statements. 'fileInfo' is set for this
                        // last file, if the batch has multiple files.
                        if (ii == filesInfo.size() - 1) {
                            reader = adapter = new LineReaderAdapter(new StringReader(batchStmts.toString()));
                        }
                    }
                }

                // Execute script
                if (reader != null) {
                    executeScriptFromReader(fileInfo, reader);
                }
            }
            catch (FileNotFoundException ex) {
                System.err.printf("Script file '%s' could not be found.\n", fileInfo.getFile());
                stopOrContinue(ex);
                break; // abandon the file loop
            }
            catch (Exception ex) {
                stopOrContinue(ex);
            }
            finally {
                if (adapter != null) {
                    adapter.close();
                    adapter = null;
                }
            }
        }
    }

    /**
     * Execute script from a supplied reader. This is used for
     * 'file' directives and for handling primary input when
     * sqlcmd is executed in non-interactive mode.
     *
     * The input may be stdin in two cases: sqlcmd is executing
     * in non-interactive mode (likely 'sqlcmd <script'), or
     * when a 'file' directive in interactive mode uses a
     * here-document.
     *
     * The 'reader' is expected to have been opened using an
     * appropriate charset for input conversion.
     */
    private void executeScriptFromReader(FileInfo fileInfo, SQLCommandLineReader reader)
        throws Exception {
        if (reader == null) {
            return; // nothing to see here
        }

        StringBuilder statement = new StringBuilder();
        boolean statementStarted = false;
        StringBuilder batch = fileInfo.isBatch() ? new StringBuilder() : null;

        String delimiter = (fileInfo.getOption() == FileOption.INLINEBATCH) ?
            fileInfo.getDelimiter() : null;

        // Loop by lines
        while (true) {
            String line = reader.readBatchLine();

            // Inline batch? (a here document)
            if (delimiter != null) {
                if (line == null) {
                    // We only print this nice message if the inline batch is
                    // being executed non-interactively. For an inline batch
                    // entered from the command line, SQLConsoleReader catches
                    // ctrl-D and exits the process before this code can execute,
                    // even if this code is in a "finally" block.
                    throw new EOFException("ERROR: Failed to find delimiter \"" + delimiter +
                             "\" indicating end of inline batch. No batched statements were executed.");
                }
                if (delimiter.equals(line)) {
                    line = null;
                }
            }

            // No more lines? Execute whatever we got.
            if (line == null) {
                if (batch == null) {
                    String statementString = statement.toString();
                    // Trim here avoids a "missing statement" error from adhoc in an edge case
                    // like a blank line from stdin.
                    if (!statementString.trim().isEmpty()) {
                        executeStatements(statementString, reader.getLineNumber());
                    }
                }
                else {
                    batch.append(statement);
                    if (batch.length() > 0) {
                        String batchName = fileInfo.getFilePath();
                        if (fileInfo.getFileSequence() > 1) {
                            batchName = "(file input)";
                        }
                        executeDDLBatch(batchName, batch.toString(), reader.getLineNumber());
                    }
                }
                return;
            }

            // Handle sqlcmd directives, but not in the middle of
            // collecting a SQL statement,
            if (!statementStarted) {
                if (line.trim().isEmpty() || SQLParser.isWholeLineComment(line)) {
                    // We don't strictly have to include a blank line or whole-line
                    // comment at the start of a statement, but when we want to preserve line
                    // numbers (in a batch), we should at least append a newline.
                    // Whether to echo comments or blank lines from a batch is
                    // a grey area.
                    if (batch != null) {
                        statement.append(line).append("\n");
                    }
                    continue;
                }

                // Recursively process FILE commands, any failure will cause a recursive failure
                List<FileInfo> nestedFilesInfo = SQLParser.parseFileStatement(fileInfo, line);
                if (nestedFilesInfo != null) {
                    // Guards must be added for FILE Batch containing batches.
                    if (batch != null) {
                        stopOrContinue(new RuntimeException("A FILE command is invalid in a batch."));
                        continue; // continue execution, just ignoring the FILE command
                    }

                    // Execute the file content or fail to do so.
                    executeScriptFiles(nestedFilesInfo, reader);
                    continue;
                }
            }

            // Process normal @AdHoc commands which may be
            // multi-line-statement continuations.
            statement.append(line).append("\n");

            // Check if the current statement ends here and now.
            // if it is an incomplete multi statement procedure, it is returned back
            if (SQLParser.isSemiColonTerminated(line)) {
                String statementString = statement.toString();
                if (batch == null) {
                    String incompleteStmt = executeStatements(statementString, reader.getLineNumber());
                    if (incompleteStmt != null) {
                        statement = new StringBuilder(incompleteStmt);
                        // TODO: should statementStarted be set true here?
                    } else {
                        statement.setLength(0);
                        statementStarted = false;
                    }
                } else { // when in a batch:
                    SplitStmtResults splitResults = SQLLexer.splitStatements(statementString);
                    if (splitResults.getIncompleteStmt() == null) {
                        // not in the middle of a statement.
                        statementStarted = false;
                        batch.append(statement);
                        statement.setLength(0);
                    } else {
                        int incompleteStmtOffset = splitResults.getIncompleteStmtOffset();
                        statementStarted = true;
                        if (incompleteStmtOffset != 0) {
                            batch.append(statementString.substring(0, incompleteStmtOffset));
                            statement = new StringBuilder(statementString.substring(incompleteStmtOffset));
                        }
                    }
                }
            } else {
                // Disable directive processing until end of statement.
                statementStarted = true;
            }
        }
    }

    /**
     * Process batch of DDL statements (presented as a single string).
     * Batching is only supported for DDL.
     *
     * 'batchFileName' may not be an actual file name; it is
     * used only in error messages.
     *
     * All exceptions are swallowed here. If m_stopOnError is set
     * then a StopException will be thrown, which is expected to
     * bubble all the way up the stack. Otherwise we make a normal
     * return-to-caller.
     */
    private void executeDDLBatch(String batchFileName, String statements,
                                 int batchEndLineNumber) {
        try {
            System.out.println();
            System.out.println(statements);

            if (!SQLParser.appearsToBeValidDDLBatch(statements)) {
                throw new RuntimeException("Error: This batch begins with a non-DDL statement.  "
                        + "Batching is only supported for DDL.");
            }

            ClientResponse response = m_client.callProcedure("@AdHoc", statements);
            if (response.getStatus() != ClientResponse.SUCCESS) {
                throw new Exception("Execution Error: " + response.getStatusString());
            }

            // Assert the current DDL AdHoc batch call behavior
            assert(response.getResults().length == 1);
            System.out.println("Batch command succeeded.");
        }
        catch (ProcCallException ex) {
            String fixedMessage = patchErrorMessageWithFile(batchFileName, ex.getMessage());
            stopOrContinue(new RuntimeException(fixedMessage));
        }
        catch (Exception ex) {
            stopOrContinue(ex);
        }
    }

    private static String patchErrorMessageWithFile(String batchFileName, String message) {
        Pattern errorMessageFilePrefix = Pattern.compile("\\[.*:([0-9]+)\\]");
        Matcher matcher = errorMessageFilePrefix.matcher(message);
        if (matcher.find()) {
            // This won't work right if the filename contains a "$"...
            message = matcher.replaceFirst("[" + batchFileName + ":$1]");
        }
        return message;
    }

    /**
     * Executes a string of complete statements.
     * Exception handling depends on m_stopOnError:
     * - If true (the default) a StopException is thrown
     * - Otherwise it's reported, and execution continues
     * Any terminal incomplete statement is returned.
     */
    private String executeStatements(String statements, int lineNum) {
        SplitStmtResults parsedOutput = SQLLexer.splitStatements(statements);
        List<String> parsedStatements = parsedOutput.getCompletelyParsedStmts();
        for (String statement : parsedStatements) {
            try {
                executeStatement(statement, lineNum);
            }
            catch (Exception ex) {
                stopOrContinue(ex);
            }
        }
        return parsedOutput.getIncompleteStmt();
    }

    // Unit tests override this
    /**
     * <p>executeStatement.</p>
     *
     * @param statement a {@link java.lang.String} object
     * @param lineNum a int
     * @throws java.lang.Exception if any.
     */
    protected void executeStatement(String statement, int lineNum) throws Exception {

        System.out.println();
        System.out.println(statement + ";");

        // DDL statements get forwarded to @AdHoc,
        // but get special post-processing to reload stored procedures
        if (SQLParser.queryIsDDL(statement)) {
            printDdlResponse(m_client.callProcedure("@AdHoc", statement));
            return;
        }

        // All other commands get forwarded to @AdHoc
        printResponse(callProcedureHelper("@AdHoc", statement), true);
    }

    /**
     * On exception, determines whether to stop execution
     * or continue. 'Stop' is indicated by throwing a
     * specific exception. In interactive mode, this
     * causes return to the top-level prompt.
     */
    private void stopOrContinue(Exception ex) {
        // Display error message, set exit code (even if we are not going
        // to exit immediately) and possibly signal a stop.
        printExceptionMessage(ex);
    }

    private void printExceptionMessage(Exception ex) {
        String msg = ex.getMessage();
        if (msg == null || msg.isEmpty()) {
            msg = ex.getClass().getName();
        }
        System.err.println(msg);
    }

    /**
     * Output generation. Output is either to standard output or
     * to a named output file, in both cases mediated by the
     * user's choice of formatter (fixed, csv, or tab-delimited)
     *
     * In the case of a named output file, the file may have been
     * opened with a user-specified charset.
     *
     * The printResponseToStdout variant is available for cases
     * that should never use the named file even if configured.
     */
    private void printResponse(ClientResponse response, boolean suppressTableOutputForDML) throws Exception {
        doPrintResp(System.out, response, suppressTableOutputForDML);
    }

    private void printResponseToStdout(ClientResponse response, boolean suppressTableOutputForDML) throws Exception {
        doPrintResp(System.out, response, suppressTableOutputForDML);
    }

    private void doPrintResp(PrintStream ps, ClientResponse response, boolean suppressTableOutputForDML) throws Exception {
        if (response.getStatus() != ClientResponse.SUCCESS) {
            throw new Exception("Execution Error: " + response.getStatusString());
        }
        for (VoltTable t : response.getResults()) {
            if (suppressTableOutputForDML && isUpdateResult(t))
                continue;
            ps.println(t.toFormattedString(true));
        }
    }

    private static boolean isUpdateResult(VoltTable table) {
        return (table.getColumnName(0).isEmpty() || table.getColumnName(0).equals("modified_tuples")) &&
                table.getRowCount() == 1 &&
                table.getColumnCount() == 1 &&
                table.getColumnType(0) == VoltType.BIGINT;
    }

    private static void printDdlResponse(ClientResponse response) throws Exception {
        if (response.getStatus() != ClientResponse.SUCCESS) {
            throw new Exception("Execution Error: " + response.getStatusString());
        }
        else {
            System.out.println("Command succeeded.");
        }
    }

    /**
     * VoltDB client setup.
     * Fails only if we can't connect to any server.
     */
    private Client getClient(ClientConfig config, String[] servers, int port) throws Exception {
        Client client = ClientFactory.createClient(config);
        boolean connectedAnyServer = false;
        StringBuilder connectionErrorMessages = new StringBuilder();
        for (String server : servers) {
            try {
                client.createConnection(server.trim(), port);
                connectedAnyServer = true;
            }
            catch (UnknownHostException e) {
                connectionErrorMessages.append("\n    ")
                        .append(server.trim()).append(':').append(port)
                        .append(" - UnknownHostException");
            }
            catch (IOException e) {
                connectionErrorMessages.append("\n    ")
                        .append(server.trim()).append(':').append(port)
                        .append(" - ").append(e.getMessage());
            }
        }
        if (!connectedAnyServer) {
            closeClient(client);
            throw new IOException("Unable to connect to the cluster" + connectionErrorMessages);
        }
        return client;
    }

    private static void closeClient(Client client) {
        try {
            client.close();
        }
        catch (Exception ex) {
            // ignored
        }
    }

    private ClientResponse callProcedureHelper(String procName, Object... parameters)
            throws IOException, ProcCallException {
        ClientResponse response;
        if (m_hasBatchTimeout) {
            response = m_client.callProcedureWithTimeout(m_batchTimeout, procName, parameters);
        } else {
            response = m_client.callProcedure(procName, parameters);
        }
        return response;
    }

    /**
     * General application support. Note that merely
     * printing usage (or help text) is not an error,
     * so we don't set the exit code.
     */
    private void printUsage() {
        System.out.println(
        "Usage: sqlcmd --help\n"
        + "   or  sqlcmd [--servers=server-list]\n"
        + "              [--port=port-number]\n"
        + "              [--user=user]\n"
        + "              [--password=password]\n"
        + "              [--credentials=properties-file]\n"
        + "              [--kerberos or --kerberos=jaas-entry-name]\n"
        + "              [--ssl or --ssl=properties-or-truststore-file]\n"
        + "              [--charset=charset-name]\n"
        + "              [--query=query-string]\n"
        + "              [--file=filename]\n"
        + "              [--batch]\n"
        + "              [--output-format=(fixed|csv|tab)]\n"
        + "              [--output-file=filename]\n"
        + "              [--output-skip-metadata]\n"
        + "              [--stop-on-error=(true|false)]\n"
        + "              [--query-timeout=milliseconds]\n"
        + "\n"
        + "[--servers=server-list]\n"
        + "  Comma-separated list of servers to connect to.\n"
        + "  Default: localhost.\n"
        + "\n"
        + "[--port=port-number]\n"
        + "  Client port to connect to on cluster nodes.\n"
        + "  Default: 21212.\n"
        + "\n"
        + "[--user=user]\n"
        + "  Name of the user for database login.\n"
        + "  Default: (not defined - connection made without credentials).\n"
        + "\n"
        + "[--password=password]\n"
        + "  Password of the user for database login.\n"
        + "  Default: (not defined - connection made without credentials).\n"
        + "\n"
        + "[--credentials=properties-file]\n"
        + "  File that contains username and password information.\n"
        + "  Default: (not defined - connection made without credentials).\n"
        + "\n"
        + "[--kerberos[=jaas-entry-name]]\n"
        + "  Enable kerberos authentication for user login by specifying the\n"
        + "  JAAS login configuration file entry name, or 'VoltDBClient' if\n"
        + "  no entry name is given.\n"
        + "  Default: (not defined - connection made without credentials).\n"
        + "\n"
        + "[--ssl[=properties-file]]\n"
        + "  Enable TLS/SSL for server communication with truststore specified\n"
        + "  in the named properties file, or with system default truststore.\n"
        + "  Default: TLS/SSL not enabled.\n"
        + "\n"
        + "[--charset=charset-name]]\n"
        + "  Use the named character set for input via 'file' command or the\n"
        + "  '--file' option, and for result output when the '--output-file'\n"
        + "  option is used.\n"
        + "  Default: Use the UTF-8 character set.\n"
        + "\n"
        + "[--query=query-string]\n"
        + "  Execute a non-interactive query. Multiple query options are allowed.\n"
        + "  Default: (runs the interactive shell when no query options are present).\n"
        + "\n"
        + "[--file=filename]\n"
        + "  Executes a sqlcmd 'FILE' operation from the command line.\n"
        + "  Default: (runs the interactive shell when no file option is present).\n"
        + "\n"
        + "[--batch]\n"
        + "  In conjunction with the --file option, executes the file in batch mode.\n"
        + "  Default: (executes the file in statement-by-statement mode).\n"
        + "\n"
        + "[--output-format=(fixed|csv|tab)]\n"
        + "  Format of returned resultset data (fixed-width, CSV or tab-delimited).\n"
        + "  Default: fixed.\n"
        + "\n"
        + "[--output-file=filename]\n"
        + "  Specifies that formatted resultset data are written to the named file.\n"
        + "  Default: (formatted data are written to standard output).\n"
        + "\n"
        + "[--output-skip-metadata]\n"
        + "  Removes metadata information such as column headers and row count from\n"
        + "  produced output. Default: metadata output is enabled.\n"
        + "\n"
        + "[--stop-on-error=(true|false)]\n"
        + "  Causes the utility to stop immediately or continue after detecting an error.\n"
        + "  In interactive mode, a value of \"true\" discards any unprocessed input\n"
        + "  and returns to the command prompt. Default: true.\n"
        + "\n"
        + "[--query-timeout=milliseconds]\n"
        + "  Read-only queries that take longer than this number of milliseconds will abort.\n"
        + "  Default: " + BatchTimeoutOverrideType.DEFAULT_TIMEOUT/1000.0 + " seconds.\n"
        + "\n"
        );
    }

    private static String userHome() {
        String home = System.getProperty("user.home");
        if (home == null || home.isEmpty() || home.equals("?")) {
            home = System.getenv("HOME");
            if (home == null || home.isEmpty()) {
                home = "/tmp";
            }
        }
        return home;
    }

    private static String expandTilde(String s) {
        return s.startsWith("~/") ? s.replace("~", USER_HOME) : s;
    }

    /**
     * This is the main entry point into any given SQLCommand
     * instance. main() itself creates an instance and calls
     * this method. Absent catastrophic failure of the JVM,
     * this method always returns; it catches all exceptions,
     * and it never calls System.exit().
     *
     * The return value is non-zero for an error that results in
     * immediate return. Otherwise, if we encoutered an earlier
     * error, m_exitCode will be set (see method stopOrContinue)
     * and we use that as the return code. If there was no such
     * error, then m_exitCode will still be zero.
     *
     * @param args a {@link java.lang.String} object
     * @return a int
     */
    public int execute(String... args) {
        TimeZone.setDefault(TimeZone.getTimeZone("GMT+0"));

        // Parameters from command line
        String inputFilePath = "";
        boolean inputBatch = false;

        // Parse out parameters.
        for (String arg : args) {
            String key = "", val = "";
            boolean hasValue = false, recognized = false;

            try {
                // Split into key and value around equals sign
                key = arg.substring(2);
                if (key.contains("=")) {
                    String[] split = key.split(" *= *", 2);
                    key = split[0]; val = split[1];
                    hasValue = true; // even if empty
                }

                // Dispose of cases that never have a value or are allowed to
                // have no value (either way, no value was actually given).
                // Please keep the switch cases in the same order as
                // the 'usage' text for ease of checking for consistency.
                if (!hasValue) {
                    recognized = true; // assumed
                    switch (key) {
                    case "batch":
                        inputBatch = true;
                        break;
                    default: // may be a valid key requring a value, or entirely unknown
                        recognized = false;
                        break;
                    }
                }

                // If we have not so far recognized the parameter, then see if
                // it's one that requires a value. The value may or may not have
                // been provided. We'll deal with that later after recognizing
                // (or not) the key. Please keep the switch cases in the same
                // order as the 'usage' text for ease of checking for consistency.
                if (!recognized) {
                    switch (key) {
                    case "file":
                        inputFilePath = val;
                        break;
                    default:
                        recognized = false;
                        break;
                    }
                }
            }
            catch (NumberFormatException ex) {
                return -1;
            }
        }

        // Check for option conflicts
        int nAuth = 0;
        try {
            // Command-line file input; just like a 'FILE file' command
             if (!inputFilePath.isEmpty()) {
                List<FileInfo> files = Collections.singletonList(new FileInfo(inputFilePath, inputBatch));
                executeScriptFiles(files, null);
            }
        } catch (Exception ex) {
            printExceptionMessage(ex);
            return -1;
        }
        return 0;
    }
}
