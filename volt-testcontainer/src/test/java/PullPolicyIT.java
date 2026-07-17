/*
 * Copyright (C) 2025-2026 Volt Active Data Inc.
 *
 * Use of this source code is governed by an MIT
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/MIT.
 */
import org.junit.jupiter.api.Test;
import org.voltdbtest.testcontainer.VoltDBContainer;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the mutable-tag classification that drives whether
 * {@link VoltDBContainer} applies an always-pull image pull policy at
 * construction time.
 *
 * <p>Mutable tags (rolling names whose registry digest can change without
 * the tag string changing) must return {@code true} so that the container
 * is forced to re-pull on every run; every other tag shape must return
 * {@code false} so that pinned tags keep the testcontainers default (pull
 * only when absent locally).
 *
 * <p>Placed as a failsafe integration test ({@code *IT.java}) because the
 * module's pom configures failsafe only. No Docker daemon interaction is
 * required for the assertions themselves.
 */
public class PullPolicyIT {

    @Test
    public void perBranchLatestSuffixIsMutable() {
        assertTrue(VoltDBContainer.isMutableTag("voltdb/voltdb-enterprise-dev:never-released--latest"),
                "A tag ending in --latest must be classified as mutable");
        assertTrue(VoltDBContainer.isMutableTag("some-image:main--latest"),
                "Any tag ending in --latest must be classified as mutable regardless of branch prefix");
    }

    @Test
    public void perBranchDebugSuffixIsMutable() {
        assertTrue(VoltDBContainer.isMutableTag("voltdb/voltdb-enterprise:master--debug"),
                "A per-branch --debug tag published by 1_pre_check_and_build must be mutable");
        assertTrue(VoltDBContainer.isMutableTag("voltdb/voltdb-enterprise:release-13.3.x--debug"),
                "Any branch prefix followed by --debug must be mutable");
    }

    @Test
    public void perBranchDevSuffixIsMutable() {
        assertTrue(VoltDBContainer.isMutableTag("voltdb/voltdb-vmc-svc-dev:master--dev"),
                "A per-branch --dev tag (VMC-svc / legacy) must be mutable");
    }

    @Test
    public void globalLatestIsMutable() {
        assertTrue(VoltDBContainer.isMutableTag("voltdb/voltdb-vmc-svc-dev:latest"),
                "Docker's global :latest tag must be mutable");
        assertTrue(VoltDBContainer.isMutableTag("nginx:latest"),
                "Any :latest tag must be mutable, including third-party images");
    }

    @Test
    public void pinnedTagIsNotMutable() {
        assertFalse(VoltDBContainer.isMutableTag("voltdb/voltdb-enterprise:15.3.0"),
                "A pinned version tag must not be classified as mutable");
        assertFalse(VoltDBContainer.isMutableTag("voltdb/voltdb-enterprise:master--142"),
                "A per-branch build-number tag must not be classified as mutable");
        assertFalse(VoltDBContainer.isMutableTag("voltdb/voltdb-enterprise:master--debug-142"),
                "A per-branch --debug-<build> tag is immutable and must not be classified as mutable");
    }

    @Test
    public void repoNameContainingMutableSubstringIsNotMutable() {
        // The classification is a suffix match on the whole image reference,
        // not a substring search. "voltdb-dev" in the repository name must
        // not be enough to mark a pinned tag as mutable.
        assertFalse(VoltDBContainer.isMutableTag("voltdb/voltdb-enterprise-dev:15.3.0"),
                "Repo names that contain -dev must not affect the classification");
        assertFalse(VoltDBContainer.isMutableTag("voltdb/voltdb-enterprise-dev:master--142"),
                "Repo names that contain -dev must not affect the classification");
    }

    @Test
    public void nullImageIsNotMutable() {
        assertFalse(VoltDBContainer.isMutableTag(null),
                "A null image reference must not be classified as mutable");
    }
}
