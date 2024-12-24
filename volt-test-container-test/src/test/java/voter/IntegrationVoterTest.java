/*
 * Copyright (C) 2024 Volt Active Data Inc.
 *
 * Use of this source code is governed by an MIT
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/MIT.
 */
package voter;

import org.joda.time.LocalTime;
import org.joda.time.format.ISODateTimeFormat;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.voltdb.VoltTable;
import org.voltdb.client.ClientResponse;
import org.voltdb.client.ProcCallException;
import org.voltdbtest.testcontainer.VoltDBCluster;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class IntegrationVoterTest extends TestBase {
    private static final Logger LOG = LoggerFactory.getLogger(IntegrationVoterTest.class);

    @Test
    public void testBasicContainer() {
        VoltDBCluster db = new VoltDBCluster(getLicensePath(), "voltdb/voltdb-enterprise:14.1.0");
        db.withLogConsumer(LOG);
        try {
            configureTestContainer(db);
            // Now run benchmark which will invoke all procedures.
            VoterValidation.run(db.getClient());
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (ProcCallException e) {
            throw new RuntimeException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            if (db != null) {
                db.shutdown();
            }
        }
    }

    @Test
    public void testBasicContainerWithExternalJars() {
        LocalTime currentTime = LocalTime.now();
        VoltDBCluster db = new VoltDBCluster(getLicensePath(), "voltdb/voltdb-enterprise:13.3.0", getExtraLibDirectory());
        try {
            configureTestContainer(db);
            String s = currentTime.toString(ISODateTimeFormat.time());
            ClientResponse response = db.callProcedure("JodaTimeInsert", s);
            assertTrue("Insert must pass", response.getStatus() == ClientResponse.SUCCESS);
            response = db.callProcedure("@AdHoc", "select * from jodatime;");
            assertTrue("Select must pass", response.getStatus() == ClientResponse.SUCCESS);
            VoltTable t = response.getResults()[0];
            t.advanceRow();
            String tstr = t.getString(0);
            assertEquals("Inserted time string does not match", s, tstr);
            long ts = t.getTimestampAsLong(1);
            assertEquals("Inserted time milis does not match", currentTime.getMillisOfDay(), ts);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (ProcCallException e) {
            throw new RuntimeException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            if (db != null) {
                db.shutdown();
            }
        }
    }

}
