package io.safebump.core.version;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VersionRangeTest {

    @Test
    void includesAndExcludesBoundaryVersions() {
        VersionRange range = VersionRange.between(
                version("1.2.0"), true, version("2.0.0"), false);

        assertTrue(range.contains(version("1.2.0")));
        assertTrue(range.contains(version("1.9.9")));
        assertFalse(range.contains(version("1.1.9")));
        assertFalse(range.contains(version("2.0.0")));
    }

    @Test
    void intersectsRangesAtTheirTightestBounds() {
        VersionRange first = VersionRange.between(
                version("1.0.0"), true, version("3.0.0"), false);
        VersionRange second = VersionRange.between(
                version("2.0.0"), false, version("4.0.0"), true);

        assertEquals(
                VersionRange.between(version("2.0.0"), false, version("3.0.0"), false),
                first.intersect(second).orElseThrow());
    }

    @Test
    void identifiesEmptyBoundaryIntersection() {
        VersionRange first = VersionRange.atMost(version("2.0.0"), false);
        VersionRange second = VersionRange.atLeast(version("2.0.0"), true);

        assertTrue(first.intersect(second).isEmpty());
        assertFalse(first.overlaps(second));
    }

    @Test
    void retainsASharedInclusiveExactVersion() {
        VersionRange first = VersionRange.atMost(version("2.0.0"), true);
        VersionRange second = VersionRange.atLeast(version("2.0.0"), true);

        assertEquals(
                VersionRange.exact(version("2.0.0")),
                first.intersect(second).orElseThrow());
    }

    private static SemanticVersion version(String value) {
        return SemanticVersion.parse(value);
    }
}
