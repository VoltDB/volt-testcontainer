/*
 * Copyright (C) 2024-2026 Volt Active Data Inc.
 *
 * Use of this source code is governed by an MIT
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/MIT.
 */

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.voltdb.VoltTable;
import org.voltdb.client.Client2;
import org.voltdb.client.ClientResponse;
import org.voltdb.client.ProcCallException;
import org.voltdbtest.testcontainer.VoltDBCluster;
import org.voltdbtest.testcontainer.VoltDBContainer;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

/**
 * Verifies {@link VoltDBContainer.NetworkType} HOST and DOCKER modes,
 * and that a custom topic public interface is advertised correctly.
 */
public class NetworkTypeIT extends TestBase {

    private VoltDBCluster  cluster;
    private VoltDBContainer container;

    @AfterEach
    public void tearDown() {
        if (cluster   != null) cluster.shutdown();
        if (container != null) container.stop();
    }

    /** In HOST network mode the topic public interface should be "localhost". */
    @Test
    public void testMapPortInHostMode() throws IOException, ProcCallException {
        cluster = new VoltDBCluster(validLicensePath, VOLTDB_IMAGE);
        cluster.setNetworkType(VoltDBContainer.NetworkType.HOST);
        cluster.start(60000);

        Client2 client = cluster.getClient2();
        ClientResponse resp = client.callProcedureSync("@SystemInformation", "OVERVIEW");
        VoltTable table = resp.getResults()[0];

        String publicHost = null;
        String publicPort = null;
        while (table.advanceRow()) {
            switch (table.getString("KEY")) {
                case "TOPICSPUBLICINTERFACE": publicHost = table.getString("VALUE"); break;
                case "TOPICSPUBLICPORT":      publicPort = table.getString("VALUE"); break;
            }
        }
        assertEquals("localhost", publicHost);
        assertEquals(Integer.toString(cluster.getMappedPort(9092)), publicPort);
    }

    /** In DOCKER network mode each node advertises its hostname alias (host-0, host-1, …). */
    @Test
    public void testUseHostNamesAsAliases() throws IOException, ProcCallException {
        Network network = Network.newNetwork();
        cluster = new VoltDBCluster(validLicensePath, VOLTDB_IMAGE, 3, 0);
        cluster.withNetwork(network);
        cluster.setNetworkType(VoltDBContainer.NetworkType.DOCKER);
        cluster.start(60000);

        Client2 client = cluster.getClient2();
        ClientResponse resp = client.callProcedureSync("@SystemInformation", "OVERVIEW");

        Set<String>  publicHosts = new HashSet<>();
        Set<Integer> publicPorts = new HashSet<>();
        for (VoltTable table : resp.getResults()) {
            while (table.advanceRow()) {
                switch (table.getString("KEY")) {
                    case "TOPICSPUBLICINTERFACE":
                        publicHosts.add(table.getString("VALUE")); break;
                    case "TOPICSPUBLICPORT":
                        publicPorts.add(Integer.parseInt(table.getString("VALUE"))); break;
                }
            }
        }
        assertThat(publicHosts).containsOnly("host-0", "host-1", "host-2");
        assertThat(publicPorts).containsExactly(9092);
    }

    /** A custom topic public interface supplied via {@code setTopicPublicInterface} is advertised. */
    @Test
    public void testUseSuppliedTopicPublicAddress() throws IOException, ProcCallException {
        Network network = Network.newNetwork();
        container = new VoltDBContainer(0, validLicensePath, VOLTDB_IMAGE, 1, 0,
                "--ignore=thp --count=1", null);
        container.withNetwork(network);
        container.setNetworkType(VoltDBContainer.NetworkType.DOCKER);
        container.setTopicPublicInterface("foobar");
        container.withLogConsumer(
                new Slf4jLogConsumer(LoggerFactory.getLogger(NetworkTypeIT.class)));

        container.start();
        await().until(container::isRunning);

        Client2 client = container.getConnectedClient2();
        ClientResponse resp = client.callProcedureSync("@SystemInformation", "OVERVIEW");

        Set<String>  publicHosts = new HashSet<>();
        Set<Integer> publicPorts = new HashSet<>();
        for (VoltTable table : resp.getResults()) {
            while (table.advanceRow()) {
                switch (table.getString("KEY")) {
                    case "TOPICSPUBLICINTERFACE":
                        publicHosts.add(table.getString("VALUE")); break;
                    case "TOPICSPUBLICPORT":
                        publicPorts.add(Integer.parseInt(table.getString("VALUE"))); break;
                }
            }
        }
        assertThat(publicHosts).containsOnly("foobar");
        assertThat(publicPorts).containsExactly(9092);
    }
}
