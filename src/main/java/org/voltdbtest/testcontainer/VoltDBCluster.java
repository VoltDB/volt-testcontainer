/*
 * Copyright (C) 2024 Volt Active Data Inc.
 *
 * Use of this source code is governed by an MIT
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/MIT.
 */
package org.voltdbtest.testcontainer;

import com.google.common.base.Joiner;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.Network;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.utility.MountableFile;
import org.voltdb.client.Client;
import org.voltdb.client.ClientResponse;
import org.voltdb.client.ProcCallException;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Represents a cluster of VoltDB instances.
 *
 * @author akhanzode
 * @version $Id: $Id
 */
public class VoltDBCluster {

    private final ExecutorService executorService = getSingleThreadExecutor("VoltDB Starter");
    /**
     * A container class that holds VoltDB containers in a map.
     * The map is used to store VoltDBContainer objects with their corresponding names as keys.
     */
    protected final Map<String, VoltDBContainer> containers = new HashMap<>();
    /**
     * The images variable is a protected final Map that stores image names for the VoltDB instances.
     * The keys are strings representing the image names, and the values are also strings representing the image paths.
     * The images map is used within the VoltDBCluster class to specify the image name for the VoltDB instances.
     * <p>
     * Example usage:
     * VoltDBCluster cluster = new VoltDBCluster("volt-image");
     * cluster.images.put("volt-image", "/path/to/volt-image");
     * cluster.start();
     * <p>
     * This variable is only accessible within the class and its subclasses.
     */
    protected final Map<String, String> images = new HashMap<>();
    /**
     * This variable represents a map of stopped nodes in the VoltDB cluster.
     * Each entry in the map consists of a node name as the key and its corresponding status as the value.
     * The map is initialized as an empty HashMap and is marked as final to prevent reassignment.
     */
    protected final Map<String, String> stoppedNodes = new HashMap<>();
    /**
     * Represents the count of hosts in a VoltDB cluster.
     * This variable is used to specify the number of hosts in the cluster when creating a new instance of the VoltDBCluster class.
     * The hostCount value determines the number of VoltDB containers that will be started in the cluster.
     * It can be accessed and modified by the subclasses of the VoltDBCluster class.
     */
    protected int hostCount;
    /**
     * The kfactor value of the VoltDB cluster.
     * <p>
     * The kfactor value determines the level of fault tolerance in the cluster. It represents the number of replicas
     * for each partition of data in the cluster. A kfactor value of 0 means no replicas, while a kfactor value of 1
     * means each partition has one replica.
     */
    protected int kfactor;
    /**
     * The licensePath variable represents the path of the license file used by the VoltDBCluster class.
     *
     * <p>
     * The licensePath is a protected String variable that is used within the VoltDBCluster class to specify the path of the license file.
     * It is used during the initialization and configuration of the VoltDB cluster.
     * <p>
     * For example, the licensePath variable can be set to a file path like "/path/to/license.txt" to specify the location of the license file.
     * </p>
     *
     * <p>
     * Example usage:
     * </p>
     * <pre>{@code
     * VoltDBCluster cluster = new VoltDBCluster();
     * cluster.setLicensePath("/path/to/license.txt");
     * }</pre>
     *
     * @see VoltDBCluster
     */
    protected String licensePath;

    /**
     * Represents a VoltDB cluster with a single host for testing purposes.
     */
    public VoltDBCluster(String licensePath) {
        this(licensePath, "voltdb/voltdb-enterprise-dev", 1, 0);
    }

    /**
     * Represents a VoltDB cluster with a single host for testing purposes.
     *
     * @param image the image name of the VoltDB instance to use
     */
    public VoltDBCluster(String licensePath, String image) {
        this(licensePath, image, null);
    }

    /**
     * Creates a VoltDBCluster with the specified number of hosts, kfactor value, and image name.
     *
     * @param image     the image name of the VoltDB instance to use
     * @param extraLibs Folder from where extra jars needs to be added to server extension directory
     * @throws java.lang.RuntimeException if the license file is not found or the VOLTDB_LICENSE environment variable is not set correctly
     */
    public VoltDBCluster(String licensePath, String image, String extraLibs) {
        this(licensePath, image, 1, 0, extraLibs);
    }

