/*
 * This file is part of VoltSP.
 * Copyright (C) 2024-2025 Volt Active Data Inc. All rights reserved.
 */
package org.voltdbtest.testcontainer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * Helper class to locate VoltDB license files from standard locations.
 */
public class LicenseHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(LicenseHelper.class);

    /**
     * Searches for a VoltDB license file in standard locations and returns the path.
     * Checks in order: VOLTDB_LICENSE environment variable, user home directory, temp directory.
     *
     * @return the absolute path to the license file
     * @throws IllegalArgumentException if no license file is found in any standard location
     */
    public static String getLicenseFromStandardLocationOrFail() {
        String licenseFromEnv = System.getenv("VOLTDB_LICENSE");
        File licenseInUserHomeDir = new File(System.getProperty("user.home"), "license.xml");
        File licenseInTmpDir = new File(System.getProperty("java.io.tmpdir"), "license.xml");

        if (licenseFromEnv != null) {
            File licenseFile = new File(licenseFromEnv);
            if (licenseFile.exists()) {
                LOGGER.info("Using license file found through VOLTDB_LICENSE environment variable: {}", licenseFile.getAbsolutePath());
                return licenseFile.getAbsolutePath();
            }
        }

        if (licenseInUserHomeDir.exists()) {
            LOGGER.info("Using license file found in user home directory: {}", licenseInUserHomeDir.getAbsolutePath());
            return licenseInUserHomeDir.getAbsolutePath();
        }

        if (licenseInTmpDir.exists()) {
            LOGGER.info("Using license file found in user home directory: {}", licenseInTmpDir.getAbsolutePath());
            return licenseInTmpDir.getAbsolutePath();
        }

        throw new IllegalArgumentException(
                "Missing license file, the VoltDB container will fail to start without it. Searched:\n\t"
                + licenseInUserHomeDir + "\n\t"
                + licenseInTmpDir
        );
    }
}
