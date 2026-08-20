package io.safebump.core.conflict;

import io.safebump.core.graph.DependencyGraph;
import io.safebump.core.model.PackageVersion;
import io.safebump.core.version.SemanticVersion;
import io.safebump.core.version.VersionConflict;
import io.safebump.core.version.VersionRange;
import io.safebump.core.version.VersionRequirement;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConflictExplainerTest {

    private static final PackageVersion APP = version("app", "1.0.0");
    private static final PackageVersion PACKAGE_A = version("package-a", "2.0.0");
    private static final PackageVersion PACKAGE_B = version("package-b", "1.0.0");
    private static final PackageVersion PACKAGE_D = version("package-d", "3.1.0");

    @Test
    void tracesAndFormatsBothShortestOriginPaths() {
        DependencyGraph graph = new DependencyGraph();
        graph.addDependency(APP, PACKAGE_A);
        graph.addDependency(APP, PACKAGE_B);
        graph.addDependency(PACKAGE_B, PACKAGE_D);
        VersionConflict conflict = new VersionConflict(
                "core-lib",
                new VersionRequirement(PACKAGE_A, VersionRange.atLeast(
                        semanticVersion("5.0.0"), true)),
                new VersionRequirement(PACKAGE_D, VersionRange.atMost(
                        semanticVersion("5.0.0"), false)));

        ConflictExplanation explanation = new ConflictExplainer().explain(
                graph, APP, conflict);

        assertEquals(java.util.List.of(APP, PACKAGE_A), explanation.firstPath());
        assertEquals(
                java.util.List.of(APP, PACKAGE_B, PACKAGE_D),
                explanation.secondPath());
        assertTrue(explanation.format().contains("Conflict detected for core-lib"));
        assertTrue(explanation.format().contains("app@1.0.0 -> package-b@1.0.0"));
        assertTrue(explanation.format().contains("do not overlap"));
    }

    @Test
    void rejectsAConflictOriginOutsideTheDependencyGraph() {
        DependencyGraph graph = new DependencyGraph();
        graph.addDependency(APP, PACKAGE_A);
        VersionConflict conflict = new VersionConflict(
                "core-lib",
                new VersionRequirement(PACKAGE_A, VersionRange.exact(
                        semanticVersion("1.0.0"))),
                new VersionRequirement(PACKAGE_D, VersionRange.exact(
                        semanticVersion("2.0.0"))));

        assertThrows(
                IllegalArgumentException.class,
                () -> new ConflictExplainer().explain(graph, APP, conflict));
    }

    private static PackageVersion version(String name, String version) {
        return new PackageVersion(name, version);
    }

    private static SemanticVersion semanticVersion(String version) {
        return SemanticVersion.parse(version);
    }
}
