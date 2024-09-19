/*
 * Copyright (C) 2024 Volt Active Data Inc.
 *
 * Use of this source code is governed by an MIT
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/MIT.
 */
package voter;

import org.jetbrains.annotations.Nullable;
import org.voltdb.client.ClientResponse;
import org.voltdb.client.ProcCallException;
import org.voltdbtest.testcontainer.VoltDBCluster;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Paths;

import static org.junit.Assert.assertTrue;

public class TestBase {

    public void configureTestContainer(VoltDBCluster db) {
        try {
            db.start();
            ClientResponse response;
            File[] jars = getJars();
            for (File jarToLoad : jars) {
                System.out.println("Loading classes from: " + jarToLoad);
                response = db.loadClasses(jarToLoad.getAbsolutePath());
                assertTrue("Load classes must pass", response.getStatus() == ClientResponse.SUCCESS);
            }

            URL schema = getClass().getClassLoader().getResource("schema.ddl");
            assertTrue("Schema must get loaded", db.runDDL(new File(schema.getFile())));
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (ProcCallException e) {
            throw new RuntimeException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected String getExtraLibDirectory() {
        URL schema = getClass().getClassLoader().getResource("schema.ddl");
        return new File((new File(schema.getFile()).getParent())).getAbsolutePath();
    }

    protected File @Nullable [] getJars() {
        String relPath = getClass().getProtectionDomain().getCodeSource().getLocation().getFile();
        File targetDir = new File(relPath + "/../../target");
        FileFilter jarFiles = pathname -> {
            if (pathname.isDirectory()) {
                return false;
            }
            String name = pathname.getName();
            if (name.endsWith(".jar") && name.startsWith("volt-test")) {
                return true;
            }
            return false;
        };
        File jars[] = targetDir.listFiles(jarFiles);
        return jars;
    }

    protected String getLicensePath() {
        String licensePath = "/tmp/voltdb-license.xml";
        // Try file from environment variable
        String elicenseFile = System.getenv("VOLTDB_LICENSE");
        if (elicenseFile != null) {
            File file = Paths.get(elicenseFile).toAbsolutePath().toFile();
            if (file.exists()) {
                licensePath = file.getAbsolutePath();
            }
        }
        System.out.println("License file path is: " + licensePath);
        return licensePath;
    }
}
