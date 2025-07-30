/*
 * Copyright (C) 2024 Volt Active Data Inc.
 *
 * Use of this source code is governed by an MIT
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/MIT.
 */
package voter;

import junit.framework.TestCase;
import org.voltdbtest.testcontainer.VoltDBCluster;

import static org.junit.jupiter.api.Assertions.fail;

public class IntegrationVoltDBClusterTest extends TestBase {

    // This test should fail unless you have a valid license.
    public void testStart() {
        VoltDBCluster cluster = new VoltDBCluster("/tmp/xxx-voltdb-license.xml", VOLTDB_IMAGE);
        try {
            cluster.start(2000);
            fail("Cluster should fail to start without license. Most likely you have voltdb-license.xml in your /tmp directory.");
        } catch (Exception e) {
            // We are good.
        } finally {
            cluster.shutdown();
        }
    }
}
