/*
 * Copyright (C) 2024-2026 Volt Active Data Inc.
 *
 * Use of this source code is governed by an MIT
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/MIT.
 */
package flashsale.procedures;

import flashsale.common.Constants;
import org.voltdb.SQLStmt;
import org.voltdb.VoltProcedure;
import org.voltdb.VoltTable;

/**
 * ACID purchase transaction for the flash sale.
 *
 * <p>Atomically checks product stock and either records the purchase or
 * returns an error code. Because VoltDB serializes stored procedure
 * execution, the check-then-decrement sequence is race-free: no two
 * concurrent calls can both see sufficient stock and both succeed when
 * only one unit remains.
 *
 * <p>Return codes are defined in {@link Constants}.
 */
public class Purchase extends VoltProcedure {

    public final SQLStmt checkStockStmt = new SQLStmt(
            "SELECT stock_quantity FROM products WHERE product_id = ?;");

    public final SQLStmt decrementStockStmt = new SQLStmt(
            "UPDATE products SET stock_quantity = stock_quantity - ? " +
            "WHERE product_id = ? AND stock_quantity >= ?;");

    public final SQLStmt insertPurchaseStmt = new SQLStmt(
            "INSERT INTO purchases (purchase_id, customer_id, product_id, quantity) VALUES (?, ?, ?, ?);");

    public long run(long customerId, int productId, int quantity) {
        voltQueueSQL(checkStockStmt, EXPECT_ZERO_OR_ONE_ROW, productId);
        VoltTable[] results = voltExecuteSQL();

        if (results[0].getRowCount() == 0) {
            return Constants.ERR_INVALID_PRODUCT;
        }

        long stock = results[0].fetchRow(0).getLong(0);
        if (stock < quantity) {
            return Constants.ERR_OUT_OF_STOCK;
        }

        long purchaseId = getUniqueId();
        voltQueueSQL(decrementStockStmt, EXPECT_SCALAR_MATCH(1), quantity, productId, quantity);
        voltQueueSQL(insertPurchaseStmt, EXPECT_SCALAR_MATCH(1), purchaseId, customerId, productId, quantity);
        voltExecuteSQL(true);

        return Constants.PURCHASE_SUCCESS;
    }
}
