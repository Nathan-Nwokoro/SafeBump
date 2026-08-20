package io.safebump.cli;

import io.safebump.core.adapter.DependencySourceException;
import io.safebump.core.analysis.AnalysisIssue;
import io.safebump.core.analysis.ProjectAnalysis;
import io.safebump.core.analysis.ProjectAnalysisService;
import io.safebump.core.graph.DependencyGraph;
import io.safebump.core.model.DependencyKind;
import io.safebump.core.model.DependencySnapshot;
import io.safebump.core.model.PackageMetadata;
import io.safebump.core.model.PackageVersion;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.Map;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GraphCommandTest {

    @TempDir
    private Path projectDirectory;

    @Test
    void printsTheResolvedGraphSummary() {
        CommandResult result = execute(ignored -> new ProjectAnalysis(snapshot(false), List.of()));

        assertEquals(0, result.exitCode());
        assertTrue(result.standardOutput().contains("SafeBump dependency graph"));
        assertTrue(result.standardOutput().contains("Project: example_app@1.0.0"));
        assertTrue(result.standardOutput().contains("Packages: 2"));
        assertTrue(result.standardOutput().contains("Dependencies: 1"));
        assertTrue(result.standardOutput().contains("Cycles: none"));
        assertTrue(result.standardOutput().contains("Metadata: consistent"));
        assertEquals("", result.standardError());
    }

    @Test
    void printsAReadableCyclePath() {
        CommandResult result = execute(ignored -> new ProjectAnalysis(snapshot(true), List.of()));

        assertEquals(0, result.exitCode());
        assertTrue(result.standardOutput().contains(
                "Cycles: dependency@2.0.0 -> example_app@1.0.0 -> dependency@2.0.0"));
    }

    @Test
    void printsMetadataReconciliationIssues() {
        CommandResult result = execute(ignored -> new ProjectAnalysis(
                snapshot(false),
                List.of(new AnalysisIssue(
                        "VERSION_MISMATCH",
                        "dependency differs between graph and lockfile"))));

        assertEquals(0, result.exitCode());
        assertTrue(result.standardOutput().contains("Metadata: 1 issue(s)"));
        assertTrue(result.standardOutput().contains(
                "[VERSION_MISMATCH] dependency differs between graph and lockfile"));
    }

    @Test
    void returnsAUsefulExitCodeAndErrorForInvalidProjects() {
        CommandResult result = execute(ignored -> {
            throw new DependencySourceException("No pubspec.yaml found");
        });

        assertEquals(2, result.exitCode());
        assertEquals("", result.standardOutput());
        assertTrue(result.standardError().contains("Error: No pubspec.yaml found"));
    }

    private CommandResult execute(ProjectAnalysisService analysisService) {
        CommandLine commandLine = new CommandLine(new GraphCommand(analysisService));
        StringWriter standardOutput = new StringWriter();
        StringWriter standardError = new StringWriter();
        commandLine.setOut(new PrintWriter(standardOutput, true));
        commandLine.setErr(new PrintWriter(standardError, true));

        int exitCode = commandLine.execute(projectDirectory.toString());
        return new CommandResult(exitCode, standardOutput.toString(), standardError.toString());
    }

    private static DependencySnapshot snapshot(boolean includeCycle) {
        PackageVersion root = new PackageVersion("example_app", "1.0.0");
        PackageVersion dependency = new PackageVersion("dependency", "2.0.0");
        DependencyGraph graph = new DependencyGraph();
        graph.addDependency(root, dependency);
        if (includeCycle) {
            graph.addDependency(dependency, root);
        }

        return new DependencySnapshot(
                root,
                graph,
                Map.of(
                        root.name(), new PackageMetadata(root, DependencyKind.ROOT, "root"),
                        dependency.name(), new PackageMetadata(
                                dependency, DependencyKind.DIRECT, "hosted")));
    }

    private record CommandResult(
            int exitCode,
            String standardOutput,
            String standardError) {
    }
}
