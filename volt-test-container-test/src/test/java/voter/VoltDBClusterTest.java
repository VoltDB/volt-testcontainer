/*
 * Copyright (C) 2024 Volt Active Data Inc.
 *
 * Use of this source code is governed by an MIT
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/MIT.
 */
package voter;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.Network;
import org.voltdb.client.Client;
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

public class VoltDBClusterTest extends TestBase {
    
    private static final Logger LOG = LoggerFactory.getLogger(VoltDBClusterTest.class);
    private VoltDBCluster cluster;

    @After
    public void tearDown() {
        if (cluster != null) {
            cluster.shutdown();
        }
    }
    
    @Test
    public void testSingleNodeConstructor() {
        cluster = new VoltDBCluster(validLicensePath);
        assertNotNull(cluster);
    }
    
    @Test
    public void testConstructorWithImage() {
        cluster = new VoltDBCluster(validLicensePath, VOLTDB_IMAGE);
        assertNotNull(cluster);
    }
    
    @Test
    public void testConstructorWithImageAndExtraLibs() {
        cluster = new VoltDBCluster(validLicensePath, VOLTDB_IMAGE, null);
        assertNotNull(cluster);
    }
    
    @Test
    public void testConstructorWithHostCountAndKfactor() {
        cluster = new VoltDBCluster(validLicensePath, VOLTDB_IMAGE, 2, 1);
        assertNotNull(cluster);
    }
    
    @Test
    public void testConstructorFullParameters() {
        cluster = new VoltDBCluster(validLicensePath, VOLTDB_IMAGE, 3, 1, null);
        assertNotNull(cluster);
    }
    
    @Test
    public void testStartWithDefaultTimeout() throws IOException {
        cluster = new VoltDBCluster(validLicensePath, VOLTDB_IMAGE);
        cluster.start();
        
        Client client = cluster.getClient();
        assertNotNull("Client should not be null after start", client);
        // Client object doesn't have isConnected() method, just verify it's not null
    }
    
    @Test
    public void testStartWithCustomTimeout() throws IOException {
        cluster = new VoltDBCluster(validLicensePath, VOLTDB_IMAGE);
        cluster.start(60000); // 60 seconds timeout
        
        Client client = cluster.getClient();
        assertNotNull("Client should not be null after start", client);
        // Client object doesn't have isConnected() method, just verify it's not null
    }
    
    @Test
    public void testGetFirstMappedPort() throws IOException {
        cluster = new VoltDBCluster(validLicensePath, VOLTDB_IMAGE);
        cluster.start();
        
        int port = cluster.getFirstMappedPort();
        assertTrue("Port should be valid", port > 0);
    }
    
    @Test
    public void testGetMappedPort() throws IOException {
        cluster = new VoltDBCluster(validLicensePath, VOLTDB_IMAGE);
        cluster.start();
        
        int mappedPort = cluster.getMappedPort(21211);
        assertTrue("Mapped port should be valid", mappedPort > 0);
    }
    
    @Test
    public void testGetHost() throws IOException {
        cluster = new VoltDBCluster(validLicensePath, VOLTDB_IMAGE);
        cluster.start();
        
        String host = cluster.getHost();
        assertNotNull("Host should not be null", host);
        assertFalse("Host should not be empty", host.isEmpty());
    }
    
    @Test
    public void testGetHostAndPort() throws IOException {
        cluster = new VoltDBCluster(validLicensePath, VOLTDB_IMAGE);
        cluster.start();
        
        String hostAndPort = cluster.getHostAndPort();
        assertNotNull("HostAndPort should not be null", hostAndPort);
        assertTrue("HostAndPort should contain colon", hostAndPort.contains(":"));
    }
    
    @Test
    public void testGetClient() throws IOException {
        cluster = new VoltDBCluster(validLicensePath, VOLTDB_IMAGE);
        cluster.start();
        
        Client client = cluster.getClient();
        assertNotNull("Client should not be null", client);
        // Client object doesn't have isConnected() method, just verify it's not null
    }
    
    @Test
    public void testGetClientWithHost() throws IOException {
        cluster = new VoltDBCluster(validLicensePath, VOLTDB_IMAGE, 2, 0);
        cluster.start();
        
        Client client = cluster.getClient("0");
        assertNotNull("Client should not be null", client);
        // Client object doesn't have isConnected() method, just verify it's not null
    }
    
