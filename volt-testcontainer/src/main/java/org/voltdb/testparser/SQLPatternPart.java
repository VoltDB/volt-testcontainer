/*
 * Copyright (C) 2025-2026 Volt Active Data Inc.
 *
 * Use of this source code is governed by an MIT
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/MIT.
 */
package org.voltdb.testparser;

import java.util.regex.Pattern;

public abstract class SQLPatternPart
{
    int m_flags = 0;
    Integer m_minCount = null;
    Integer m_maxCount = null;

    abstract String generateExpression(int flagsAdd);
    abstract void setCaptureLabel(String captureLabel);
    abstract Pattern compile(String label);

    // Chainable methods for tweaking after construction.
    public SQLPatternPart withFlags(int flags)
    {
        m_flags |= flags;
        return this;
    }
}
