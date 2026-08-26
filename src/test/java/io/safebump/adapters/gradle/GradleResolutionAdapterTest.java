package io.safebump.adapters.gradle;

import io.safebump.core.analysis.ProjectAnalysis;
import io.safebump.core.model.DependencyKind;
import io.safebump.core.model.PackageVersion;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GradleResolutionAdapterTest {

    @Test
    void parsesResolutionResultExport() throws Exception {
        String json = Files.readString(Path.of(
                "src/test/resources/fixtures/gradle/resolution-result.json"));

        ProjectAnalysis analysis = new GradleResolutionAdapter().parse(json);
        var snapshot = analysis.dependencySnapshot();
        PackageVersion root = new PackageVersion("com.example:demo", "1.0.0");
        PackageVersion library = new PackageVersion("org.example:library", "2.1.0");

        assertEquals("gradle", analysis.ecosystem());
        assertEquals(root, snapshot.rootPackage());
        assertEquals(3, snapshot.graph().packageCount());
        assertEquals(2, snapshot.graph().dependencyCount());
        assertTrue(snapshot.graph().getDirectDependencies(root).contains(library));
        assertEquals(DependencyKind.DIRECT,
                snapshot.findPackage("org.example:library").orElseThrow().kind());
        assertEquals(DependencyKind.TRANSITIVE,
                snapshot.findPackage("org.shared:core").orElseThrow().kind());
    }
}
