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
 * <p>Mutable tags (ending in {@code --latest}) must return {@code true} so
 * that the container is forced to re-pull on every run; every other tag shape
 * must return {@code false} so that pinned tags keep the testcontainers
 * default (pull only when absent locally).
 *
 * <p>Placed as a failsafe integration test ({@code *IT.java}) because the
 * module's pom configures failsafe only. No Docker daemon interaction is
 * required for the assertions themselves.
 */
public class PullPolicyIT {

    @Test
    public void latestSuffixIsMutable() {
        assertTrue(VoltDBContainer.isMutableTag("voltdb/voltdb-enterprise-dev:never-released--latest"),
                "A tag ending in --latest must be classified as mutable");
        assertTrue(VoltDBContainer.isMutableTag("some-image:main--latest"),
                "Any tag ending in --latest must be classified as mutable regardless of branch prefix");
    }

    @Test
    public void pinnedTagIsNotMutable() {
        assertFalse(VoltDBContainer.isMutableTag("voltdb/voltdb-enterprise:14.3.3"),
                "A pinned version tag must not be classified as mutable");
        assertFalse(VoltDBContainer.isMutableTag("voltdb/voltdb-enterprise:master--debug-1234"),
                "A build-number tag must not be classified as mutable");
    }

    @Test
    public void bareLatestIsNotMutable() {
        // The classification is intentionally strict: only the "--latest"
        // suffix (double dash) qualifies, not the ambiguous single-dash
        // Docker convention ":latest".
        assertFalse(VoltDBContainer.isMutableTag("nginx:latest"),
                "A tag of :latest (single dash) must not be classified as mutable");
    }

    @Test
    public void nullImageIsNotMutable() {
        assertFalse(VoltDBContainer.isMutableTag(null),
                "A null image reference must not be classified as mutable");
    }
}