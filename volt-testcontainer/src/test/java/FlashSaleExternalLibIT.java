/*
 * Copyright (C) 2024-2026 Volt Active Data Inc.
 *
 * Use of this source code is governed by an MIT
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/MIT.
 */

import org.junit.jupiter.api.Test;
import org.voltdb.VoltTable;
import org.voltdb.client.ClientResponse;
import org.voltdbtest.testcontainer.VoltDBCluster;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies that a stored procedure depending on an external runtime JAR
 * (Gson) can be loaded into VoltDB via
 * {@link VoltDBCluster#VoltDBCluster(String, String, String)}.
 *
 * <p>The {@code extraJarsDir} argument points to {@code target/lib/}, where
 * maven-dependency-plugin copies runtime dependencies during the {@code package}
 * phase. VoltDB adds every JAR in that directory to the server class path,
 * making the Gson library available to stored procedures.
 */
public class FlashSaleExternalLibIT extends TestBase {

    @Test
    public void testExternalLibraryLoadedIntoProcedure() throws Exception {
        String input    = "hello world";
        String expected = "{\"input\":\"hello world\"}";  // Gson output

        VoltDBCluster db = new VoltDBCluster(
                validLicensePath, VOLTDB_IMAGE, getExtraLibDirectory());
        try {
            configureTestContainer(db);

            ClientResponse insert = db.callProcedure("ToJson", input);
            assertTrue(insert.getStatus() == ClientResponse.SUCCESS, "ToJson must succeed");

            ClientResponse select = db.callProcedure("@AdHoc",
                    "SELECT input, result FROM string_transform;");
            assertTrue(select.getStatus() == ClientResponse.SUCCESS, "SELECT must succeed");

            VoltTable t = select.getResults()[0];
            assertTrue(t.advanceRow(), "Must have one row");
            assertEquals(input,    t.getString("input"),  "Stored input must match");
            assertEquals(expected, t.getString("result"), "JSON result must match");
        } finally {
            db.shutdown();
        }
    }
}
