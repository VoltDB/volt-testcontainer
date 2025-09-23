package ${package};

import org.junit.Test;
import org.voltdb.client.Client;
import org.voltdb.client.ClientResponse;
import org.voltdb.VoltTable;
import org.voltdbtest.testcontainer.VoltDBCluster;

import java.io.IOException;

import static org.junit.Assert.fail;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class KeyValueIT extends TestBase {

    @Test
    public void testKeyValue() {
        VoltDBCluster db = new VoltDBCluster(getLicensePath(), "voltdb/voltdb-enterprise:" + getImageVersion(), getExtraLibDirectory());
        try {
            configureTestContainer(db);
            Client client = db.getClient();
            ClientResponse response = client.callProcedure("Put", 10, "Hello");
            assertTrue(response.getStatus() == ClientResponse.SUCCESS);
            response = client.callProcedure("Get", 10);
            VoltTable table = response.getResults()[0];
            // Advance to first row
            table.advanceRow();
            String val = table.getString(0);
            assertTrue(val.equals("Hello"));
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            if (db != null) {
                db.shutdown();
            }
        }
    }
}
