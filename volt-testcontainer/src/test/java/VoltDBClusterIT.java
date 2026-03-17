/*
 * Copyright (C) 2024-2025 Volt Active Data Inc.
 *
 * Use of this source code is governed by an MIT
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/MIT.
 */

import org.junit.After;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.Network;
import org.voltdb.client.Client;
import org.voltdb.client.Client2;
import org.voltdb.client.ClientResponse;
import org.voltdb.client.ProcCallException;
import org.voltdbtest.testcontainer.VoltDBCluster;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Integration tests for the {@link VoltDBCluster} API.
 *
 * <p>Covers constructors, start/shutdown, port and host getters, DDL execution,
 * procedure calls, class loading, multi-node topology, and fluent builder
 * chaining. Both the legacy {@link Client} and the modern {@link Client2} are
 * verified against the same running cluster instance.
 */
public class VoltDBClusterIT extends TestBase {

    private static final Logger LOG = LoggerFactory.getLogger(VoltDBClusterIT.class);
    private VoltDBCluster cluster;

    @After
    public void tearDown() {
        if (cluster != null) {
            cluster.shutdown();
            cluster = null;
        }
    }

    /** All five constructor overloads must produce a non-null cluster instance. */
    @Test
    public void testConstructors() {
        new VoltDBCluster(validLicensePath).shutdown();
        new VoltDBCluster(validLicensePath, VOLTDB_IMAGE).shutdown();
        new VoltDBCluster(validLicensePath, VOLTDB_IMAGE, (String) null).shutdown();
        new VoltDBCluster(validLicensePath, VOLTDB_IMAGE, 2, 0).shutdown();
        new VoltDBCluster(validLicensePath, VOLTDB_IMAGE, 2, 0, null).shutdown();
    }

    /** After start(), port and host getters return valid values and both client types connect. */
    @Test
    public void testStartGettersAndBothClients() throws IOException, ProcCallException {
        cluster = new VoltDBCluster(validLicensePath, VOLTDB_IMAGE);
        cluster.start();

        assertTrue("Mapped port 21211 should be > 0", cluster.getMappedPort(21211) > 0);
        assertTrue("First mapped port should be > 0", cluster.getFirstMappedPort() > 0);
        assertFalse("Host should not be empty", cluster.getHost().isEmpty());
        assertTrue("HostAndPort should contain ':'", cluster.getHostAndPort().contains(":"));

        // Both client types must be obtainable and functional
        Client2 client2 = cluster.getClient2();
        assertNotNull("Client2 must not be null", client2);
        assertEquals(ClientResponse.SUCCESS,
                client2.callProcedureSync("@SystemInformation").getStatus());

        Client client = cluster.getClient();
        assertNotNull("Client must not be null", client);
        assertEquals(ClientResponse.SUCCESS,
                client.callProcedure("@SystemInformation").getStatus());
    }

    /** All fluent builder methods return the same cluster instance (enables chaining). */
    @Test
    public void testFluentBuilderChaining() throws IOException {
        Path tempSchema = Files.createTempFile("schema", ".sql");
        Path tempJar    = Files.createTempFile("test",   ".jar");
        try {
            cluster = new VoltDBCluster(validLicensePath, VOLTDB_IMAGE, 3, 0);
            VoltDBCluster result = cluster
                    .withLogConsumer(LOG)
                    .withUserNameAndPassword("user", "pass")
                    .withDeploymentContent(
                            "<?xml version=\"1.0\"?><deployment><cluster kfactor=\"0\"/></deployment>")
                    .withKsafety(0)
                    .withInitialSchemaFromHostPath(tempSchema.toAbsolutePath().toString(), "schema.sql")
                    .withInitialClasses(new File[]{tempJar.toFile()})
                    .withCommandLogEnabled(false);
            assertSame("Builder methods must return same instance", cluster, result);

            Network network = Network.newNetwork();
            assertSame("withNetwork must return same instance", cluster, cluster.withNetwork(network));
            network.close();
        } finally {
            Files.deleteIfExists(tempSchema);
            Files.deleteIfExists(tempJar);
        }
    }

