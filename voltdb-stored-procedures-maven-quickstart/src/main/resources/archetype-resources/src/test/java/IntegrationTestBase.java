/*
 * Copyright (C) 2024 Volt Active Data Inc.
 *
 * Use of this source code is governed by an MIT
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/MIT.
 */
package ${package};


import org.voltdb.client.ClientResponse;
import org.voltdbtest.testcontainer.VoltDBCluster;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class IntegrationTestBase {

    private static final Properties props = new Properties();

    static {
        // load properties from properties file in resources, which are set by the pom.xml
        try (InputStream input = IntegrationTestBase.class.getClassLoader().getResourceAsStream("test.properties")) {
            if (input != null) {
                props.load(input);
            }
        } catch (IOException e) {
            // Use default values if properties file is missing
        }
    }

    public String getImageVersion() {
        return props.getProperty("voltdb.image.version", "14.3.0");
    }

    public void configureTestContainer(VoltDBCluster db) {
        try {
            db.start();
            ClientResponse response;
            File[] jars = getJars();
            if (jars != null) {
                for (File jarToLoad : jars) {
                    System.out.println("Loading classes from: " + jarToLoad);
                    response = db.loadClasses(jarToLoad.getAbsolutePath());
                    assertEquals(ClientResponse.SUCCESS, response.getStatus(), "Load classes must pass");
                }
            }

            // Load schema from project root schema directory
            String basedir = System.getProperty("user.dir");
            File schemaFile = new File(basedir, "schema/ddl.sql");
            if (schemaFile.exists()) {
                System.out.println("Loading schema from: " + schemaFile.getAbsolutePath());
                assertTrue(db.runDDL(schemaFile), "Schema must get loaded");
            } else {
                System.err.println("Schema file not found at: " + schemaFile.getAbsolutePath());
            }
        } catch (Exception e) {
            System.out.println("Exception while configuring Test Container: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    // If the project produces a target/lib directory with dependency jar files, return the path so the test container can load them
    protected String getExtraLibDirectory() {
        String basedir = System.getProperty("user.dir");
        File libdir = new File(basedir, "target/lib");
        if (libdir.exists() && libdir.isDirectory() &&
            Arrays.stream(libdir.listFiles())
            .anyMatch(file -> file.getName().toLowerCase().endsWith(".jar"))) {
            return libdir.getAbsolutePath();
        } else {
            return null;
        }
    }

    protected File[] getJars() {
        String relPath = getClass().getProtectionDomain().getCodeSource().getLocation().getFile();
        File targetDir = new File(relPath + "/../");
        FileFilter jarFiles = pathname -> {
            if (pathname.isDirectory()) {
                return false;
            }
            String name = pathname.getName();
            if (name.endsWith(".jar") && !name.startsWith("original")) {
                return true;
            }
            return false;
        };
        File jars[] = targetDir.listFiles(jarFiles);
        return jars;
    }

    protected String getLicensePath() {
        String licensePath = "/tmp/license.xml";
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
