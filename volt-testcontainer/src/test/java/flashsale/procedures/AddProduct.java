/*
 * Copyright (C) 2025-2026 Volt Active Data Inc.
 *
 * Use of this source code is governed by an MIT
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/MIT.
 */
package flashsale.procedures;

import org.voltdb.SQLStmt;
import org.voltdb.VoltProcedure;

/**
 * Inserts a product into the flash sale catalog.
 * Used during test setup to seed product inventory.
 */
public class AddProduct extends VoltProcedure {

    public final SQLStmt insertStmt = new SQLStmt(
            "INSERT INTO products (product_id, product_name, price, stock_quantity) VALUES (?, ?, ?, ?);");

    public long run(int productId, String name, double price, int stockQuantity) {
        voltQueueSQL(insertStmt, EXPECT_SCALAR_MATCH(1), productId, name, price, stockQuantity);
        voltExecuteSQL(true);
        return 1;
    }
}
