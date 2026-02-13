/*
 * Copyright (C) 2024 Volt Active Data Inc.
 *
 * Use of this source code is governed by an MIT
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/MIT.
 */
package voter.client1;

import org.junit.Test;
import org.voltdb.client.ProcCallException;
import org.voltdbtest.testcontainer.VoltDBCluster;
import voter.TestBase;

import java.io.IOException;

public class IntegrationVoter3NK1Test extends TestBase {

    @Test
    public void testBasicContainer() {
        VoltDBCluster db = new VoltDBCluster(validLicensePath, VOLTDB_IMAGE, 3,1);
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
}