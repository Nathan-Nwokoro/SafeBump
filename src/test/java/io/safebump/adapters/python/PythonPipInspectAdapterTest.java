package io.safebump.adapters.python;

import io.safebump.core.analysis.ProjectAnalysis;
import io.safebump.core.model.DependencyKind;
import io.safebump.core.model.PackageVersion;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PythonPipInspectAdapterTest {

    @TempDir
    Path projectDirectory;

    @Test
    void parsesStablePipInspectReportAndRequirementNames() throws Exception {
        Files.writeString(projectDirectory.resolve("pyproject.toml"), """
                [project]
                name = "demo-app"
                version = "1.0.0"
                """);
        String json = Files.readString(Path.of(
                "src/test/resources/fixtures/python/pip-inspect.json"));

        ProjectAnalysis analysis = new PythonPipInspectAdapter().parse(projectDirectory, json);
        var snapshot = analysis.dependencySnapshot();
        PackageVersion root = new PackageVersion("demo-app", "1.0.0");
        PackageVersion requests = new PackageVersion("requests", "2.32.4");

        assertEquals("python", analysis.ecosystem());
        assertEquals(root, snapshot.rootPackage());
        assertEquals(5, snapshot.graph().packageCount());
        assertEquals(4, snapshot.graph().dependencyCount());
        assertTrue(snapshot.graph().getDirectDependencies(root).contains(requests));
        assertEquals(DependencyKind.DIRECT,
                snapshot.findPackage("requests").orElseThrow().kind());
        assertEquals(DependencyKind.TRANSITIVE,
                snapshot.findPackage("urllib3").orElseThrow().kind());
        assertTrue(analysis.issues().isEmpty());
    }
}
