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
        String home = System.getProperty("user.home");
        if (home == null || home.isEmpty() || home.equals("?")) {
            home = System.getenv("HOME");
            if (home == null || home.isEmpty()) {
                home = "/etc";
            }
        }
        String licensePath = home + "/voltdb-license.xml";
        // Try file from environment variable
        String elicenseFile = System.getenv("VOLTDB_LICENSE");
        if (elicenseFile != null) {
            File file = Paths.get(elicenseFile).toAbsolutePath().toFile();
            licensePath = file.getAbsolutePath();
        } else {
            // Try -D this will only work for us in our jenkins.
            String proenv = System.getProperty("env.VOLTPRO");
            if (proenv != null) {
                File prodir = new File(proenv).getAbsoluteFile();
                File file = new File(prodir.getPath() + "/tests/frontend/org/voltdb/v4_general_test_license.xml");
                licensePath = file.getAbsolutePath();
            }
            //env.VOLTPRO
        }
        File file = Paths.get(licensePath).toAbsolutePath().toFile();
        if (!file.exists()) {
            // finally Look for license in /tmp
            licensePath = "/tmp/voltdb-license.xml";
            file = Paths.get(licensePath).toAbsolutePath().toFile();
            if (!file.exists()) {
                throw new RuntimeException("Could not find license file. " +
                        "environment variable VOLTDB_LICENSE not pointing to a license file. " +
                        "Default locations $HOME/voltdb-license.xml /tmp/voltdb-license.xml does not have a license file.");
            }
        }
        return licensePath;
    }
}
