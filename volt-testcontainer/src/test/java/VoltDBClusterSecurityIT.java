/*
 * Copyright (C) 2025-2026 Volt Active Data Inc.
 *
 * Use of this source code is governed by an MIT
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/MIT.
 */

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.voltdb.VoltTable;
import org.voltdb.client.Client2;
import org.voltdb.client.ClientResponse;
import org.voltdb.client.ProcCallException;
import org.voltdbtest.testcontainer.VoltDBCluster;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Verifies security-related cluster configuration options: deployment XML
 * (inline and from a host path), username/password chaining, k-safety, and
 * multi-node security setup.
 */
public class VoltDBClusterSecurityIT extends TestBase {

    private VoltDBCluster cluster;

    @AfterEach
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
            assertSame(cluster, result, "Must return same instance");
        } finally {
            Files.delete(tempDeployment);
        }
    }

    @Test
    public void testWithDeploymentIsApplied() throws IOException, ProcCallException {
        cluster = new VoltDBCluster(validLicensePath, VOLTDB_IMAGE);
        cluster.withDeployment("custom-deployment.xml");
        cluster.start();

        assertEquals("2", readSitesPerHost(cluster),
                "Custom deployment passed via withDeployment() must be applied "
                + "(auto-generated default is 8)");
    }

    @Test
    public void testWithDeploymentContentIsApplied() throws IOException, ProcCallException {
        String customDeployment = """
                <?xml version="1.0" encoding="UTF-8"?>\
                <deployment>\
                    <cluster hostcount="1" sitesperhost="3" kfactor="0"/>\
                </deployment>""";

        cluster = new VoltDBCluster(validLicensePath, VOLTDB_IMAGE);
        cluster.withDeploymentContent(customDeployment);
        cluster.start();

        assertEquals("3", readSitesPerHost(cluster),
                "Custom deployment passed via withDeploymentContent() must be applied");
    }

    private static String readSitesPerHost(VoltDBCluster cluster)
            throws IOException, ProcCallException {
        ClientResponse resp = cluster.callProcedure("@SystemInformation", "DEPLOYMENT");
        assertEquals(ClientResponse.SUCCESS, resp.getStatus());

        for (VoltTable table : resp.getResults()) {
            while (table.advanceRow()) {
                if ("sitesperhost".equals(table.getString("PROPERTY"))) {
                    return table.getString("VALUE");
                }
            }
        }
        return null;
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
        assertSame(cluster, result, "Chaining must return same instance");

        cluster.start();
        Client2 client = cluster.getClient2();
        assertNotNull(client, "Client2 must not be null after start");
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
