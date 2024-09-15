/*
 * Copyright (C) 2024 Volt Active Data Inc.
 *
 * Use of this source code is governed by an MIT
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/MIT.
 */
package org.voltdbtest.testcontainer;

import com.github.dockerjava.api.command.InspectContainerResponse;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.OutputFrame;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.PullPolicy;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;
import org.voltdb.client.Client;
import org.voltdb.client.ClientConfig;
import org.voltdb.client.ClientFactory;
import org.voltdb.client.ClientResponse;
import org.voltdb.client.ProcCallException;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.time.Duration;

/**
 * A local containerized cluster which takes host alias, docker image name
 * a valid license file, deployment file and voltdb start options.
 *
 * @author akhanzode
 * @version $Id: $Id
 */
public class VoltDBContainer extends GenericContainer<VoltDBContainer> {
    private static final Network NETWORK = Network.newNetwork();

    String startScript = "#!/bin/sh\n" +
            "# This file is part of VoltDB.\n" +
            "# Copyright (C) 2022 Volt Active Data Inc.\n" +
            "#\n" +
            "# A simple script to (optionally) initialize a voltdb directory and start\n" +
            "# a voltdb instance.  Suitable for simple container testing\n" +
            "#\n" +
            "# Assumes license is mounted at /etc/voltdb-license.xml, otherwise\n" +
            "# set VOLTDB_LICENSE environment variable\n" +
            "\n" +
            ": ${VOLTDB_START_CONFIG:=}\n" +
            ": ${VOLTDB_DIR:=$(pwd)}\n" +
            ": ${VOLTDB_CONFIG:=}\n" +
            ": ${VOLTDB_LICENSE:=/etc/voltdb-license.xml}\n" +
            ": ${VOLTDB_SCHEMA:=/etc/schemas}\n" +
            ": ${VOLTDB_CLASSES:=/etc/classes}\n" +
            "\n" +
            "s=\"\"\n" +
            "if [ -n \"${VOLTDB_SCHEMA}\" -a -e \"${VOLTDB_SCHEMA}\" ] ; then\n" +
            "  s=`ls ${VOLTDB_SCHEMA}/*.ddl ${VOLTDB_SCHEMA}/*.sql | tr '\\n' ',' | sed 's/,$/\\n/'`\n" +
            "fi\n" +
            "\n" +
            "j=\"\"\n" +
            "if [ -n \"${VOLTDB_CLASSES}\" -a -e \"${VOLTDB_CLASSES}\" ] ; then\n" +
            "  j=`ls ${VOLTDB_CLASSES}/*.jar | tr '\\n' ',' | sed 's/,$/\\n/'`\n" +
            "fi\n" +
            "\n" +
            "echo \"Schemas requested to load: \" $s\n" +
            "echo \"Classes requested to load: \" $j\n" +
            "\n" +
            "if [ ! -e ${VOLTDB_DIR}/voltdbroot ] ; then\n" +
            "    if [ -n \"${VOLTDB_CONFIG}\" -a -e \"${VOLTDB_CONFIG}\" ] ; then\n" +
            "        INIT_CMD=\"voltdb init -C ${VOLTDB_CONFIG} -D ${VOLTDB_DIR} --license=${VOLTDB_LICENSE}\"\n" +
            "    else\n" +
            "        INIT_CMD=\"voltdb init -D ${VOLTDB_DIR} --license=${VOLTDB_LICENSE}\"\n" +
            "    fi\n" +
            "    if [ ! -z $s ] ; then\n" +
            "        INIT_CMD=\"$INIT_CMD -s $s\"\n" +
            "    fi\n" +
            "    if [ ! -z $j ] ; then\n" +
            "        INIT_CMD=\"$INIT_CMD -j $j\"\n" +
            "    fi\n" +
            "    echo $INIT_CMD\n" +
            "    eval $INIT_CMD\n" +
            "fi\n" +
            "\n" +
            "exec voltdb start -D ${VOLTDB_DIR} ${VOLTDB_START_CONFIG} --topicspublic=%s --drpublic=%s \"$@\"\n";

    String deploymentTemplate = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
            "<deployment>\n" +
            "    <cluster hostcount=\"%d\" sitesperhost=\"8\" kfactor=\"%d\"/>\n" +
            "    <metrics enabled=\"true\" interval=\"60s\" maxbuffersize=\"200\" />\n" +
            "</deployment>\n";

    // This client is created automatically when cluster is up, dont close this its used for internal healthcheck.
    Client client;
    private final String hostId;
    private boolean tlsEnabled = false;
    private String username = "";
    private String password = "";
    private String trustStorePassword = "";
    private String keyStorePassword = "";
    private String keyStorePath = "";
    private String trustStorePath = "";
    private int kfactor = 0;
    private int hostcount = 1;

