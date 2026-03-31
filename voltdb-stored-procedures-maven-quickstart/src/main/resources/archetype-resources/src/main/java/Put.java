/*
 * Copyright (C) 2024 Volt Active Data Inc.
 *
 * Use of this source code is governed by an MIT
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/MIT.
 */
package ${package};

import org.voltdb.SQLStmt;
import org.voltdb.VoltProcedure;
import org.voltdb.VoltTable;

public class Put extends VoltProcedure {

    // Insert a key and value*
    public final SQLStmt insertKey = new SQLStmt("INSERT INTO KEYVALUE (KEYNAME, VALUE) VALUES (?, ?);");

    public VoltTable[] run(int key, String value) {
        voltQueueSQL(insertKey, key, value);
        VoltTable result[] = voltExecuteSQL();
        return result;
    }
}