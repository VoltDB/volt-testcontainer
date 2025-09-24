package ${package};

import org.junit.jupiter.api.Test;
import org.voltdb.client.Client;
import org.voltdb.client.ClientResponse;
import org.voltdb.VoltTable;
import org.voltdbtest.testcontainer.VoltDBCluster;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class KeyValueIT extends IntegrationTestBase {

    @Test
    public void testKeyValue() {
        VoltDBCluster db = new VoltDBCluster(getLicensePath(), "voltdb/voltdb-enterprise:" + getImageVersion(), getExtraLibDirectory());
        try {
            configureTestContainer(db);
            Client client = db.getClient();

            // Test Put
            int key = 10;
            String value = "Hello";

            ClientResponse response = client.callProcedure("Put", key, value);
            assertEquals(ClientResponse.SUCCESS, response.getStatus());

            response = client.callProcedure("Get", key);
            VoltTable table = response.getResults()[0];
            // Advance to first row
            table.advanceRow();

            String val = table.getString(0);
            assertEquals(value, val);



            // Test CapitalizeAndPut
            key = 20;
            value = "for whom the bell tolls";

            response = client.callProcedure("CapitalizeAndPut", key, value);
            assertEquals(ClientResponse.SUCCESS, response.getStatus());

            response = client.callProcedure("Get", key);
            table = response.getResults()[0];
            // Advance to first row
            table.advanceRow();

            val = table.getString(0);
            assertEquals("For Whom The Bell Tolls", val, "The value should have been capitalized in the CapitalizeAndPut procedure");


        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            if (db != null) {
                db.shutdown();
            }
        }
    }
}