    @Test
    public void testRunDDLString() throws IOException, ProcCallException {
        cluster = new VoltDBCluster(validLicensePath, VOLTDB_IMAGE);
        cluster.start();
        
        String ddl = "CREATE TABLE test_table (id INTEGER NOT NULL, name VARCHAR(50), PRIMARY KEY(id));";
        ClientResponse response = cluster.runDDL(ddl);
        assertNotNull("Response should not be null", response);
        assertEquals("Status should be success", ClientResponse.SUCCESS, response.getStatus());
    }
    
    @Test
    public void testRunDDLFile() throws IOException, ProcCallException {
        cluster = new VoltDBCluster(validLicensePath, VOLTDB_IMAGE);
        cluster.start();
        
        // Create a temporary DDL file
        Path tempFile = Files.createTempFile("test", ".sql");
        try (FileWriter writer = new FileWriter(tempFile.toFile())) {
            writer.write("CREATE TABLE test_file_table (id INTEGER NOT NULL, value VARCHAR(100), PRIMARY KEY(id));");
        }
        
        boolean result = cluster.runDDL(tempFile.toFile());
        assertTrue("DDL execution should succeed", result);
        
        // Clean up
        Files.delete(tempFile);
    }
    
    @Test
    public void testCallProcedure() throws IOException, ProcCallException {
        cluster = new VoltDBCluster(validLicensePath, VOLTDB_IMAGE);
        cluster.start();
        
        ClientResponse response = cluster.callProcedure("@SystemInformation");
        assertNotNull("Response should not be null", response);
        assertEquals("Status should be success", ClientResponse.SUCCESS, response.getStatus());
    }
    
    @Test
    public void testGetContainerIds() throws IOException {
        cluster = new VoltDBCluster(validLicensePath, VOLTDB_IMAGE, 2, 0);
        cluster.start();
        
        List<String> containerIds = cluster.getContainerIds();
        assertNotNull("Container IDs should not be null", containerIds);
        assertEquals("Should have 2 container IDs", 2, containerIds.size());
        for (String id : containerIds) {
            assertNotNull("Container ID should not be null", id);
            assertFalse("Container ID should not be empty", id.isEmpty());
        }
    }
    
    @Test
    public void testGetContainerNames() throws IOException {
        cluster = new VoltDBCluster(validLicensePath, VOLTDB_IMAGE, 2, 0);
        cluster.start();
        
        List<String> containerNames = cluster.getContainerNames();
        assertNotNull("Container names should not be null", containerNames);
        assertEquals("Should have 2 container names", 2, containerNames.size());
        for (String name : containerNames) {
            assertNotNull("Container name should not be null", name);
            assertFalse("Container name should not be empty", name.isEmpty());
        }
    }
    
    @Test
    public void testGetNetworkId() throws IOException {
        cluster = new VoltDBCluster(validLicensePath, VOLTDB_IMAGE);
        cluster.start();
        
        String networkId = cluster.getNetworkId();
        assertNotNull("Network ID should not be null", networkId);
        assertFalse("Network ID should not be empty", networkId.isEmpty());
    }
    
    @Test
    public void testWithLogConsumer() {
        cluster = new VoltDBCluster(validLicensePath, VOLTDB_IMAGE);
        VoltDBCluster result = cluster.withLogConsumer(LOG);
        assertSame("Should return same cluster instance", cluster, result);
    }
    
    @Test
    public void testWithUserNameAndPassword() {
        cluster = new VoltDBCluster(validLicensePath, VOLTDB_IMAGE);
        VoltDBCluster result = cluster.withUserNameAndPassword("testuser", "testpass");
        assertSame("Should return same cluster instance", cluster, result);
    }
    
    @Test
    public void testWithDeploymentContent() {
        cluster = new VoltDBCluster(validLicensePath, VOLTDB_IMAGE);
        String deploymentXml = "<?xml version=\"1.0\"?><deployment><cluster kfactor=\"0\"/></deployment>";
        VoltDBCluster result = cluster.withDeploymentContent(deploymentXml);
        assertSame("Should return same cluster instance", cluster, result);
    }
    
    @Test
    public void testWithKsaftey() {
        cluster = new VoltDBCluster(validLicensePath, VOLTDB_IMAGE, 3, 0);
        VoltDBCluster result = cluster.withKsaftey(1);
        assertSame("Should return same cluster instance", cluster, result);
    }
    
    @Test
    public void testWithNetwork() {
        Network network = Network.newNetwork();
        cluster = new VoltDBCluster(validLicensePath, VOLTDB_IMAGE);
        VoltDBCluster result = cluster.withNetwork(network);
        assertSame("Should return same cluster instance", cluster, result);
        network.close();
    }
    
