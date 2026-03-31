/*
 * Copyright (C) 2024 Volt Active Data Inc.
 *
 * Use of this source code is governed by an MIT
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/MIT.
 */
package org.voltdb.testparser;
import java.util.List;

/**
 * To store the statements which are parsed/split by the SQLLexer.splitStatements().
 * Currently the incompleteStmt is used to store statements that are not complete,
 * so that more user input can be appended at the end of it.
 */
public class SplitStmtResults {
    private final List<String> m_completelyParsedStmts;
    private final String m_incompleteStmt;
    private final int m_incompleteStmtOffset;

    public SplitStmtResults(List<String> completelyParsedStmts, String incompleteStmt, int incompleteStmtOffset) {
        m_completelyParsedStmts = completelyParsedStmts;
        m_incompleteStmt = incompleteStmt;
        m_incompleteStmtOffset = incompleteStmtOffset;
    }

    public String getIncompleteStmt() {
        return m_incompleteStmt;
    }

    public List<String> getCompletelyParsedStmts() {
        return m_completelyParsedStmts;
    }

    public int getIncompleteStmtOffset() {
        return m_incompleteStmtOffset;
    }
}
