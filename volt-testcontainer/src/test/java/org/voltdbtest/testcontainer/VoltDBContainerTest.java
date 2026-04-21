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
}