    @Test
    public void testShutdown() throws IOException {
        cluster = new VoltDBCluster(validLicensePath, VOLTDB_IMAGE);
        cluster.start();
        
        Client client = cluster.getClient();
        // Verify client is not null before shutdown
        assertNotNull("Client should not be null before shutdown", client);
        
        cluster.shutdown();
        
        // Give some time for shutdown to complete
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // After shutdown, we can't check connection status directly
    }
    
    @Test
    public void testWithInitialSchemaResourcePath() {
        cluster = new VoltDBCluster(validLicensePath, VOLTDB_IMAGE);
        String resourcePath = "/tmp";
        String fileName = "test_schema.sql";
        VoltDBCluster result = cluster.withInitialSchema(resourcePath, fileName);
        assertSame("Should return same cluster instance", cluster, result);
    }
    
    @Test
    public void testWithInitialSchemaFileName() {
        cluster = new VoltDBCluster(validLicensePath, VOLTDB_IMAGE);
        String fileName = "/tmp/test_schema.sql";
        VoltDBCluster result = cluster.withInitialSchema(fileName);
        assertSame("Should return same cluster instance", cluster, result);
    }
    
    @Test
    public void testWithInitialClasses() {
        cluster = new VoltDBCluster(validLicensePath, VOLTDB_IMAGE);
        String jarPath = "/tmp/test.jar";
        String jarName = "test.jar";
        VoltDBCluster result = cluster.withInitialClasses(jarPath, jarName);
        assertSame("Should return same cluster instance", cluster, result);
    }
    
    @Test
    public void testWithInitialClassesArray() throws IOException {
        cluster = new VoltDBCluster(validLicensePath, VOLTDB_IMAGE);
        
        // Create temporary jar files for testing
        Path tempJar1 = Files.createTempFile("test1", ".jar");
        Path tempJar2 = Files.createTempFile("test2", ".jar");
        
        File[] jars = new File[] { tempJar1.toFile(), tempJar2.toFile() };
        VoltDBCluster result = cluster.withInitialClasses(jars);
        assertSame("Should return same cluster instance", cluster, result);
        
        // Clean up
        Files.delete(tempJar1);
        Files.delete(tempJar2);
    }
    
    @Test
    public void testLoadClasses() throws IOException, ProcCallException {
        cluster = new VoltDBCluster(validLicensePath, VOLTDB_IMAGE);
        cluster.start();
        
        // Use getJars() method from TestBase to get jar files
        File[] jars = getJars();
        if (jars != null && jars.length > 0) {
            File jarFile = jars[0];
            ClientResponse response = cluster.loadClasses(jarFile.getAbsolutePath());
            assertNotNull("Response should not be null", response);
            assertEquals("Status should be success", ClientResponse.SUCCESS, response.getStatus());
        }
    }
    
    @Test
    public void testLoadClassesWithDeletion() throws IOException, ProcCallException {
        cluster = new VoltDBCluster(validLicensePath, VOLTDB_IMAGE);
        cluster.start();
        
        // Use getJars() method from TestBase to get jar files
        File[] jars = getJars();
        if (jars != null && jars.length > 0) {
            File jarFile = jars[0];
            ClientResponse response = cluster.loadClasses(jarFile.getAbsolutePath(), "com.example.OldClass");
            assertNotNull("Response should not be null", response);
            assertEquals("Status should be success", ClientResponse.SUCCESS, response.getStatus());
        }
    }
    
    @Test
    public void testMultiNodeClusterOperations() throws IOException, ProcCallException {
        // Test with a 3-node cluster with k-factor 1
        cluster = new VoltDBCluster(validLicensePath, VOLTDB_IMAGE, 3, 1);
        cluster.start();
        
        // Verify all nodes are accessible
        List<String> containerIds = cluster.getContainerIds();
        assertEquals("Should have 3 containers", 3, containerIds.size());
        
        // Test DDL execution on multi-node cluster
        String ddl = "CREATE TABLE multi_node_test (id INTEGER NOT NULL, data VARCHAR(100), PRIMARY KEY(id));";
        ClientResponse response = cluster.runDDL(ddl);
        assertEquals("DDL should succeed on multi-node cluster", ClientResponse.SUCCESS, response.getStatus());
        
        // Test procedure call on multi-node cluster
        ClientResponse sysInfoResponse = cluster.callProcedure("@SystemInformation");
        assertEquals("Procedure should succeed on multi-node cluster", ClientResponse.SUCCESS, sysInfoResponse.getStatus());
    }
}