/*
 * Copyright (C) 2024 Volt Active Data Inc.
 *
 * Use of this source code is governed by an MIT
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/MIT.
 */
package org.voltdb.testparser;

import java.util.regex.Pattern;

public class SQLPatternPartString extends SQLPatternPart
{
    private String m_str;

    SQLPatternPartString(String str)
    {
        m_str = str;
    }

    @Override
    public String generateExpression(int flagsAdd)
    {
        return m_str;
    }

    @Override
    void setCaptureLabel(String captureLabel)
    {
        // Only meaningful to capture-able elements, not raw strings.
        assert false;
    }

    @Override
    Pattern compile(String label)
    {
        // Shouldn't really be used, but this is an obvious implementation.
        return Pattern.compile(m_str);
    }
}