    /**
     * Creates a VoltDBCluster with the specified number of hosts, kfactor value, and image name.
     *
     * @param hostCount the number of hosts in the cluster
     * @param image     the image name of the VoltDB instance to use
     * @param kfactor   kfactor of voltdb cluster.
     * @throws java.lang.RuntimeException if the license file is not found or the VOLTDB_LICENSE environment variable is not set correctly
     */
    public VoltDBCluster(String licensePath, String image, int hostCount, int kfactor) {
        this(licensePath, image, hostCount, kfactor, null);
    }

    /**
     * Creates a VoltDBCluster with the specified number of hosts, kfactor value, and image name.
     * The license file is picked by
     * - env variable VOLTDB_LICENSE pointing to a license file
     * - or $HOME/license.xml
     * - or /tmp/voltdb-license.xml
     *
     * @param image         the image name of the VoltDB instance to use
     * @param hostCount     the number of hosts in the cluster
     * @param kfactor       kfactor of voltdb cluster.
     * @param extraLibs     Folder from where extra jars needs to be added to server extension directory
     * @throws java.lang.RuntimeException if the license file is not found or the VOLTDB_LICENSE environment variable is not set correctly
     */
    public VoltDBCluster(String licensePath, String image, int hostCount, int kfactor, String extraLibs) {
        this.licensePath = licensePath;
        this.hostCount = hostCount;
        this.kfactor = kfactor;
        String startCommand = getStartCommand(hostCount);
        for (int i = 0; i < hostCount; i++) {
            String host = String.format("%s-%d", "host", i);
            VoltDBContainer container = new VoltDBContainer(i, image, licensePath, hostCount, kfactor, startCommand, extraLibs);
            container.setKfactor(kfactor);
            containers.put(host, container);
            images.put(host, image);
        }
    }


