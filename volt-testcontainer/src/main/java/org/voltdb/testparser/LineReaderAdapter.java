/*
 * Copyright (C) 2024 Volt Active Data Inc.
 *
 * Use of this source code is governed by an MIT
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/MIT.
 */
package org.voltdb.testparser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;

/**
 * Wraps a reader into an SQLCommandLineReader. Can also keep track of line numbers.
 *
 * @author jcrump
 */
class LineReaderAdapter implements SQLCommandLineReader {
    private final BufferedReader m_reader;
    private int m_lineNum = 0;

    /**
     * {@inheritDoc}
     */
    @Override
    public int getLineNumber() {
        return m_lineNum;
    }

    /**
     * <p>Constructor for LineReaderAdapter.</p>
     *
     * @param reader a {@link java.io.Reader} object
     */
    public LineReaderAdapter(Reader reader) {
        m_reader = new BufferedReader(reader);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String readBatchLine() throws IOException {
        m_lineNum++;
        return m_reader.readLine();
    }

    /**
     * <p>close.</p>
     */
    public void close() {
        try {
            m_reader.close();
        } catch (IOException e) {
        }
    }
}
