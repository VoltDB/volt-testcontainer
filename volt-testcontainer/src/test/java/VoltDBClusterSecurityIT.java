/*
 * Copyright (C) 2024-2025 Volt Active Data Inc.
 *
 * Use of this source code is governed by an MIT
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/MIT.
 */

import org.junit.After;
import org.junit.Test;
import org.voltdb.client.Client2;
import org.voltdb.client.ClientResponse;
import org.voltdb.client.ProcCallException;
import org.voltdbtest.testcontainer.VoltDBCluster;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.*;

/**
 * Verifies security-related cluster configuration options: deployment XML
 * (inline and from a host path), username/password chaining, k-safety, and
 * multi-node security setup.
 */
public class VoltDBClusterSecurityIT extends TestBase {

    private VoltDBCluster cluster;

    @After
    public void tearDown() {
        if (cluster != null) {
            cluster.shutdown();
            cluster = null;
        }
    }

    /**
     * {@code withDeploymentResourceFromHostPath()} accepts a deployment file
     * and returns the same cluster instance for chaining.
     */
    @Test
    public void testWithDeploymentResource() throws IOException {
        cluster = new VoltDBCluster(validLicensePath, VOLTDB_IMAGE);

        Path tempDeployment = Files.createTempFile("deployment", ".xml");
        try (FileWriter w = new FileWriter(tempDeployment.toFile())) {
            w.write("<?xml version=\"1.0\"?>\n");
            w.write("<deployment>\n");
            w.write("    <cluster kfactor=\"0\"/>\n");
            w.write("    <paths><voltdbroot path=\"voltdbroot\"/></paths>\n");
            w.write("</deployment>");
        }
        try {
            VoltDBCluster result = cluster.withDeploymentResourceFromHostPath(
                    tempDeployment.toAbsolutePath().toString());
            assertSame("Must return same instance", cluster, result);
        } finally {
            Files.delete(tempDeployment);
        }
    }

    /**
     * Multiple security configurations can be chained before {@code start()};
     * the cluster must start and accept connections.
     */
    @Test
    public void testSecureClusterConfiguration() throws IOException {
        cluster = new VoltDBCluster(validLicensePath, VOLTDB_IMAGE);
        VoltDBCluster result = cluster
                .withUserNameAndPassword("admin", "admin123")
                .withDeploymentContent(
                        "<?xml version=\"1.0\"?><deployment><cluster kfactor=\"0\"/></deployment>")
                .withKsafety(0);
        assertSame("Chaining must return same instance", cluster, result);

        cluster.start();
        Client2 client = cluster.getClient2();
        assertNotNull("Client2 must not be null after start", client);
    }

    /**
     * A 3-node cluster with k=1 starts, reports 3 containers, and shares a
     * network ID.
     */
    @Test
    public void testMultiNodeSecureCluster() throws IOException, ProcCallException {
        cluster = new VoltDBCluster(validLicensePath, VOLTDB_IMAGE, 3, 1);
        cluster
            .withUserNameAndPassword("testuser", "testpass")
            .withDeploymentContent(
                    "<?xml version=\"1.0\"?><deployment><cluster kfactor=\"1\"/></deployment>");

        cluster.start();

        assertEquals(3, cluster.getContainerIds().size());
        assertEquals(3, cluster.getContainerNames().size());

        String networkId = cluster.getNetworkId();
        assertNotNull(networkId);
        assertFalse(networkId.isEmpty());

        assertEquals(ClientResponse.SUCCESS,
                cluster.callProcedure("@SystemInformation").getStatus());
    }
}
