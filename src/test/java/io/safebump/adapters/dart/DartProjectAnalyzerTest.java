package io.safebump.adapters.dart;

import io.safebump.core.analysis.ProjectAnalysis;
import org.junit.jupiter.api.Test;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DartProjectAnalyzerTest {

    @Test
    void loadsAndReconcilesACompleteDartProject() throws Exception {
        Path projectDirectory = fixtureDirectory();
        String dependencyJson = Files.readString(projectDirectory.resolve("pub-deps.json"));
        DartProjectAdapter projectAdapter = new DartProjectAdapter(
                ignored -> dependencyJson,
                new DartPubDepsAdapter());
        DartProjectAnalyzer analyzer = new DartProjectAnalyzer(
                projectAdapter,
                new DartPubspecParser(),
                new DartLockfileParser(),
                new DartProjectReconciler(),
                new DartConstraintAnalyzer());

        ProjectAnalysis analysis = analyzer.analyse(projectDirectory);

        assertTrue(analysis.isConsistent());
        assertEquals("safebump_probe", analysis.dependencySnapshot().rootPackage().name());
        assertEquals(5, analysis.dependencySnapshot().graph().packageCount());
        assertEquals(4, analysis.dependencySnapshot().graph().dependencyCount());
    }

    private static Path fixtureDirectory() throws URISyntaxException {
        URL resource = Objects.requireNonNull(
                DartProjectAnalyzerTest.class.getResource(
                        "/fixtures/dart/project/consistent/pubspec.yaml"),
                "Missing consistent Dart project fixture");
        return Path.of(resource.toURI()).getParent();
    }
}
