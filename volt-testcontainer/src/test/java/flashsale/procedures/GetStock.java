/*
 * Copyright (C) 2024-2026 Volt Active Data Inc.
 *
 * Use of this source code is governed by an MIT
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/MIT.
 */
package flashsale.procedures;

import org.voltdb.SQLStmt;
import org.voltdb.VoltProcedure;
import org.voltdb.VoltTable;

/**
 * Returns the current stock level for a product.
 * Returns -1 if the product does not exist.
 */
public class GetStock extends VoltProcedure {

    public final SQLStmt selectStmt = new SQLStmt(
            "SELECT stock_quantity FROM products WHERE product_id = ?;");

    public long run(int productId) {
        voltQueueSQL(selectStmt, EXPECT_ZERO_OR_ONE_ROW, productId);
        VoltTable[] results = voltExecuteSQL(true);
        if (results[0].getRowCount() == 0) {
            return -1;
        }
        return results[0].fetchRow(0).getLong(0);
    }
}
