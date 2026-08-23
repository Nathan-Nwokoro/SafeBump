package io.safebump.core.version;

import io.safebump.core.model.PackageVersion;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VersionConflictDetectorTest {

    private final VersionConflictDetector detector = new VersionConflictDetector();

    @Test
    void findsPairwiseNonOverlappingRequirementsDeterministically() {
        VersionRequirement older = new VersionRequirement(
                packageVersion("package-d", "3.1.0"), range("1.0.0", "5.0.0"));
        VersionRequirement newer = new VersionRequirement(
                packageVersion("package-a", "2.0.0"),
                VersionRange.atLeast(version("5.0.0"), true));

        List<VersionConflict> conflicts = detector.findConflicts(
                "core-lib", List.of(older, newer));

        assertEquals(1, conflicts.size());
        assertEquals(
                packageVersion("package-a", "2.0.0"),
                conflicts.getFirst().first().origin());
        assertEquals(
                packageVersion("package-d", "3.1.0"),
                conflicts.getFirst().second().origin());
    }

    @Test
    void acceptsRequirementsWithACommonVersion() {
        VersionRequirement first = new VersionRequirement(
                packageVersion("package-a", "1.0.0"), range("1.0.0", "3.0.0"));
        VersionRequirement second = new VersionRequirement(
                packageVersion("package-b", "1.0.0"), range("2.0.0", "4.0.0"));

        assertTrue(detector.findConflicts("shared", List.of(first, second)).isEmpty());
    }

    private static VersionRange range(String lower, String upper) {
        return VersionRange.between(version(lower), true, version(upper), false);
    }

    private static SemanticVersion version(String value) {
        return SemanticVersion.parse(value);
    }

    private static PackageVersion packageVersion(String name, String version) {
        return new PackageVersion(name, version);
    }
}
