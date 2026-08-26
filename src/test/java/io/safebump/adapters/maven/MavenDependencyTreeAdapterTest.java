package io.safebump.adapters.maven;

import io.safebump.core.analysis.ProjectAnalysis;
import io.safebump.core.model.DependencyKind;
import io.safebump.core.model.PackageVersion;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MavenDependencyTreeAdapterTest {

    @Test
    void parsesMavenDependencyPluginJson() throws Exception {
        String json = Files.readString(Path.of(
                "src/test/resources/fixtures/maven/dependency-tree.json"));

        ProjectAnalysis analysis = new MavenDependencyTreeAdapter().parse(json);
        var snapshot = analysis.dependencySnapshot();
        PackageVersion root = new PackageVersion("com.example:demo-app", "1.0.0");
        PackageVersion library = new PackageVersion("org.example:library-a", "2.0.0");

        assertEquals("maven", analysis.ecosystem());
        assertEquals(root, snapshot.rootPackage());
        assertEquals(4, snapshot.graph().packageCount());
        assertEquals(3, snapshot.graph().dependencyCount());
        assertTrue(snapshot.graph().getDirectDependencies(root).contains(library));
        assertEquals(DependencyKind.DEV,
                snapshot.findPackage("org.junit.jupiter:junit-jupiter").orElseThrow().kind());
        assertEquals(DependencyKind.TRANSITIVE,
                snapshot.findPackage("org.shared:core").orElseThrow().kind());
    }
}
