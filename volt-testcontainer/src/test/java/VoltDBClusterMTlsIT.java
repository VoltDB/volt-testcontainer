/*
 * Copyright (C) 2025-2026 Volt Active Data Inc.
 *
 * Use of this source code is governed by an MIT
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/MIT.
 */

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.voltdb.client.Client;
import org.voltdb.client.Client2;
import org.voltdb.client.ClientResponse;
import org.voltdb.client.ProcCallException;
import org.voltdbtest.testcontainer.VoltDBCluster;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Integration test for mutual-TLS support in {@link VoltDBCluster#getClient2()}.
 *
 * <p>This test boots a VoltDB cluster with an mTLS-required deployment, then
 * asserts that both the legacy {@link Client} and the
 * modern {@link Client2} can connect and successfully execute a procedure.
 * Before the fix, the Client2 assertion fails because the broker rejects the
 * connection with {@code "client certificate required for mTLS"}.
 */
public class VoltDBClusterMTlsIT extends TestBase {

    private static final String KEYSTORE_PASSWORD = "topsecret";
    private static final String TRUSTSTORE_PASSWORD = "topsecret";

    private static final String USERNAME = "john";
    private static final String PASSWORD = "john";

    private VoltDBCluster cluster;

    @AfterEach
    public void tearDown() {
        if (cluster != null) {
            cluster.shutdown();
        }
    }

    @Test
    public void legacyClientConnectsToMutualTlsCluster() throws IOException, ProcCallException {
        cluster = startMutualTlsCluster();

        Client client = cluster.getClient();
        assertNotNull(client, "Legacy Client must not be null");

        ClientResponse response = client.callProcedure("@Ping");
        assertEquals(ClientResponse.SUCCESS, response.getStatus(),
                "Legacy Client must be able to ping an mTLS cluster");
    }

    @Test
    public void client2ConnectsToMutualTlsCluster() throws IOException, ProcCallException {
        cluster = startMutualTlsCluster();

        Client2 client2 = cluster.getClient2();
        assertNotNull(client2, "Client2 must not be null");

        ClientResponse response = client2.callProcedureSync("@Ping");
        assertEquals(ClientResponse.SUCCESS, response.getStatus(),
                "Client2 must be able to ping an mTLS cluster");
    }

    private VoltDBCluster startMutualTlsCluster() throws IOException {
        VoltDBCluster c = new VoltDBCluster(validLicensePath, VOLTDB_IMAGE)
                .withDeployment("mtls-deployment.xml")
                .withTruststore("volt.truststore.jks", TRUSTSTORE_PASSWORD)
                .withKeystore("volt.keystore.jks", KEYSTORE_PASSWORD)
                .withUserNameAndPassword(USERNAME, PASSWORD);
        c.start();
        return c;
    }
}
