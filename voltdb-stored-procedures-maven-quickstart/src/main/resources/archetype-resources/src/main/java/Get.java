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

public class Get extends VoltProcedure {

    // Get the current key inserted*
    public final SQLStmt getKey = new SQLStmt("SELECT VALUE from KEYVALUE WHERE KEYNAME = ?;");

    public VoltTable[] run(int key) {
        voltQueueSQL(getKey, key);
        return voltExecuteSQL();
    }
}