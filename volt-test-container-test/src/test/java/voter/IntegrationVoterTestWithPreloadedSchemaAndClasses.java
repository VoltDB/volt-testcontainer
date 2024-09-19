/*
 * Copyright (C) 2024 Volt Active Data Inc.
 *
 * Use of this source code is governed by an MIT
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/MIT.
 */
package voter;

import org.junit.Test;
import org.voltdb.client.ProcCallException;
import org.voltdbtest.testcontainer.VoltDBCluster;

import java.io.IOException;

public class IntegrationVoterTestWithPreloadedSchemaAndClasses extends TestBase {

    @Test
    public void testBasicContainer() {
        // Create a cluster and load schema and classes at init tile then just run the app.
        VoltDBCluster db = new VoltDBCluster(getLicensePath(), "voltdb/voltdb-enterprise-dev:master--latest").
                withInitialSchema("/schema.ddl", "schema.ddl").
                withInitialClasses(getJars());
        try {
            db.start();
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
