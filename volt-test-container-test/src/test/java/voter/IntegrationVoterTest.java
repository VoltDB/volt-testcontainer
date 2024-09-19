/* This file is part of VoltDB.
 * Copyright (C) 2024 Volt Active Data Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS BE LIABLE FOR ANY CLAIM, DAMAGES OR
 * OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */
package voter;

import org.joda.time.LocalTime;
import org.joda.time.format.ISODateTimeFormat;
import org.junit.Test;
import org.voltdb.VoltTable;
import org.voltdb.client.ClientResponse;
import org.voltdb.client.ProcCallException;
import org.voltdbtest.testcontainer.VoltDBCluster;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class IntegrationVoterTest extends TestBase {

    @Test
    public void testBasicContainer() {
        VoltDBCluster db = new VoltDBCluster(getLicensePath(), "voltdb/voltdb-enterprise-dev:master--latest");
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
