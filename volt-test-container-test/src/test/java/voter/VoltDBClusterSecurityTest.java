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
import org.voltdbtest.testcontainer.VoltDBCluster;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.*;

public class VoltDBClusterSecurityTest extends TestBase {
    
    private VoltDBCluster cluster;

    @After
    public void tearDown() {
        if (cluster != null) {
            cluster.shutdown();
        }
    }
    
    @Test
    public void testWithDeploymentResource() throws IOException {
        cluster = new VoltDBCluster(validLicensePath, VOLTDB_IMAGE);
        
        // Create a deployment XML file
        Path tempDeployment = Files.createTempFile("deployment", ".xml");
        try (FileWriter writer = new FileWriter(tempDeployment.toFile())) {
            writer.write("<?xml version=\"1.0\"?>\n");
            writer.write("<deployment>\n");
            writer.write("    <cluster kfactor=\"0\"/>\n");
            writer.write("    <paths>\n");
            writer.write("        <voltdbroot path=\"voltdbroot\"/>\n");
            writer.write("    </paths>\n");
            writer.write("</deployment>");
        }
        
        try {
            VoltDBCluster result = cluster.withDeploymentResource(tempDeployment.toAbsolutePath().toString());
            assertSame("Should return same cluster instance", cluster, result);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            Files.delete(tempDeployment);
        }
    }
    
    @Test
    public void testSecureClusterConfiguration() throws IOException {
        cluster = new VoltDBCluster(validLicensePath, "voltdb/voltdb-enterprise:14.1.0");
        
        // Test chaining multiple security configurations
        VoltDBCluster result = cluster
            .withUserNameAndPassword("admin", "admin123")
            .withDeploymentContent("<?xml version=\"1.0\"?><deployment><cluster kfactor=\"0\"/></deployment>")
            .withKsaftey(0);
            
        assertSame("Should return same cluster instance after chaining", cluster, result);
        
        // Start the cluster with security configuration
        cluster.start();
        
        // Verify cluster is running
        assertNotNull("Client should not be null", cluster.getClient());
    }
    
    @Test
    public void testMultiNodeSecureCluster() throws IOException {
        // Create a 3-node cluster with security configurations
        cluster = new VoltDBCluster(validLicensePath, "voltdb/voltdb-enterprise:14.1.0", 3, 1);
        
        // Apply security configurations
        cluster
            .withUserNameAndPassword("testuser", "testpass")
            .withDeploymentContent("<?xml version=\"1.0\"?><deployment><cluster kfactor=\"1\"/></deployment>");
        
        // Start the secure cluster
        cluster.start();
        
        // Verify all nodes started
        assertEquals("Should have 3 containers", 3, cluster.getContainerIds().size());
        assertEquals("Should have 3 container names", 3, cluster.getContainerNames().size());
        
        // Verify network is shared
        String networkId = cluster.getNetworkId();
        assertNotNull("Network ID should not be null", networkId);
        assertFalse("Network ID should not be empty", networkId.isEmpty());
    }
}