    private static ExecutorService getSingleThreadExecutor(String name) {
        return new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue());
    }

    /**
     * Starts the VoltDB cluster by starting each VoltDB container in the cluster.
     *
     * @throws java.io.IOException        if an I/O error occurs while starting the VoltDB containers
     * @throws java.lang.RuntimeException if an error occurs during the starting process
     */
    public void start() throws IOException {
        List<Future> starters = new ArrayList<>();
        for (VoltDBContainer voltDBContainer : containers.values()) {
            starters.add(executorService.submit(new Runnable() {
                @Override
                public void run() {
                    voltDBContainer.start();
                }
            }));
        }
        for (Future voltDB : starters) {
            try {
                voltDB.get();
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        }
        for (VoltDBContainer voltDBContainer : containers.values()) {
            voltDBContainer.getConnectedClient();
        }
    }

    /**
     * <p>start.</p>
     *
     * @param voltDBContainer a {@link org.voltdbtest.testcontainer.VoltDBContainer} object
     * @param expectClient    a boolean
     * @throws java.io.IOException if any.
     */
    protected void start(VoltDBContainer voltDBContainer, boolean expectClient) throws IOException {
        Future f = executorService.submit(new Runnable() {
            @Override
            public void run() {
                voltDBContainer.start();
            }
        });
        try {
            f.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
        if (expectClient) {
            voltDBContainer.getConnectedClient();
        }
    }

    /**
     * Executes the given DDL file on the VoltDB cluster.
     *
     * @param ddl the DDL file to execute
     * @return true if the DDL execution is successful, false otherwise
     * @throws java.io.IOException                 if an I/O error occurs while reading the DDL file
     * @throws org.voltdb.client.ProcCallException if an error occurs during the DDL execution process
     */
    public boolean runDDL(File ddl) throws IOException, ProcCallException {
        String[] args = {"--file=" + ddl.getAbsolutePath()};
        Client client = getClient();
        SQLLoader sqlcmd = new SQLLoader(client);
        int exitCode = sqlcmd.execute(args);
        return exitCode == 0;
    }

    /**
     * Executes the given DDL schema on the VoltDB cluster.
     *
     * @param schema the DDL schema to execute
     * @return a {@link org.voltdb.client.ClientResponse} object
     * @throws java.io.IOException                 if an I/O error occurs while executing the DDL schema
     * @throws org.voltdb.client.ProcCallException if an error occurs during the DDL execution process
     */
    public ClientResponse runDDL(String schema) throws IOException, ProcCallException {
        for (VoltDBContainer voltDBContainer : containers.values()) {
            if (voltDBContainer.isRunning()) {
                return voltDBContainer.runDDL(schema);
            }
        }
        throw new RuntimeException("No running VoltDB containers found");
    }

    /**
     * Loads classes from a JAR file into the VoltDB cluster.
     *
     * @param jar the path to the JAR file to be loaded
     * @return a ClientResponse object representing the result of the operation
     * @throws java.io.IOException                 if an I/O error occurs while reading the JAR file
     * @throws org.voltdb.client.ProcCallException if an error occurs during the class loading process
     */
    public ClientResponse loadClasses(String jar) throws IOException, ProcCallException {
        for (VoltDBContainer voltDBContainer : containers.values()) {
            if (voltDBContainer.isRunning()) {
                return voltDBContainer.loadClasses(jar);
            }
        }
        return null;
    }

    /**
     * Loads classes from a JAR file into the VoltDB cluster.
     *
     * @param jar             the path to the JAR file to be loaded
     * @param classesToDelete classes to delete from the cluster
     * @return a ClientResponse object representing the result of the operation
     * @throws java.io.IOException                 if an I/O error occurs while reading the JAR file
     * @throws org.voltdb.client.ProcCallException if an error occurs during the class loading process
     */
    public ClientResponse loadClasses(String jar, String classesToDelete) throws IOException, ProcCallException {
        for (VoltDBContainer voltDBContainer : containers.values()) {
            if (voltDBContainer.isRunning()) {
                return voltDBContainer.loadClasses(jar, classesToDelete);
            }
        }
        return null;
    }

    /**
     * Calls a stored procedure with the given name and parameters.
     *
     * @param proc   the name of the stored procedure to call
     * @param params the parameters to pass to the stored procedure
     * @return a ClientResponse object representing the result of the procedure call
     * @throws java.io.IOException                 if an I/O error occurs while making the procedure call
     * @throws org.voltdb.client.ProcCallException if an error occurs during the procedure call process
     */
    public ClientResponse callProcedure(String proc, Object... params) throws IOException, ProcCallException {
        Client client = getClient();
        return client.callProcedure(proc, params);
    }

    /**
     * <p>getFirstMappedPort.</p>
     *
     * @return a int
     */
    public int getFirstMappedPort() {
        return containers.values().stream().findFirst().get().getFirstMappedPort();
    }

    /**
     * <p>getMappedPort.</p>
     *
     * @param port a int
     * @return a int
     */
    public int getMappedPort(int port) {
        return containers.values().stream().findFirst().get().getMappedPort(port);
    }

    /**
     * <p>getHost.</p>
     *
     * @return a {@link java.lang.String} object
     */
    public String getHost() {
        return containers.values().stream().findFirst().get().getHost();
    }

    /**
     * <p>getHostAndPort.</p>
     *
     * @return a {@link java.lang.String} object
     */
    public String getHostAndPort() {
        return String.format("%s:%d", getHost(), getMappedPort(21211));
    }

    /**
     * Retrieves a VoltDB client from the cluster instance.
     *
     * @return The VoltDB client associated with the running instance, or null if no running instance is found.
     * @throws java.io.IOException if an I/O error occurs while retrieving the client.
     */
    public Client getClient() throws IOException {
        for (VoltDBContainer voltDBContainer : containers.values()) {
            if (voltDBContainer.isRunning()) {
                return voltDBContainer.getConnectedClient();
            }
        }
        return null;
    }

    /**
     * Retrieves a VoltDB client from the cluster instance with the specified host ID.
     *
     * @param host The host ID of the cluster instance from which to retrieve the client.
     * @return The VoltDB client associated with the specified host, or null if no running instance is found.
     * @throws java.io.IOException if an I/O error occurs while retrieving the client.
     */
    public Client getClient(String host) throws IOException {
        for (VoltDBContainer voltDBContainer : containers.values()) {
            if (voltDBContainer.isRunning() && voltDBContainer.getHostId().equals(host)) {
                return voltDBContainer.getConnectedClient();
            }
        }
        return null;
    }

    private static String userHome() {
        String home = System.getProperty("user.home");
        if (home == null || home.isEmpty() || home.equals("?")) {
            home = System.getenv("HOME");
            if (home == null || home.isEmpty()) {
                home = "/etc";
            }
        }
        return home;
    }

    private String[] getHostList(int hostcount) {
        List<String> hosts = new ArrayList<>();
        for (int i = 0; i < hostcount; i++) {
            hosts.add("host-" + i);
        }
        return hosts.toArray(new String[0]);
    }

    // Used by our extension which does add (elastic) tests

    /**
     * <p>getJoinCommand.</p>
     *
     * @return a {@link java.lang.String} object
     */
    protected String getJoinCommand() {
        String startAction = "--ignore=thp --count=" + (hostCount + kfactor + 1) + " --host=" +
                containers.keySet().stream().findFirst().get() + " --add";
        return startAction;
    }

    /**
     * <p>getStartCommand.</p>
     *
     * @param hostcount a int
     * @return a {@link java.lang.String} object
     */
    protected String getStartCommand(int hostcount) {
        return "--ignore=thp --count=" + hostcount + " --host=" + Joiner.on(',').join(getHostList(hostcount));
    }

    // Used by our extension which does stop and require access to container for many other purposes.

    /**
     * <p>getContainer.</p>
     *
     * @param container a {@link java.lang.String} object
     * @return a {@link org.voltdbtest.testcontainer.VoltDBContainer} object
     */
    protected VoltDBContainer getContainer(String container) {
        return containers.get(container);
    }

    /**
     * Shuts down all the VoltDB instances in the cluster.
     * Waits for each instance to stop before proceeding to the next one.
     * This method blocks until all instances have been shutdown or the timeout is reached.
     */
    public void shutdown() {
        for (VoltDBContainer voltDBContainer : containers.values()) {
            voltDBContainer.stop();
            long st = System.currentTimeMillis();
            while (st < System.currentTimeMillis() + 30000) {
                try {
                    if (!voltDBContainer.isRunning()) {
                        break;
                    }
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException ex) {
                        //Ignore
                    }
                }
            }
        }
        System.out.println("Done Shutting down VoltDB");
    }

    /**
     * Sets the truststore for the VoltDB cluster.
     *
     * @param resourcePath the path to the truststore file
     * @param password     the password for the truststore
     * @return the updated VoltDBCluster object
     */
    public VoltDBCluster withTruststore(String resourcePath, String password) {
        String certificateTxt = "trustStore=" + "/etc/ssl/truststore.jks" + "\n" +
                "trustStorePassword=" + password + "\n" +
                "external=true";

        for (VoltDBContainer voltDBContainer : containers.values()) {
            voltDBContainer.withCopyToContainer(Transferable.of(password, 511), "/etc/ssl/truststore.pswd");
            voltDBContainer.withCopyToContainer(Transferable.of(certificateTxt, 511), "/etc/ssl/certificate.txt");
            voltDBContainer.withClasspathResourceMapping(resourcePath, "/etc/ssl/truststore.jks", BindMode.READ_ONLY);
            URL res = getClass().getClassLoader().getResource(resourcePath);
            String trustStorePath = (new File(res.getFile())).getAbsolutePath();
            voltDBContainer.setTrustStorePath(trustStorePath);
            voltDBContainer.setTlsEnabled(true);
            voltDBContainer.setTrustStorePassword(password);
        }
        return this;
    }

    /**
     * Sets the keystore for the VoltDB cluster.
     *
     * @param resourcePath the path to the keystore file
     * @param password     the password for the keystore
     * @return the updated VoltDBCluster object
     */
    public VoltDBCluster withKeystore(String resourcePath, String password) {
        for (VoltDBContainer voltDBContainer : containers.values()) {
            voltDBContainer.withClasspathResourceMapping(resourcePath, "/etc/ssl/keystore.jks",
                    BindMode.READ_ONLY);
            voltDBContainer.withCopyToContainer(Transferable.of(password, 511), "/etc/ssl/keystore.pswd");
            voltDBContainer.setTlsEnabled(true);
            URL res = getClass().getClassLoader().getResource(resourcePath);
            String keyStorePath = (new File(res.getFile())).getAbsolutePath();
            voltDBContainer.setKeyStorePath(keyStorePath);
            voltDBContainer.setKeyStorePassword(password);
        }
        return this;
    }

    /**
     * Sets the username and password for all VoltDB containers in the cluster.
     *
     * @param username the username for authentication
     * @param password the password for authentication
     * @return the updated VoltDBCluster object
     */
    public VoltDBCluster withUserNameAndPassword(String username, String password) {
        for (VoltDBContainer voltDBContainer : containers.values()) {
            voltDBContainer.setUsername(username);
            voltDBContainer.setPassword(password);
        }
        return this;
    }

    /**
     * Sets the deployment resource for all VoltDB containers in the cluster.
     *
     * @param fileName the file name of the deployment resource
     * @return the updated VoltDBCluster object
     */
    public VoltDBCluster withDeploymentResource(String fileName) {
        for (VoltDBContainer voltDBContainer : containers.values()) {
            voltDBContainer.withCopyToContainer(MountableFile.forClasspathResource(fileName), "/etc/deployment.xml");
        }
        return this;
    }

    /**
     * Sets the deployment content for all VoltDB containers in the cluster.
     *
     * @param deploymentContent the deployment content to set for the containers
     * @return the updated VoltDBCluster object
     */
    public VoltDBCluster withDeploymentContent(String deploymentContent) {
        for (VoltDBContainer voltDBContainer : containers.values()) {
            voltDBContainer.withCopyToContainer(Transferable.of(deploymentContent), "/etc/deployment.xml");
        }
        return this;
    }

    /**
     * Sets the initial schema for all VoltDB containers in the cluster by executing the given DDL schema file.
     *
     * @param resourcePath the path to the folder containing the DDL schema file
     * @param fileName     the name of the DDL schema file
     * @return the updated VoltDBCluster object
     */
    public VoltDBCluster withInitialSchema(String resourcePath, String fileName) {
        for (VoltDBContainer voltDBContainer : containers.values()) {
            voltDBContainer.withClasspathResourceMapping(resourcePath, "/etc/schemas/" + fileName, BindMode.READ_ONLY);
        }
        return this;
    }

    /**
     * Configures the initial schema for the VoltDB cluster by mapping a schema file to each container in the cluster.
     *
     * @param fileName the name of the schema file to be mapped
     * @return the updated VoltDBCluster object
     */
    public VoltDBCluster withInitialSchema(String fileName) {
        for (VoltDBContainer voltDBContainer : containers.values()) {
            voltDBContainer.withClasspathResourceMapping(fileName, "/etc/schemas/" + fileName, BindMode.READ_ONLY);
        }
        return this;
    }

    /**
     * Sets the initial classes for all VoltDB containers in the cluster.
     *
     * @param jar  The path to the JAR file containing the classes.
     * @param name The name of the classes.
     * @return The updated VoltDBCluster instance.
     */
    public VoltDBCluster withInitialClasses(String jar, String name) {
        for (VoltDBContainer voltDBContainer : containers.values()) {
            voltDBContainer.withCopyToContainer(MountableFile.forHostPath(jar), "/etc/classes/" + name);
        }
        return this;
    }

    /**
     * Adds the specified JAR files to the container's initial classes directory.
     *
     * @param jars the JAR files to copy to the container
     * @return the updated VoltDBCluster instance
     */
    public VoltDBCluster withInitialClasses(File[] jars) {
        for (VoltDBContainer voltDBContainer : containers.values()) {
            for (File file : jars) {
                for (File jar : jars) {
                    String name = jar.getName();
                    voltDBContainer.withCopyToContainer(MountableFile.forHostPath(jar.getAbsolutePath()), "/etc/classes/" + name);
                }
            }
        }
        return this;
    }

    /**
     * <p>withKsaftey.</p>
     *
     * @param kfactor a int
     * @return a {@link org.voltdbtest.testcontainer.VoltDBCluster} object
     */
    public VoltDBCluster withKsaftey(int kfactor) {
        for (VoltDBContainer voltDBContainer : containers.values()) {
            voltDBContainer.setKfactor(kfactor);
        }
        return this;
    }

    /**
     * Sets the network for all containers in the VoltDB cluster.
     *
     * @param network The network to be set for the containers.
     * @return This VoltDBCluster instance with the specified network set for all containers.
     */
    public VoltDBCluster withNetwork(Network network) {
        for (VoltDBContainer voltDBContainer : containers.values()) {
            voltDBContainer.setNetwork(network);
            voltDBContainer.setNetworkMode(network.getId());
        }
        return this;
    }
}
