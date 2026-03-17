/*
 * Copyright (C) 2024-2025 Volt Active Data Inc.
 *
 * Use of this source code is governed by an MIT
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/MIT.
 */

import org.jetbrains.annotations.Nullable;
import org.voltdb.client.ClientResponse;
import org.voltdb.client.ProcCallException;
import org.voltdbtest.testcontainer.VoltDBCluster;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.URL;

import static org.junit.Assert.assertTrue;

/**
 * Base class for VoltDB testcontainer integration tests.
 *
 * <p>Provides shared constants, license path resolution, and helpers for
 * locating the procedures JAR and external-library directory that are
 * produced during the {@code package} phase and consumed by the
 * {@code integration-test} phase via maven-failsafe-plugin.
 */
public class TestBase {

    /** Default enterprise image used by most tests. */
    public static final String VOLTDB_IMAGE = "voltdb/voltdb-enterprise:14.3.3";

    /** Path to a valid VoltDB license file, resolved from well-known locations. */
    protected static final String validLicensePath;

    static {
        // Resolution order: VOLTDB_LICENSE env var → ~/license.xml → /tmp/license.xml
        String path = "/tmp/license.xml";
        String envLicense = System.getenv("VOLTDB_LICENSE");
        if (envLicense != null) {
            File f = new File(envLicense);
            if (f.exists()) {
                path = f.getAbsolutePath();
            }
        }
        if (path.equals("/tmp/license.xml")) {
            File home = new File(System.getProperty("user.home"), "license.xml");
            if (home.exists()) {
                path = home.getAbsolutePath();
            }
        }
        validLicensePath = path;
        System.out.println("License file path is: " + validLicensePath);
    }

    /**
     * Starts the cluster, loads the procedures JAR, and applies the DDL schema.
     * Used by flash sale tests that need a fully configured database.
     */
    public void configureTestContainer(VoltDBCluster db) {
        try {
            db.start();
            for (File jar : getJars()) {
                System.out.println("Loading classes from: " + jar);
                ClientResponse response = db.loadClasses(jar.getAbsolutePath());
                assertTrue("Load classes must pass", response.getStatus() == ClientResponse.SUCCESS);
            }
            URL schema = getClass().getClassLoader().getResource("schema.ddl");
            assertTrue("Schema must be loaded", db.runDDL(new File(schema.getFile())));
        } catch (IOException | ProcCallException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns the procedures JAR files produced by the {@code procedures-jar} execution
     * of maven-jar-plugin (classifier "procedures") during the {@code package} phase.
     *
     * <p>The test-classes directory is at {@code target/test-classes/}; its parent
     * is {@code target/}, where maven-jar-plugin writes the classified JAR.
     */
    protected File[] getJars() {
        File targetDir = getTargetDir();
        FileFilter procedureJars = f ->
                !f.isDirectory()
                && f.getName().endsWith(".jar")
                && f.getName().contains("procedures");
        File[] jars = targetDir.listFiles(procedureJars);
        if (jars == null || jars.length == 0) {
            throw new RuntimeException(
                    "No procedures JAR found in " + targetDir.getAbsolutePath() +
                    ". Run 'mvn package' first.");
        }
        return jars;
    }

    /**
     * Returns the {@code target/lib/} directory containing runtime dependencies
     * (e.g. joda-time) copied there by maven-dependency-plugin during the
     * {@code package} phase. Used by external-library tests.
     */
    protected String getExtraLibDirectory() {
        File libDir = new File(getTargetDir(), "lib");
        if (!libDir.exists() || !libDir.isDirectory()) {
            throw new RuntimeException(
                    "Extra lib directory not found: " + libDir.getAbsolutePath() +
                    ". Run 'mvn package' first.");
        }
        return libDir.getAbsolutePath();
    }

    /**
     * Returns the {@code target/} directory of this module by navigating one level
     * up from the test-classes output directory.
     */
    private File getTargetDir() {
        // getCodeSource().getLocation() points to target/test-classes/
        File testClassesDir = new File(
                getClass().getProtectionDomain().getCodeSource().getLocation().getFile());
        return testClassesDir.getParentFile();
    }
}
