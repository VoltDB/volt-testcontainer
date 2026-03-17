/*
 * Copyright (C) 2024-2025 Volt Active Data Inc.
 *
 * Use of this source code is governed by an MIT
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/MIT.
 */

import org.junit.Test;
import org.voltdbtest.testcontainer.VoltDBCluster;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * Verifies that a cluster fails to start when given an invalid license path.
 */
public class VoltDBLicenseIT extends TestBase {

    @Test
    public void testClusterFailsWithoutValidLicense() {
        // VoltDBContainer validates the license path at construction time, so the
        // exception may be thrown before start() is called.
        VoltDBCluster cluster = null;
        try {
            cluster = new VoltDBCluster(
                    "/tmp/xxx-nonexistent-voltdb-license.xml", VOLTDB_IMAGE);
            cluster.start(2000);
            fail("Cluster should have failed — ensure /tmp/xxx-nonexistent-voltdb-license.xml does not exist.");
        } catch (Exception e) {
            // Expected: construction or start() rejects the invalid license path
        } finally {
            if (cluster != null) cluster.shutdown();
        }
    }
}
