package voter;

import junit.framework.TestCase;
import org.voltdbtest.testcontainer.VoltDBCluster;

public class VoltDBClusterTest extends TestCase {

    // This test should fail unless you put a valid VoltDB license in /tmp/voltdb-license.xml
    public void testStart() {
        VoltDBCluster cluster = new VoltDBCluster("/tmp/voltdb-license.xml", "voltdb/voltdb-enterprise-dev:master--latest");
        try {
            cluster.start();
            fail("Cluster should fail to start without license. Most likely you have voltdb-license.xml in your /tmp directory.");
        } catch (Exception e) {
            // We are good.
            e.printStackTrace();
        } finally {
            cluster.shutdown();
        }
    }
}
