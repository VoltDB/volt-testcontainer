/*
 * Copyright (C) 2025-2026 Volt Active Data Inc.
 *
 * Use of this source code is governed by an MIT
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/MIT.
 */
package org.voltdbtest.testcontainer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link VoltDBContainer} configuration behaviour.
 * These tests verify that the container's {@code configure()} method
 * does not override user-supplied settings (e.g. extra exposed ports)
 * and that the fluent "with" methods work correctly.
 */
@SuppressWarnings("resource")
public class VoltDBContainerTest {

    @TempDir
    Path tempDir;

    private VoltDBContainer createContainer() throws IOException {
        Path fakeLicense = tempDir.resolve("license.xml");
        Files.writeString(fakeLicense, "<license/>");

        return new VoltDBContainer(
                0,
                fakeLicense.toAbsolutePath().toString(),
                VoltDBContainer.DEV_IMAGE,
                1,
                0,
                "--ignore=thp --count=1",
                null
        );
    }

    @Test
    void addExposedPortIsNotOverriddenByConfigure() throws IOException {
        // Given
        VoltDBContainer container = createContainer();

        int customPort = 12345;
        container.addExposedPort(customPort);

        // When
        container.configure();

        List<Integer> exposedPorts = container.getExposedPorts();
        assertThat(exposedPorts)
                .as("Custom port added via addExposedPort must survive configure()")
                .contains(customPort);

        assertThat(exposedPorts)
                .contains(21212, VoltDBContainer.VOLTDB_CLIENT_PORT, 9092, 5555);
    }

    @Test
    void withHostCountReturnsSameInstance() throws IOException {
        // Given
        VoltDBContainer container = createContainer();

        // When
        VoltDBContainer result = container.withHostCount(3);
        result.configure();

        // Then
        assertThat(result).isSameAs(container);
    }

    @Test
    void withKfactorReturnsSameInstance() throws IOException {
        // Given
        VoltDBContainer container = createContainer();

        // When
        VoltDBContainer result = container.withKfactor(2);
        result.configure();

        // Then
        assertThat(result).isSameAs(container);
    }

    @Test
    void withCommandLogEnabledReturnsSameInstance() throws IOException {
        // Given
        VoltDBContainer container = createContainer();

        // When
        VoltDBContainer result = container.withCommandLogEnabled(true);

        // Then
        assertThat(result).isSameAs(container);
    }

    @Test
    void withNetworkTypeReturnsSameInstance() throws IOException {
        // Given
        VoltDBContainer container = createContainer();

        // When
        VoltDBContainer result = container.withNetworkType(VoltDBContainer.NetworkType.DOCKER);
        result.configure();

        // Then
        assertThat(result).isSameAs(container);
    }

    @Test
    void withTopicPublicInterfaceReturnsSameInstance() throws IOException {
        // Given
        VoltDBContainer container = createContainer();

        // When
        VoltDBContainer result = container.withTopicPublicInterface("my-host");
        result.configure();

        // Then
        assertThat(result).isSameAs(container);
    }

    @Test
    void withDrPublicInterfaceReturnsSameInstance() throws IOException {
        // Given
        VoltDBContainer container = createContainer();

        // When
        VoltDBContainer result = container.withDrPublicInterface("dr-host");
        result.configure();

        // Then
        assertThat(result).isSameAs(container);
    }

    @Test
    void withTlsEnabledReturnsSameInstance() throws IOException {
        // Given
        VoltDBContainer container = createContainer();

        // When
        VoltDBContainer result = container.withTlsEnabled(true);
        result.configure();

        // Then
        assertThat(result).isSameAs(container);
    }

    @Test
    void withUsernameReturnsSameInstance() throws IOException {
        // Given
        VoltDBContainer container = createContainer();

        // When
        VoltDBContainer result = container.withUsername("admin");
        result.configure();

        // Then
        assertThat(result).isSameAs(container);
    }

    @Test
    void withPasswordReturnsSameInstance() throws IOException {
        // Given
        VoltDBContainer container = createContainer();

        // When
        VoltDBContainer result = container.withPassword("secret");
        result.configure();

        // Then
        assertThat(result).isSameAs(container);
    }

    @Test
    void withDeploymentContentReturnsSameInstance() throws IOException {
        // Given
        VoltDBContainer container = createContainer();

        // When
        VoltDBContainer result = container.withDeploymentContent("<deployment/>");
        result.configure();

        // Then
        assertThat(result).isSameAs(container);
    }

    @Test
    void withDeploymentLoadsClasspathResource() throws IOException {
        // Given
        VoltDBContainer container = createContainer();

        // When
        VoltDBContainer result = container.withDeployment("custom-deployment.xml");
        result.configure();

        // Then
        assertThat(result).isSameAs(container);
    }

