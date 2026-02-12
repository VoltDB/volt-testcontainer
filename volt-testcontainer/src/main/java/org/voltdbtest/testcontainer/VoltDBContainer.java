/*
 * Copyright (C) 2024 Volt Active Data Inc.
 *
 * Use of this source code is governed by an MIT
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/MIT.
 */
package org.voltdbtest.testcontainer;

import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.ContainerNetwork;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;

/**
 * A local containerized cluster which takes host alias, docker image name
 * a valid license file, deployment file and voltdb start options.
 *
 * @author akhanzode
 * @version $Id: $Id
 */
@SuppressWarnings("resource")
public class VoltDBContainer extends GenericContainer<VoltDBContainer> {

    /** Default VoltDB developer edition image. */
    public static final String DEV_IMAGE = "voltactivedata/volt-developer-edition:14.1.0_voltdb";

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

    /** Network mode for the VoltDB container. */
    public enum NetworkType {
        /** Use host networking mode. */
        HOST,
        /** Use Docker bridge networking mode. */
        DOCKER
    }

    // This client is created automatically when cluster is up, dont close this its used for internal healthcheck.
    Client client;
    private final String hostId;
    private NetworkType networkType = NetworkType.HOST;
    private String topicPublicInterface;
    private String drPublicInterface;
    private boolean tlsEnabled = false;
    private String username = "";
    private String password = "";
    private String trustStorePassword = "";
    private String keyStorePassword = "";
    private String keyStorePath = "";
    private String trustStorePath = "";
    private int kfactor = 0;
    private int hostcount = 1;
    private String containerName = "";

    /**
     * Creates a VoltDB container using a public, free Developer Edition container.
     * <p>
     * Does not add any jars to its extension directory.
     * Assumes the license file is named "license.xml" and placed in the user home directory or system temp directory.
     *
     * @return a new VoltDB container instance
     */
    public static VoltDBContainer withDevImage() {
        return withImage(DEV_IMAGE, null);
    }

    /**
     * Creates a VoltDB container using a public, free Developer Edition container.
     * <p>
     * Assumes the license file is named "license.xml" and placed in the user home directory or system temp directory.
     *
     * @param extraJarsDir folder from where extra jars need to be added to the server extension directory.
     * @return a new VoltDB container instance
     */
    public static VoltDBContainer withDevImage(String extraJarsDir) {
        return withImage(DEV_IMAGE, extraJarsDir);
    }

    /**
     * Creates a VoltDB container using a specified container image.
     * <p>
     * Assumes the license file is named "license.xml" and placed in the user home directory or system temp directory.
     *
     * @param image        the image name of the Docker container
     * @param extraJarsDir folder from where extra jars need to be added to the server extension directory. Can be null.
     * @return a new VoltDB container instance
     */
    public static VoltDBContainer withImage(String image, String extraJarsDir) {
        return new VoltDBContainer(
                0,
                "",
                image,
                1,
                0,
                VoltDBCluster.getStartCommand(1),
                extraJarsDir
        );
    }

