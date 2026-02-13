package voter.client1;

import org.junit.After;
import org.junit.Test;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.voltdb.VoltTable;
import org.voltdb.client.Client;
import org.voltdb.client.ClientResponse;
import org.voltdb.client.ProcCallException;
import org.voltdbtest.testcontainer.VoltDBCluster;
import org.voltdbtest.testcontainer.VoltDBContainer;
import voter.TestBase;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

public class NetworkTypeTest extends TestBase {

    private VoltDBCluster cluster;
    private VoltDBContainer container;

    @After
    public void tearDown() {
        if (cluster != null) {
            cluster.shutdown();
        }

        if (container != null) {
            container.stop();
        }
    }

    @Test
    public void testMapPortInHostMode() throws IOException, ProcCallException {
        cluster = new VoltDBCluster(validLicensePath, VOLTDB_IMAGE);
        cluster.setNetworkType(VoltDBContainer.NetworkType.HOST);
        cluster.start(60000); // 60 seconds timeout

        Client client = cluster.getClient();
        ClientResponse clientResponse = client.callProcedure("@SystemInformation", "OVERVIEW");
        VoltTable table = clientResponse.getResults()[0];

        String publicHost = null;
        String publicPort = null;

        while (table.advanceRow()) {
            switch (table.getString("KEY")) {
                case "TOPICSPUBLICINTERFACE":
                    publicHost = table.getString("VALUE");
                    break;
                case "TOPICSPUBLICPORT":
                    publicPort = table.getString("VALUE");
                    break;
            }
        }

        assertEquals("localhost", publicHost);
        assertEquals(Integer.toString(cluster.getMappedPort(9092)), publicPort);
    }

    @Test
    public void testUseHostNamesAsAliases() throws IOException, ProcCallException {
        Network network = Network.newNetwork();

        cluster = new VoltDBCluster(validLicensePath, VOLTDB_IMAGE, 3, 0);
        cluster.withNetwork(network);
        cluster.setNetworkType(VoltDBContainer.NetworkType.DOCKER);
        cluster.start(60000); // 60 seconds timeout

        Client client = cluster.getClient();
        ClientResponse clientResponse = client.callProcedure("@SystemInformation", "OVERVIEW");

        Set<String> publicHosts = new HashSet<>();
        Set<Integer> publicPorts = new HashSet<>();

        VoltTable[] tables = clientResponse.getResults();
        for (VoltTable table : tables) {
            while (table.advanceRow()) {
                switch (table.getString("KEY")) {
                    case "TOPICSPUBLICINTERFACE":
                        publicHosts.add(table.getString("VALUE"));
                        break;
                    case "TOPICSPUBLICPORT":
                        publicPorts.add(Integer.parseInt(table.getString("VALUE")));
                        break;
                }
            }

        }

        assertThat(publicHosts).containsOnly("host-0", "host-1", "host-2");
        assertThat(publicPorts).containsExactly(9092);
    }

    @Test
    public void testUseSuppliedTopicPublicAddress() throws IOException, ProcCallException {
        Network network = Network.newNetwork();

        container = new VoltDBContainer(0, validLicensePath, VOLTDB_IMAGE, 1, 0,
                "--ignore=thp --count=1", null);
        container.withNetwork(network);
        container.setNetworkType(VoltDBContainer.NetworkType.DOCKER);
        container.setTopicPublicInterface("foobar");
        container.withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger(NetworkTypeTest.class)));

        container.start(); // 60 seconds timeout
        await().until(container::isRunning);

        Client client = container.getConnectedClient();
        ClientResponse clientResponse = client.callProcedure("@SystemInformation", "OVERVIEW");

        Set<String> publicHosts = new HashSet<>();
        Set<Integer> publicPorts = new HashSet<>();

        VoltTable[] tables = clientResponse.getResults();
        for (VoltTable table : tables) {
            while (table.advanceRow()) {
                switch (table.getString("KEY")) {
                    case "TOPICSPUBLICINTERFACE":
                        publicHosts.add(table.getString("VALUE"));
                        break;
                    case "TOPICSPUBLICPORT":
                        publicPorts.add(Integer.parseInt(table.getString("VALUE")));
                        break;
                }
            }

        }

        assertThat(publicHosts).containsOnly("foobar");
        assertThat(publicPorts).containsExactly(9092);
    }
}
