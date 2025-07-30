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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;

public class IntegrationVoterTestWithPreloadedSchemaAndClasses extends TestBase {

    @Test
    public void testBasicContainer() throws Exception {
        // Create a cluster and load schema and classes at init tile then just run the app.
        VoltDBCluster db = new VoltDBCluster(validLicensePath, VOLTDB_IMAGE).
                withInitialSchema(getResourceFile("schema.ddl").getAbsolutePath(), "schema.ddl").
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

    private static File getResourceFile(String fileName) throws Exception {
        URL resource = IntegrationVoterTestWithPreloadedSchemaAndClasses.class.getClassLoader().getResource(fileName);
        if (resource == null) {
            throw new FileNotFoundException("Resource file not found: " + fileName);
        }
        return new File(resource.toURI());
    }

    @Test
    public void testBasicContainerSchemaClassesAfterConstructed() {
        // Create a cluster and load schema and classes at init tile then just run the app.
        VoltDBCluster db = new VoltDBCluster(validLicensePath, VOLTDB_IMAGE);
        try {
            File schemaFile = null;
            try {
                schemaFile = getResourceFile("schema.ddl");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            db.withInitialSchema(schemaFile.getAbsolutePath(), "schema.ddl");
            db.withInitialClasses(getJars());
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
