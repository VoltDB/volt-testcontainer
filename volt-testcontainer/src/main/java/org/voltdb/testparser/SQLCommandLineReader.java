/*
 * Copyright (C) 2024 Volt Active Data Inc.
 *
 * Use of this source code is governed by an MIT
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/MIT.
 */

package org.voltdb.testparser;

import java.io.IOException;

/**
 * This class provides a common interface for executing sqlcmd's file
 * command both for files on a disk or "here documents" entered
 * interactively.
 */
interface SQLCommandLineReader {
    /**
     * Read the next line of input from some underlying input stream.
     * If the underlying stream is interactive, print out the prompt.
     *
     * @return a {@link java.lang.String} object
     * @throws java.io.IOException if any.
     */
    String readBatchLine() throws IOException;

    /**
     * Return the line number of the most recently read line.
     *
     * @return a int
     */
    int getLineNumber();
}