    /**
     * Creates a VoltDB container with the specified parameters.
     *
     * @param id           the ID of the container
     * @param image        the image name of the Docker container
     * @param licensePath  the path to the license file
     * @param hostcount    the number of hosts in the cluster
     * @param kfactor      kfactor of voltdb cluster.
     * @param startCommand the start command for the VoltDB container
     * @param extraLibs    folder from where extra jars need to be added to server extension directory
     */
    public VoltDBContainer(int id, String licensePath, String image, int hostcount, int kfactor, String startCommand, String extraLibs) {
        this(id, licensePath, image, hostcount, kfactor, null, startCommand, extraLibs);
    }

    private String startCommand;

    /**
     * Creates a VoltDB container with the specified parameters.
     *
     * @param id           the ID of the container
     * @param image        the image name of the Docker container
     * @param licensePath  the path to the license file
     * @param hostcount    the number of hosts in the cluster
     * @param kfactor      kfactor of voltdb cluster.
     * @param deployment   the deployment information (optional, if null, it will be generated based on hostcount and kfactor)
     * @param startCommand the start command for the VoltDB container
     * @param extraLibs    folder from where extra jars need to be added to the server extension directory
     */
    public VoltDBContainer(int id, String licensePath, String image, int hostcount, int kfactor, String deployment, String startCommand, String extraLibs) {
        super(DockerImageName.parse(image));
        this.hostcount = hostcount;
        this.kfactor = kfactor;
        hostId = "host-" + id;
        if (deployment == null) {
            deployment = getDeployment();
        }
        GenericContainer container = withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger(hostId)));
        withEnv("VOLTDB_START_CONFIG", startCommand);
        withEnv("VOLTDB_CONFIG", "/etc/deployment.xml");
        withEnv("VOLTDB_OPTS",
                "-Dlog4j.configuration=file:///opt/voltdb/tools/kubernetes/console-log4j.xml "
                        + " --add-opens=java.base/java.net=ALL-UNNAMED"
                        + " --add-opens=java.base/java.lang.reflect=ALL-UNNAMED");
        withNetworkMode(NETWORK.getId());
        withNetwork(NETWORK);
        withNetworkAliases(hostId);
        withImagePullPolicy(PullPolicy.defaultPolicy());
        withCopyToContainer(MountableFile.forHostPath(licensePath), "/etc/voltdb-license.xml");
        withCopyToContainer(Transferable.of(deployment), "/etc/deployment.xml");
        withExposedPorts(21212, 21211, 9092, 5555);
        setWaitStrategy(Wait.forSuccessfulCommand("echo")
                .withStartupTimeout(Duration.ofSeconds(120L)));
        withCreateContainerCmdModifier(cmd -> cmd.withHostName(hostId));
        withReuse(true);
        // If requested to copy jars do it here.
        if (extraLibs != null) {
            File[] jars = getJars(extraLibs);
            for (File jar : jars) {
                String name = jar.getName();
                container.withCopyToContainer(MountableFile.forHostPath(jar.getAbsolutePath()), "/opt/voltdb/lib/extension/" + name);
            }
        }
    }

    private File @Nullable [] getJars(String path) {
        File targetDir = new File(path);
        FileFilter jarFiles = pathname -> {
            if (pathname.isDirectory()) {
                return false;
            }
            String name = pathname.getName();
            return name.endsWith(".jar");
        };
        File[] jars = targetDir.listFiles(jarFiles);
        return jars;
    }

    @Override
    protected void configure() {
        // START_SCRIPT waiter
        this.withCommand("/bin/bash", "-c",
                "while [ ! -f /opt/voltdb/tools/entrypoint.sh ]; " +
                        "do sleep 1; done; /opt/voltdb/tools/entrypoint.sh"
        );
    }

    @Override
    protected void containerIsStarting(InspectContainerResponse containerInfo) {
        super.containerIsStarting(containerInfo);
        String topicPublic = "localhost:" + getMappedPort(9092);
        String drpublic = "localhost:" + getMappedPort(5555);
        // Its twice here because script has a echo.
        String finalScript = String.format(startScript, topicPublic, drpublic);
        copyFileToContainer(Transferable.of(finalScript, 511), "/opt/voltdb/tools/entrypoint.sh");
        Slf4jLogConsumer logConsumer = new Slf4jLogConsumer(LoggerFactory.getLogger(hostId)).withSeparateOutputStreams();
        withLogConsumer(logConsumer);
        followOutput(logConsumer, OutputFrame.OutputType.STDOUT, OutputFrame.OutputType.STDERR);
    }

    /**
     * Executes the given DDL schema on the VoltDB cluster.
     *
     * @param schema the DDL schema to execute
     * @return a {@link ClientResponse} object representing the result of the DDL execution
     * @throws IOException       if an I/O error occurs while executing the DDL schema
     * @throws ProcCallException if an error occurs during the DDL execution process
     */
    public ClientResponse runDDL(String schema) throws IOException, ProcCallException {
        return client.callProcedure("@AdHoc", schema);
    }

    /**
     * <p>loadClasses.</p>
     *
     * @param jar a {@link java.lang.String} object
     * @return a {@link org.voltdb.client.ClientResponse} object
     * @throws java.io.IOException                 if any.
     * @throws org.voltdb.client.ProcCallException if any.
     */
    public ClientResponse loadClasses(String jar) throws IOException, ProcCallException {
        return client.updateClasses(new File(jar), null);
    }

    /**
     * <p>loadClasses.</p>
     *
     * @param jar             a {@link java.lang.String} object
     * @param classesToDelete a {@link java.lang.String} object
     * @return a {@link org.voltdb.client.ClientResponse} object
     * @throws java.io.IOException                 if any.
     * @throws org.voltdb.client.ProcCallException if any.
     */
    public ClientResponse loadClasses(String jar, String classesToDelete) throws IOException, ProcCallException {
        return client.updateClasses(new File(jar), classesToDelete);
    }

    /**
     * <p>callProcedure.</p>
     *
     * @param proc   a {@link java.lang.String} object
     * @param params a {@link java.lang.Object} object
     * @return a {@link org.voltdb.client.ClientResponse} object
     * @throws java.io.IOException                 if any.
     * @throws org.voltdb.client.ProcCallException if any.
     */
    public ClientResponse callProcedure(String proc, Object... params) throws IOException, ProcCallException {
        return client.callProcedure(proc, params);
    }

    /**
     * <p>getConnectedClient.</p>
     *
     * @return a {@link org.voltdb.client.Client} object
     * @throws java.io.IOException if any.
     */
    public Client getConnectedClient() throws IOException {
        int mappedPort = getMappedPort(21211);
        ClientConfig config = new ClientConfig(username, password);
        if (tlsEnabled) {
            config.enableSSL();
            config.setTrustStore(trustStorePath, trustStorePassword);
        }
        long st = System.currentTimeMillis();
        while (System.currentTimeMillis() <  st + 60000) {
            client = ClientFactory.createClient(config);
            try {
                client.createConnection("localhost:" + mappedPort);
                ClientResponse response = client.callProcedure("@Ping");
                if (response.getStatus() == ClientResponse.SUCCESS) {
                    return client;
                }
                Thread.sleep(5000);
            } catch (IOException | InterruptedException | ProcCallException e) {
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException ex) {
                    //Ignore
                }
            }
        }
        throw new IOException("Could not connect to VoltDB, Server may have failed to start");
    }

    /**
     * <p>Getter for the field <code>hostId</code>.</p>
     *
     * @return a {@link java.lang.String} object
     */
    public String getHostId() {
        return hostId;
    }

    /**
     * Sets the path to the keystore file.
     *
     * @param keyStorePath the path to the keystore file
     */
    public void setKeyStorePath(String keyStorePath) {
        this.keyStorePath = keyStorePath;
    }

    /**
     * Sets the path to the truststore file.
     *
     * @param trustStorePath the path to the truststore file
     */
    public void setTrustStorePath(String trustStorePath) {
        this.trustStorePath = trustStorePath;
    }

    /**
     * Sets the flag to enable or disable TLS for the VoltDBContainer.
     *
     * @param tlsEnabled true to enable TLS, false to disable TLS
     */
    public void setTlsEnabled(boolean tlsEnabled) {
        this.tlsEnabled = tlsEnabled;
    }

    /**
     * Sets the username for the VoltDBContainer.
     *
     * @param username the username to set
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * Sets the password for the truststore file.
     *
     * @param trustStorePassword the password for the truststore file
     */
    public void setTrustStorePassword(String trustStorePassword) {
        this.trustStorePassword = trustStorePassword;
    }

    /**
     * Sets the password for the keystore file.
     *
     * @param keyStorePassword the password for the keystore file
     */
    public void setKeyStorePassword(String keyStorePassword) {
        this.keyStorePassword = keyStorePassword;
    }

    /**
     * Sets the password for the VoltDBContainer.
     *
     * @param password the password to set
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * Retrieves the client associated with this VoltDBContainer.
     * This client is created automatically when cluster is up, dont close this its used for internal healthcheck.
     *
     * @return The client instance.
     */
    public Client getClient() {
        return client;
    }

    // This is only used for join, rejoin cases when our extension uses it for verification and connecting.

    /**
     * <p>Setter for the field <code>client</code>.</p>
     *
     * @param client a {@link org.voltdb.client.Client} object
     */
    public void setClient(Client client) {
        this.client = client;
    }

    private String getDeployment() {
        return String.format(deploymentTemplate, hostcount, kfactor);
    }

    /**
     * Sets the kfactor for the VoltDBContainer and updates the deployment configuration.
     *
     * @param kfactor the kfactor value to set
     */
    protected void setKfactor(int kfactor) {
        this.kfactor = kfactor;
        String deployment = getDeployment();
        withCopyToContainer(Transferable.of(deployment), "/etc/deployment.xml");
    }
}
