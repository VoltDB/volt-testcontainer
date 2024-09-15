package org.voltdbtest.testcontainer;

import junit.framework.TestCase;

import java.io.IOException;

public class VoltDBClusterTest extends TestCase {

    public void testStart() {
        VoltDBCluster cluster = new VoltDBCluster("/tmp/voltdb-license.xml");
        try {
            cluster.start();
            fail("Cluster should fail to start without license. Most likely you have voltdb-license.xml in your /tmp directory.");
        } catch (Exception e) {
            // We are good.
        } finally {
            cluster.shutdown();
        }
    }
}