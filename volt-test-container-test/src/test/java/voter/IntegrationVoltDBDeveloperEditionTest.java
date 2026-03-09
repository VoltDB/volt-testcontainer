/*
 * Copyright (C) 2024-2025 Volt Active Data Inc.
 *
 * Use of this source code is governed by an MIT
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/MIT.
 */
package voter;

import org.junit.Assume;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.voltdb.client.Client2;
import org.voltdb.client.ClientResponse;
import org.voltdb.client.ProcCallException;
import org.voltdbtest.testcontainer.VoltDBCluster;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Integration tests for the VoltDB developer edition image.
 * <p>
 * These tests verify that volt-testcontainer works correctly with the
 * VoltDB developer edition, which does not support command logging.
 * Command logging is automatically disabled when a developer edition
 * image is detected.
 * <p>
 * To run these tests, set:
 * <ul>
 *   <li>{@code VOLTDB_DEV_IMAGE} — the developer edition image to test against
 *       (e.g. {@code voltdb/voltdb-developer:14.1.0})</li>
 *   <li>{@code VOLTDB_DEV_LICENSE} — path to a valid developer edition license file.
 *       Developer and enterprise licenses are incompatible, so this must be separate
 *       from the {@code VOLTDB_LICENSE} used by the enterprise tests.</li>
 * </ul>
 * If {@code VOLTDB_DEV_IMAGE} is not set, these tests are skipped entirely, so existing
 * CI pipelines that only have an enterprise license are unaffected.
 */
public class IntegrationVoltDBDeveloperEditionTest extends TestBase {

    private static final Logger LOG = LoggerFactory.getLogger(IntegrationVoltDBDeveloperEditionTest.class);

    /**
     * Returns the developer edition image to test against.
     * Reads from the {@code VOLTDB_DEV_IMAGE} environment variable.
     * Returns {@code null} if not set, which causes tests to be skipped.
     */
    private static String getDevImage() {
        return System.getenv("VOLTDB_DEV_IMAGE");
    }

    /**
     * Returns the path to the developer edition license file.
     * Reads from {@code VOLTDB_DEV_LICENSE}. Falls back to {@code validLicensePath}
     * (i.e. {@code VOLTDB_LICENSE}) if not set, which is useful when the same license
     * file happens to work for both editions.
     */
    private static String getDevLicensePath() {
        String devLicense = System.getenv("VOLTDB_DEV_LICENSE");
        if (devLicense != null && !devLicense.isEmpty()) {
            File file = Paths.get(devLicense).toAbsolutePath().toFile();
            if (file.exists()) {
                return file.getAbsolutePath();
            }
            throw new IllegalStateException(
                    "VOLTDB_DEV_LICENSE is set but the file does not exist: " + devLicense);
        }
        return validLicensePath;
    }

    @Test
    public void testDeveloperEditionStarts() throws IOException, ProcCallException {
        String devImage = getDevImage();
        Assume.assumeNotNull("Skipping developer edition test: VOLTDB_DEV_IMAGE not set", devImage);

        VoltDBCluster cluster = new VoltDBCluster(getDevLicensePath(), devImage);
        cluster.withLogConsumer(LOG);
        try {
            cluster.start();

            Client2 client = cluster.getClient2();
            assertNotNull("Client2 should not be null after start", client);

            // Verify the cluster is functional with a basic DDL and DML
            ClientResponse ddlResponse = cluster.runDDL(
                    "CREATE TABLE dev_test (id INTEGER NOT NULL, name VARCHAR(50), PRIMARY KEY(id));"
            );
            assertEquals("DDL should succeed", ClientResponse.SUCCESS, ddlResponse.getStatus());

            ClientResponse insertResponse = cluster.callProcedure("@AdHoc",
                    "INSERT INTO dev_test VALUES (1, 'hello');");
            assertEquals("Insert should succeed", ClientResponse.SUCCESS, insertResponse.getStatus());

            ClientResponse selectResponse = cluster.callProcedure("@AdHoc",
                    "SELECT * FROM dev_test;");
            assertEquals("Select should succeed", ClientResponse.SUCCESS, selectResponse.getStatus());
        } finally {
            cluster.shutdown();
        }
    }

    @Test
    public void testDeveloperEditionWithCommandLogExplicitlyDisabled() throws IOException, ProcCallException {
        String devImage = getDevImage();
        Assume.assumeNotNull("Skipping developer edition test: VOLTDB_DEV_IMAGE not set", devImage);

        VoltDBCluster cluster = new VoltDBCluster(getDevLicensePath(), devImage);
        cluster.withLogConsumer(LOG);
        cluster.withCommandLogEnabled(false);
        try {
            cluster.start();

            ClientResponse response = cluster.callProcedure("@SystemInformation");
            assertEquals("@SystemInformation should succeed", ClientResponse.SUCCESS, response.getStatus());
        } finally {
            cluster.shutdown();
        }
    }
}
