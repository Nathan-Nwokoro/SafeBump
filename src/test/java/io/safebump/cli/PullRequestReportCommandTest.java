package io.safebump.cli;

import io.safebump.core.adapter.DependencySourceException;
import io.safebump.core.analysis.ProjectAnalysis;
import io.safebump.core.graph.DependencyGraph;
import io.safebump.core.model.DependencyKind;
import io.safebump.core.model.DependencySnapshot;
import io.safebump.core.model.PackageMetadata;
import io.safebump.core.model.PackageVersion;
import io.safebump.core.report.PullRequestReportFormatter;
import io.safebump.core.upgrade.DependencyGraphDiffer;
import io.safebump.core.upgrade.UpgradeAnalysis;
import io.safebump.core.upgrade.UpgradeAnalysisService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PullRequestReportCommandTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void writesMarkdownReportToRequestedFile() throws Exception {
        UpgradeAnalysis analysis = upgradeAnalysis();
        Path reportFile = temporaryDirectory.resolve("report.md");

        CommandResult result = execute(
                (ignoredBefore, ignoredAfter) -> analysis,
                "--output",
                reportFile.toString());

        assertEquals(0, result.exitCode());
        assertEquals("", result.standardOutput());
        assertEquals("", result.standardError());
        String report = Files.readString(reportFile);
        assertTrue(report.contains(PullRequestReportFormatter.COMMENT_MARKER));
        assertTrue(report.contains("| Packages changed | 1 |"));
    }

    @Test
    void printsMarkdownToStandardOutputByDefault() {
        CommandResult result = execute(
                (ignoredBefore, ignoredAfter) -> upgradeAnalysis());

        assertEquals(0, result.exitCode());
        assertTrue(result.standardOutput().contains("## SafeBump dependency analysis"));
        assertEquals("", result.standardError());
    }

    @Test
    void reportsSourceFailureWithInputExitCode() {
        CommandResult result = execute((ignoredBefore, ignoredAfter) -> {
            throw new DependencySourceException("No resolved project found");
        });

        assertEquals(2, result.exitCode());
        assertEquals("", result.standardOutput());
        assertTrue(result.standardError().contains("Error: No resolved project found"));
    }

    private CommandResult execute(UpgradeAnalysisService service, String... trailingArguments) {
        CommandLine commandLine = new CommandLine(new PullRequestReportCommand(
                service, new PullRequestReportFormatter()));
        StringWriter standardOutput = new StringWriter();
        StringWriter standardError = new StringWriter();
        commandLine.setOut(new PrintWriter(standardOutput, true));
        commandLine.setErr(new PrintWriter(standardError, true));

        String[] arguments = new String[2 + trailingArguments.length];
        arguments[0] = temporaryDirectory.resolve("before").toString();
        arguments[1] = temporaryDirectory.resolve("after").toString();
        System.arraycopy(trailingArguments, 0, arguments, 2, trailingArguments.length);
        int exitCode = commandLine.execute(arguments);
        return new CommandResult(exitCode, standardOutput.toString(), standardError.toString());
    }

    private static UpgradeAnalysis upgradeAnalysis() {
        ProjectAnalysis before = analysis("1.0.0");
        ProjectAnalysis after = analysis("2.0.0");
        return new UpgradeAnalysis(
                before,
                after,
                new DependencyGraphDiffer().compare(
                        before.dependencySnapshot(), after.dependencySnapshot()));
    }

    private static ProjectAnalysis analysis(String dependencyVersion) {
        PackageVersion root = new PackageVersion("app", "1.0.0");
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
        return new ProjectAnalysis(snapshot, List.of());
    }

    private record CommandResult(
            int exitCode,
            String standardOutput,
            String standardError) {
    }
}
