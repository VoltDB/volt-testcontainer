/*
 * Copyright (C) 2024-2025 Volt Active Data Inc.
 *
 * Use of this source code is governed by an MIT
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/MIT.
 */

import org.junit.Test;
import org.voltdb.VoltTable;
import org.voltdb.client.ClientResponse;
import org.voltdbtest.testcontainer.VoltDBCluster;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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
            assertTrue("ToJson must succeed",
                    insert.getStatus() == ClientResponse.SUCCESS);

            ClientResponse select = db.callProcedure("@AdHoc",
                    "SELECT input, result FROM string_transform;");
            assertTrue("SELECT must succeed",
                    select.getStatus() == ClientResponse.SUCCESS);

            VoltTable t = select.getResults()[0];
            assertTrue("Must have one row", t.advanceRow());
            assertEquals("Stored input must match", input,    t.getString("input"));
            assertEquals("JSON result must match",  expected, t.getString("result"));
        } finally {
            db.shutdown();
        }
    }
}