    @Test
    void withDeploymentRejectsMissingResource() throws IOException {
        // Given
        VoltDBContainer container = createContainer();

        // Then
        assertThatThrownBy(() -> container.withDeployment("does-not-exist.xml"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void withStartCommandReturnsSameInstance() throws IOException {
        // Given
        VoltDBContainer container = createContainer();

        // When
        VoltDBContainer result = container.withStartCommand("--count=2");
        result.configure();

        // Then
        assertThat(result).isSameAs(container);
    }

    @Test
    void withVerifyExistenceOfTheLicenseFile() throws IOException {
        // Given
        VoltDBContainer container = createContainer();

        // When
        VoltDBContainer result = container.withLicensePath("/some/path");

        // Then
        assertThatThrownBy(result::configure)
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void withExtraJarsDirReturnsSameInstance() throws IOException {
        // Given
        VoltDBContainer container = createContainer();

        // When
        VoltDBContainer result = container.withExtraJarsDir("/some/jars");
        result.configure();

        // Then
        assertThat(result).isSameAs(container);
    }

    @Test
    void withTrustStorePasswordReturnsSameInstance() throws IOException {
        // Given
        VoltDBContainer container = createContainer();

        // When
        VoltDBContainer result = container.withTrustStorePassword("pass");
        result.configure();

        // Then
        assertThat(result).isSameAs(container);
    }

    @Test
    void withKeyStorePasswordReturnsSameInstance() throws IOException {
        // Given
        VoltDBContainer container = createContainer();

        // When
        VoltDBContainer result = container.withKeyStorePassword("pass");
        result.configure();

        // Then
        assertThat(result).isSameAs(container);
    }

    @Test
    void withKeyStorePathReturnsSameInstance() throws IOException {
        // Given
        VoltDBContainer container = createContainer();

        // When
        VoltDBContainer result = container.withKeyStorePath("/ks/path");
        result.configure();

        // Then
        assertThat(result).isSameAs(container);
    }

    @Test
    void withTrustStorePathReturnsSameInstance() throws IOException {
        // Given
        VoltDBContainer container = createContainer();

        // When
        VoltDBContainer result = container.withTrustStorePath("/ts/path");
        result.configure();

        // Then
        assertThat(result).isSameAs(container);
    }

    @Test
    void fluentMethodChainingWorks() throws IOException {
        // Given
        Path fakeLicense = tempDir.resolve("license.xml");
        Files.writeString(fakeLicense, "<license/>");

        // When
        VoltDBContainer container = VoltDBContainer.withDevImage()
                .withLicensePath(fakeLicense.toAbsolutePath().toString())
                .withHostCount(3)
                .withKfactor(1)
                .withCommandLogEnabled(false)
                .withNetworkType(VoltDBContainer.NetworkType.DOCKER)
                .withTopicPublicInterface("topics-host")
                .withDrPublicInterface("dr-host")
                .withUsername("admin")
                .withPassword("secret")
                .withStartCommand("--count=3");

        container.configure();

        // Then
        assertThat(container.getExposedPorts())
                .contains(VoltDBContainer.VOLTDB_CLIENT_PORT);
    }

    @Test
    void devImageDisablesCommandLogByDefault() throws IOException {
        // Given
        Path fakeLicense = tempDir.resolve("license.xml");
        Files.writeString(fakeLicense, "<license/>");

        // When
        VoltDBContainer container = new VoltDBContainer(
                0,
                fakeLicense.toAbsolutePath().toString(),
                VoltDBContainer.DEV_IMAGE,
                1,
                0,
                "--ignore=thp --count=1",
                null
        );

        container.configure();

        // Then
        assertThat(container.getExposedPorts()).isNotEmpty();
    }

    @Test
    void staticFactoryWithDevImageCreatesValidContainer() throws IOException {
        // Given
        Path fakeLicense = tempDir.resolve("license.xml");
        Files.writeString(fakeLicense, "<license/>");

        // When
        VoltDBContainer container = VoltDBContainer.withDevImage()
                .withLicensePath(fakeLicense.toAbsolutePath().toString());

        container.configure();

        // Then
        assertThat(container.getExposedPorts())
                .contains(VoltDBContainer.VOLTDB_CLIENT_PORT, 21212, 9092, 5555);
    }

    @Test
    void staticFactoryWithImageCreatesValidContainer() throws IOException {
        // Given
        Path fakeLicense = tempDir.resolve("license.xml");
        Files.writeString(fakeLicense, "<license/>");

        // When
        VoltDBContainer container = VoltDBContainer.withImage(VoltDBContainer.DEV_IMAGE)
                .withLicensePath(fakeLicense.toAbsolutePath().toString());

        container.configure();

        // Then
        assertThat(container.getExposedPorts())
                .contains(VoltDBContainer.VOLTDB_CLIENT_PORT);
    }

    @Test
    void withJavaPropertyReturnsSameInstance() throws IOException {
        // Given
        VoltDBContainer container = createContainer();

        // When
        VoltDBContainer result = container.withJavaProperty("voltdb.test.flag", "true");

        // Then
        assertThat(result).isSameAs(container);
    }

    @Test
    void withJavaPropertyAppendsToVoltdbOpts() throws IOException {
        // Given
        VoltDBContainer container = createContainer()
                .withJavaProperty("voltdb.test.streamingSnapshot.failOnDemand", "true")
                .withJavaProperty("custom.flag", "42");

        // When
        container.configure();

        // Then — the env value should retain the base options and append both -D entries
        // in insertion order.
        String voltdbOpts = container.getEnvMap().get("VOLTDB_OPTS");
        assertThat(voltdbOpts)
                .as("base options must still be present")
                .contains("-Dlog4j.configuration=")
                .contains("--add-opens=java.base/java.net=ALL-UNNAMED")
                .contains("--add-opens=java.base/java.lang.reflect=ALL-UNNAMED");
        assertThat(voltdbOpts)
                .as("user-supplied properties must be appended")
                .contains(" -Dvoltdb.test.streamingSnapshot.failOnDemand=true")
                .contains(" -Dcustom.flag=42");
        assertThat(voltdbOpts.indexOf(" -Dvoltdb.test.streamingSnapshot.failOnDemand=true"))
                .as("insertion order must be preserved")
                .isLessThan(voltdbOpts.indexOf(" -Dcustom.flag=42"));
    }

    @Test
    void voltdbOptsHasNoExtraPropertiesByDefault() throws IOException {
        // Given
        VoltDBContainer container = createContainer();

        // When
        container.configure();

        // Then — no extra -D entries beyond the hardcoded baseline
        String voltdbOpts = container.getEnvMap().get("VOLTDB_OPTS");
        assertThat(voltdbOpts).isNotNull();
        assertThat(countSubstring(voltdbOpts, " -D"))
                .as("only the baseline log4j -D should appear when no user properties are set")
                .isZero();
        assertThat(voltdbOpts).startsWith("-Dlog4j.configuration=");
    }

    @Test
    void withJavaPropertyOverwritesPreviousValueForSameKey() throws IOException {
        // Given
        VoltDBContainer container = createContainer()
                .withJavaProperty("voltdb.test.flag", "first")
                .withJavaProperty("voltdb.test.flag", "second");

        // When
        container.configure();

        // Then
        String voltdbOpts = container.getEnvMap().get("VOLTDB_OPTS");
        assertThat(voltdbOpts).contains(" -Dvoltdb.test.flag=second");
        assertThat(voltdbOpts).doesNotContain("=first");
    }

    @Test
    void withJavaPropertyAllowsEmptyValue() throws IOException {
        // Given
        VoltDBContainer container = createContainer()
                .withJavaProperty("voltdb.test.flag", "");

        // When
        container.configure();

        // Then
        String voltdbOpts = container.getEnvMap().get("VOLTDB_OPTS");
        assertThat(voltdbOpts).contains(" -Dvoltdb.test.flag=");
    }

    @Test
    void withJavaPropertyRejectsNullKey() throws IOException {
        VoltDBContainer container = createContainer();
        assertThatThrownBy(() -> container.withJavaProperty(null, "v"))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void withJavaPropertyRejectsNullValue() throws IOException {
        VoltDBContainer container = createContainer();
        assertThatThrownBy(() -> container.withJavaProperty("k", null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void withJavaPropertyRejectsEmptyKey() throws IOException {
        VoltDBContainer container = createContainer();
        assertThatThrownBy(() -> container.withJavaProperty("", "v"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void withJavaPropertyRejectsWhitespaceInKey() throws IOException {
        VoltDBContainer container = createContainer();
        assertThatThrownBy(() -> container.withJavaProperty("bad key", "v"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void withJavaPropertyRejectsWhitespaceInValue() throws IOException {
        VoltDBContainer container = createContainer();
        assertThatThrownBy(() -> container.withJavaProperty("k", "bad value"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void clusterWithJavaPropertyFansOutToAllContainers() throws IOException {
        // Given
        Path fakeLicense = tempDir.resolve("license.xml");
        Files.writeString(fakeLicense, "<license/>");
        VoltDBCluster cluster = new VoltDBCluster(
                fakeLicense.toAbsolutePath().toString(),
                VoltDBContainer.DEV_IMAGE,
                3,
                1
        );

        // When
        VoltDBCluster result = cluster.withJavaProperty("voltdb.test.fanout", "yes");

        // Then
        assertThat(result).isSameAs(cluster);
        assertThat(cluster.containers()).hasSize(3);
        for (VoltDBContainer c : cluster.containers()) {
            c.configure();
            assertThat(c.getEnvMap().get("VOLTDB_OPTS"))
                    .as("each container in the cluster must receive the property")
                    .contains(" -Dvoltdb.test.fanout=yes");
        }
    }

    private static int countSubstring(String haystack, String needle) {
        int count = 0;
        int from = 0;
        while ((from = haystack.indexOf(needle, from)) != -1) {
            count++;
            from += needle.length();
        }
        return count;
    }
}