    /** runDDL(String) executes successfully and returns SUCCESS. */
    @Test
    public void testRunDDLString() throws IOException, ProcCallException {
        cluster = new VoltDBCluster(validLicensePath, VOLTDB_IMAGE);
        cluster.start();

        ClientResponse response = cluster.runDDL(
                "CREATE TABLE ddl_string_test (id INTEGER NOT NULL, PRIMARY KEY(id));");
        assertEquals(ClientResponse.SUCCESS, response.getStatus());
    }

    /** runDDL(File) executes successfully and returns true. */
    @Test
    public void testRunDDLFile() throws IOException, ProcCallException {
        cluster = new VoltDBCluster(validLicensePath, VOLTDB_IMAGE);
        cluster.start();

        Path tempFile = Files.createTempFile("ddl", ".sql");
        try (FileWriter w = new FileWriter(tempFile.toFile())) {
            w.write("CREATE TABLE ddl_file_test (id INTEGER NOT NULL, PRIMARY KEY(id));");
        }
        try {
            assertTrue(cluster.runDDL(tempFile.toFile()));
        } finally {
            Files.delete(tempFile);
        }
    }

    /** callProcedure() succeeds for a built-in system procedure. */
    @Test
    public void testCallProcedure() throws IOException, ProcCallException {
        cluster = new VoltDBCluster(validLicensePath, VOLTDB_IMAGE);
        cluster.start();

        ClientResponse response = cluster.callProcedure("@SystemInformation");
        assertEquals(ClientResponse.SUCCESS, response.getStatus());
    }

    /** loadClasses() succeeds; loadClasses() with a deletion hint also succeeds. */
    @Test
    public void testClassLoading() throws IOException, ProcCallException {
        cluster = new VoltDBCluster(validLicensePath, VOLTDB_IMAGE);
        cluster.start();

        File jar = getJars()[0];

        ClientResponse r1 = cluster.loadClasses(jar.getAbsolutePath());
        assertEquals(ClientResponse.SUCCESS, r1.getStatus());

        // Re-loading with a non-existent class to delete is still valid
        ClientResponse r2 = cluster.loadClasses(jar.getAbsolutePath(), "com.example.NonExistent");
        assertEquals(ClientResponse.SUCCESS, r2.getStatus());
    }

    /**
     * A 3-node cluster (k=1) starts, exposes 3 containers, shares a network,
     * and accepts DDL and procedure calls.
     */
    @Test
    public void testMultiNodeCluster() throws IOException, ProcCallException {
        cluster = new VoltDBCluster(validLicensePath, VOLTDB_IMAGE, 3, 1);
        cluster.start();

        List<String> ids   = cluster.getContainerIds();
        List<String> names = cluster.getContainerNames();
        assertEquals("Expected 3 container IDs",   3, ids.size());
        assertEquals("Expected 3 container names", 3, names.size());
        ids.forEach(id     -> assertFalse("Container ID must not be empty",   id.isEmpty()));
        names.forEach(name -> assertFalse("Container name must not be empty", name.isEmpty()));

        String networkId = cluster.getNetworkId();
        assertNotNull("Network ID must not be null", networkId);
        assertFalse("Network ID must not be empty", networkId.isEmpty());

        ClientResponse ddlResp = cluster.runDDL(
                "CREATE TABLE multi_node_test (id INTEGER NOT NULL, PRIMARY KEY(id));");
        assertEquals(ClientResponse.SUCCESS, ddlResp.getStatus());

        ClientResponse sysResp = cluster.callProcedure("@SystemInformation");
        assertEquals(ClientResponse.SUCCESS, sysResp.getStatus());
    }

    /** After shutdown() the cluster stops; a second shutdown() call is safe. */
    @Test
    public void testShutdown() throws IOException {
        cluster = new VoltDBCluster(validLicensePath, VOLTDB_IMAGE);
        cluster.start();

        Client2 client = cluster.getClient2();
        assertNotNull(client);

        cluster.shutdown();
        cluster = null; // prevent double-shutdown in tearDown
    }

    /** getClient2(hostAlias) returns a connected Client2 for the named host. */
    @Test
    public void testGetClient2ByHost() throws IOException {
        cluster = new VoltDBCluster(validLicensePath, VOLTDB_IMAGE, 1, 0);
        cluster.start();

        Client2 client = cluster.getClient2("host-0");
        assertNotNull("Client2 for host-0 must not be null", client);
    }
}