    /**
     * Creates a VoltDB container with the specified parameters.
     *
     * @param id           the ID of the container
     * @param image        the image name of the Docker container
     * @param licensePath  the path to the license file. If null, then the home directory and system temp directory will be searched for the "license.xml" file.
     * @param hostCount    the number of hosts in the cluster
     * @param kfactor      kfactor of voltdb cluster.
     * @param startCommand the start command for the VoltDB container
     * @param extraJarsDir folder from where extra jars need to be added to the server extension directory. Can be null.
     */
    public VoltDBContainer(int id, String licensePath, String image, int hostCount, int kfactor, String startCommand, String extraJarsDir) {
        this(id, licensePath, image, hostCount, kfactor, null, startCommand, extraJarsDir);
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
     * @param extraJarsDir folder from where extra jars need to be added to the server extension directory. Can be null.
     */
    public VoltDBContainer(int id, String licensePath, String image, int hostcount, int kfactor, String deployment, String startCommand, String extraJarsDir) {
        super(DockerImageName.parse(image));
        this.hostcount = hostcount;
        this.kfactor = kfactor;
        this.hostId = "host-" + id;

        if (deployment == null) {
            deployment = getDeployment();
        }

        GenericContainer container = withEnv("VOLTDB_START_CONFIG", startCommand);
        withEnv("VOLTDB_CONFIG", "/etc/deployment.xml");
        withEnv("VOLTDB_OPTS",
                "-Dlog4j.configuration=file:///opt/voltdb/tools/kubernetes/console-log4j.xml "
                + " --add-opens=java.base/java.net=ALL-UNNAMED"
                + " --add-opens=java.base/java.lang.reflect=ALL-UNNAMED");

        withNetworkMode(NETWORK.getId());
        withNetwork(NETWORK);
        withNetworkAliases(hostId);
        topicPublicInterface = hostId;
        drPublicInterface = hostId;

        handleLicenseSetup(licensePath);

        withCopyToContainer(Transferable.of(deployment), "/etc/deployment.xml");
        withExposedPorts(21212, 21211, 9092, 5555);
        withCreateContainerCmdModifier(cmd -> cmd.withHostName(hostId));
        withReuse(true);

        setWaitStrategy(Wait.forSuccessfulCommand("echo")
                .withStartupTimeout(Duration.ofSeconds(120L)));

        if (extraJarsDir != null) {
            File[] jars = getJars(extraJarsDir);
            for (File jar : jars) {
                String name = jar.getName();
                container.withCopyToContainer(
                        MountableFile.forHostPath(jar.getAbsolutePath()),
                        "/opt/voltdb/lib/extension/" + name
                );
            }
        }
    }

    private void handleLicenseSetup(String licensePath) {
        if (licensePath == null || licensePath.isEmpty()) {
            licensePath = LicenseHelper.getLicenseFromStandardLocationOrFail();
        } else {
            if (!Files.exists(Path.of(licensePath))) {
                throw new IllegalArgumentException(
                        "The provided license file does not exist: " + licensePath
                );
            }
        }
        withCopyToContainer(MountableFile.forHostPath(licensePath), "/etc/voltdb-license.xml");
    }

    private File[] getJars(String path) {
        File targetDir = new File(path);
        FileFilter jarFiles = pathname -> {
            if (pathname.isDirectory()) {
                return false;
            }
            String name = pathname.getName();
            return name.endsWith(".jar");
        };

        File[] result = targetDir.listFiles(jarFiles);
        if (result == null) {
            return new File[0];
        }

        return result;
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
        containerName = containerInfo.getName().replace("/", "");
        System.out.println("Container is starting: " + containerName);

        String topicSetting = getPublicInterfaceSetting(containerInfo, topicPublicInterface, 9092);
        String drSetting = getPublicInterfaceSetting(containerInfo, drPublicInterface, 5555);

        // It's twice here because the script has an echo.
        String finalScript = String.format(startScript, topicSetting, drSetting);
        copyFileToContainer(Transferable.of(finalScript, 511), "/opt/voltdb/tools/entrypoint.sh");
    }

    private String getPublicInterfaceSetting(InspectContainerResponse containerInfo, String maybePublicInterface, int port) {
        int actualPort;
        String publicInterface = maybePublicInterface;

        if (networkType == NetworkType.HOST) {
            actualPort = getMappedPort(port);
            publicInterface = "localhost";
        } else {
            actualPort = port;
            if (maybePublicInterface == null) {
                Map<String, ContainerNetwork> networks = containerInfo.getNetworkSettings().getNetworks();
                publicInterface = networks.values().stream()
                        .map(ContainerNetwork::getAliases)
                        .filter(Objects::nonNull)
                        .flatMap(Collection::stream)
                        .findFirst()
                        .orElse(containerInfo.getName());
            }
        }

        return publicInterface + ":" + actualPort;
    }

    @Override
    protected void containerIsStopping(InspectContainerResponse containerInfo) {
        super.containerIsStopping(containerInfo);
        System.out.println("Container is stopping: " + getContainerName());
    }

    @Override
    protected void containerIsStopped(InspectContainerResponse containerInfo) {
        super.containerIsStopped(containerInfo);
        System.out.println("Container is stopped: " + getContainerName());
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
     * Retrieves a connected client to the VoltDB instance with a default timeout of 120000 milliseconds.
     *
     * @return a {@link Client} object representing the connected client.
     * @throws IOException if an I/O error occurs while attempting to connect to the client.
     */
    public Client getConnectedClient() throws IOException {
        return getConnectedClient(120000);
    }

    /**
     * <p>getConnectedClient.</p>
     *
     * @param timeoutMillis time to wait for a client connection
     * @return a {@link org.voltdb.client.Client} object
     * @throws java.io.IOException if any.
     */
    public Client getConnectedClient(int timeoutMillis) throws IOException {
        int mappedPort = getMappedPort(21211);
        ClientConfig config = new ClientConfig(username, password);
        if (tlsEnabled) {
            config.enableSSL();
            if (keyStorePath == null || keyStorePath.isEmpty()) {
                config.setTrustStore(trustStorePath, trustStorePassword);
            } else {
                config.setTrustStoreWithMutualAuth(trustStorePath, trustStorePassword, keyStorePath, keyStorePassword);
            }
        }
        long st = System.currentTimeMillis();
        while (System.currentTimeMillis() < st + timeoutMillis) {
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

    public String getContainerName() {
        return containerName;
    }

    /**
     * Returns the network ID used by this container.
     *
     * @return the Docker network ID
     */
    public String getNetworkId() {
        return NETWORK.getId();
    }

    /**
     * Controls how public interfaces of the VoltDB server are configured. Both DR and Topics
     * protocols must be aware of the public interfaces used to communicate with the VoltDB instance.
     * If the network type is {@code NetworkType.DOCKER} then we assume all communication is within
     * a docker network and these interfaces advertise container ports (e.g., 9092 for topics).
     * If the network type is {@code NetworkType.HOST} then we assume all communication is from
     * the host machine and these interfaces advertise mapped (external) ports.
     * <p>
     * For {@code NetworkType.DOCKER} mode hostnames can be set separately using
     * #setTopicPublicInterface or #setDrPublicInterface. In case of {@code NetworkType.HOST} "localhost" is assumed.
     * In case of {@code NetworkType.DOCKER} we search for network aliases and settle for container id if none were found.
     * <p>
     * The default network type is HOST.
     *
     * @param networkType the network type to use
     */
    public void setNetworkType(NetworkType networkType) {
        this.networkType = networkType;
    }

    /**
     * Sets the public interface hostname for Kafka topics communication.
     *
     * @param topicPublicInterface the hostname to use for topics
     */
    public void setTopicPublicInterface(String topicPublicInterface) {
        this.topicPublicInterface = topicPublicInterface;
    }

    /**
     * Sets the public interface hostname for DR (Database Replication) communication.
     *
     * @param drPublicInterface the hostname to use for DR
     */
    public void setDrPublicInterface(String drPublicInterface) {
        this.drPublicInterface = drPublicInterface;
    }
}
