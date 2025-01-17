package ${package};

import org.junit.Test;
import org.voltdb.client.Client;
import org.voltdb.client.ClientResponse;
import org.voltdbtest.testcontainer.VoltDBCluster;

import java.io.IOException;

import static org.junit.Assert.fail;

public class KeyValueTest extends TestBase {

    @Test
    public void testKeyValue() {
        VoltDBCluster db = new VoltDBCluster(getLicensePath(), "voltdb/voltdb-enterprise:14.1.0");
        try {
            configureTestContainer(db);
            Client client = db.getClient();
            ClientResponse response = client.callProcedure("KeyValueInsert", 10, "Hello");
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            if (db != null) {
                db.shutdown();
            }
        }
    }
}