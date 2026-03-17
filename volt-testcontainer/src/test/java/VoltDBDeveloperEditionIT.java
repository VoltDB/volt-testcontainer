/*
 * Copyright (C) 2024-2025 Volt Active Data Inc.
 *
 * Use of this source code is governed by an MIT
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/MIT.
 */

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
 * Integration tests for the VoltDB developer-edition image.
 *
 * <p>These tests are skipped unless the {@code VOLTDB_DEV_IMAGE} environment
 * variable is set. Developer-edition images do not support command logging;
 * volt-testcontainer detects this automatically and disables command logging
 * to prevent startup failures.
 *
 * <p>To run:
 * <ul>
 *   <li>Set {@code VOLTDB_DEV_IMAGE} to the developer image name
 *       (e.g. {@code voltactivedata/volt-developer-edition:14.1.0_voltdb}).</li>
 *   <li>Optionally set {@code VOLTDB_DEV_LICENSE} to a separate developer license.
 *       Falls back to {@code VOLTDB_LICENSE} if not set.</li>
 * </ul>
 */
public class VoltDBDeveloperEditionIT extends TestBase {

    private static final Logger LOG = LoggerFactory.getLogger(VoltDBDeveloperEditionIT.class);

    /** Skips the test if {@code VOLTDB_DEV_IMAGE} is not set in the environment. */
    private static String requireDevImage() {
        String image = System.getenv("VOLTDB_DEV_IMAGE");
        Assume.assumeNotNull(
                "Skipping developer-edition test: VOLTDB_DEV_IMAGE is not set", image);
        return image;
    }

    private static String devLicensePath() {
        String devLicense = System.getenv("VOLTDB_DEV_LICENSE");
        if (devLicense != null && !devLicense.isEmpty()) {
            File f = Paths.get(devLicense).toAbsolutePath().toFile();
            if (f.exists()) return f.getAbsolutePath();
            throw new IllegalStateException(
                    "VOLTDB_DEV_LICENSE is set but file does not exist: " + devLicense);
        }
        return validLicensePath;
    }

    /**
     * Verifies the developer-edition cluster starts and accepts basic DDL and DML.
     * Command logging is auto-disabled by volt-testcontainer on developer images.
     */
    @Test
    public void testDeveloperEditionStarts() throws IOException, ProcCallException {
        String devImage = requireDevImage();

        VoltDBCluster cluster = new VoltDBCluster(devLicensePath(), devImage);
        cluster.withLogConsumer(LOG);
        try {
            cluster.start();

            Client2 client = cluster.getClient2();
            assertNotNull(client);

            ClientResponse ddl = cluster.runDDL(
                    "CREATE TABLE dev_test (id INTEGER NOT NULL, name VARCHAR(50), PRIMARY KEY(id));");
            assertEquals(ClientResponse.SUCCESS, ddl.getStatus());

            assertEquals(ClientResponse.SUCCESS,
                    cluster.callProcedure("@AdHoc", "INSERT INTO dev_test VALUES (1, 'hello');")
                           .getStatus());
            assertEquals(ClientResponse.SUCCESS,
                    cluster.callProcedure("@AdHoc", "SELECT * FROM dev_test;").getStatus());
        } finally {
            cluster.shutdown();
        }
    }

    /**
     * Verifies that explicitly calling {@code withCommandLogEnabled(false)} still
     * allows the developer-edition cluster to start normally.
     */
    @Test
    public void testDeveloperEditionWithCommandLogExplicitlyDisabled()
            throws IOException, ProcCallException {
        String devImage = requireDevImage();

        VoltDBCluster cluster = new VoltDBCluster(devLicensePath(), devImage);
        cluster.withLogConsumer(LOG);
        cluster.withCommandLogEnabled(false);
        try {
            cluster.start();
            assertEquals(ClientResponse.SUCCESS,
                    cluster.callProcedure("@SystemInformation").getStatus());
        } finally {
            cluster.shutdown();
        }
    }
}
