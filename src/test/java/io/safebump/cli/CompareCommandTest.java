package io.safebump.cli;

import io.safebump.core.adapter.DependencySourceException;
import io.safebump.core.analysis.ProjectAnalysis;
import io.safebump.core.graph.DependencyGraph;
import io.safebump.core.model.DependencyKind;
import io.safebump.core.model.DependencySnapshot;
import io.safebump.core.model.PackageMetadata;
import io.safebump.core.model.PackageVersion;
import io.safebump.core.upgrade.DependencyGraphDiffer;
import io.safebump.core.upgrade.UpgradeAnalysis;
import io.safebump.core.upgrade.UpgradeAnalysisService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompareCommandTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void printsUpgradeImpactAndDetailedChanges() {
        ProjectAnalysis before = analysis("1.0.0", "1.0.0");
        ProjectAnalysis after = analysis("1.0.0", "2.0.0");
        UpgradeAnalysis upgradeAnalysis = new UpgradeAnalysis(
                before,
                after,
                new DependencyGraphDiffer().compare(
                        before.dependencySnapshot(), after.dependencySnapshot()));

        CommandResult result = execute((ignoredBefore, ignoredAfter) -> upgradeAnalysis);

        assertEquals(0, result.exitCode());
        assertTrue(result.standardOutput().contains("SafeBump upgrade impact"));
        assertTrue(result.standardOutput().contains("Packages changed: 1"));
        assertTrue(result.standardOutput().contains("Upgraded: 1"));
        assertTrue(result.standardOutput().contains(
                "[UPGRADED] dependency 1.0.0 -> 2.0.0 (transitive)"));
        assertEquals("", result.standardError());
    }

    @Test
    void printsSourceErrorsAndReturnsInputExitCode() {
        CommandResult result = execute((ignoredBefore, ignoredAfter) -> {
            throw new DependencySourceException("No pubspec.lock found");
        });

        assertEquals(2, result.exitCode());
        assertEquals("", result.standardOutput());
        assertTrue(result.standardError().contains("Error: No pubspec.lock found"));
    }

    private CommandResult execute(UpgradeAnalysisService service) {
        CommandLine commandLine = new CommandLine(new CompareCommand(service));
        StringWriter standardOutput = new StringWriter();
        StringWriter standardError = new StringWriter();
        commandLine.setOut(new PrintWriter(standardOutput, true));
        commandLine.setErr(new PrintWriter(standardError, true));
        int exitCode = commandLine.execute(
                temporaryDirectory.resolve("before").toString(),
                temporaryDirectory.resolve("after").toString());
        return new CommandResult(exitCode, standardOutput.toString(), standardError.toString());
    }

    private static ProjectAnalysis analysis(String rootVersion, String dependencyVersion) {
        PackageVersion root = new PackageVersion("app", rootVersion);
        PackageVersion dependency = new PackageVersion("dependency", dependencyVersion);
        DependencyGraph graph = new DependencyGraph();
        graph.addDependency(root, dependency);
        DependencySnapshot snapshot = new DependencySnapshot(
                root,
                graph,
                Map.of(
                        root.name(), new PackageMetadata(root, DependencyKind.ROOT, "root"),
                        dependency.name(), new PackageMetadata(
                                dependency, DependencyKind.TRANSITIVE, "hosted")));
        return new ProjectAnalysis(snapshot, java.util.List.of());
    }

    private record CommandResult(
            int exitCode,
            String standardOutput,
            String standardError) {
    }
}
