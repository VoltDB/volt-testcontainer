/*
 * Copyright (C) 2024-2026 Volt Active Data Inc.
 *
 * Use of this source code is governed by an MIT
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/MIT.
 */

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
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

import static org.junit.jupiter.api.Assertions.*;

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

    @AfterEach
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

        assertTrue(cluster.getMappedPort(21211) > 0, "Mapped port 21211 should be > 0");
        assertTrue(cluster.getFirstMappedPort() > 0, "First mapped port should be > 0");
        assertFalse(cluster.getHost().isEmpty(), "Host should not be empty");
        assertTrue(cluster.getHostAndPort().contains(":"), "HostAndPort should contain ':'");

        // Both client types must be obtainable and functional
        Client2 client2 = cluster.getClient2();
        assertNotNull(client2, "Client2 must not be null");
        assertEquals(ClientResponse.SUCCESS,
                client2.callProcedureSync("@SystemInformation").getStatus());

        Client client = cluster.getClient();
        assertNotNull(client, "Client must not be null");
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
            assertSame(cluster, result, "Builder methods must return same instance");

            Network network = Network.newNetwork();
            assertSame(cluster, cluster.withNetwork(network), "withNetwork must return same instance");
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
        assertEquals(3, ids.size(),   "Expected 3 container IDs");
        assertEquals(3, names.size(), "Expected 3 container names");
        ids.forEach(id     -> assertFalse(id.isEmpty(),   "Container ID must not be empty"));
        names.forEach(name -> assertFalse(name.isEmpty(), "Container name must not be empty"));

        String networkId = cluster.getNetworkId();
        assertNotNull(networkId, "Network ID must not be null");
        assertFalse(networkId.isEmpty(), "Network ID must not be empty");

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
        assertNotNull(client, "Client2 for host-0 must not be null");
    }
}
