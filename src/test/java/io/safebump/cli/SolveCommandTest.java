package io.safebump.cli;

import io.safebump.core.adapter.DependencySourceException;
import io.safebump.core.model.PackageVersion;
import io.safebump.core.solver.PackageCandidate;
import io.safebump.core.solver.SafeUpgradeAnalysis;
import io.safebump.core.solver.SafeUpgradeService;
import io.safebump.core.solver.SolverFailure;
import io.safebump.core.solver.SolverProblem;
import io.safebump.core.solver.SolverSolution;
import io.safebump.core.solver.VersionSelection;
import io.safebump.core.version.SemanticVersion;
import io.safebump.core.version.VersionRange;
import io.safebump.core.version.VersionRequirement;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SolveCommandTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void printsTheMinimalCompatibleUpdateSet() {
        SolverProblem problem = problem();
        PackageCandidate root = problem.root();
        PackageCandidate selected = problem.candidatesByPackage().get("package-a").get(1);
        SolverSolution solution = new SolverSolution(
                Map.of("app", root, "package-a", selected),
                List.of(new VersionSelection(
                        "package-a",
                        Optional.of(version("package-a", "1.0.0")),
                        Optional.of(version("package-a", "2.0.0")))),
                4,
                1);

        CommandResult result = execute(ignored ->
                new SafeUpgradeAnalysis(problem, solution));

        assertEquals(0, result.exitCode());
        assertTrue(result.standardOutput().contains("Status: compatible"));
        assertTrue(result.standardOutput().contains("Packages changed: 1"));
        assertTrue(result.standardOutput().contains(
                "package-a 1.0.0 -> 2.0.0"));
        assertEquals("", result.standardError());
    }

    @Test
    void printsRequirementsAndReturnsNoSolutionExitCode() {
        SolverProblem problem = problem();
        SolverFailure failure = new SolverFailure(
                "core-lib",
                List.of(
                        new VersionRequirement(
                                version("package-a", "2.0.0"),
                                VersionRange.atLeast(semantic("5.0.0"), true)),
                        new VersionRequirement(
                                version("package-d", "1.0.0"),
                                VersionRange.atMost(semantic("5.0.0"), false))),
                5,
                0);

        CommandResult result = execute(ignored ->
                new SafeUpgradeAnalysis(problem, failure));

        assertEquals(3, result.exitCode());
        assertTrue(result.standardOutput().contains("Status: no compatible update set"));
        assertTrue(result.standardOutput().contains("Blocked package: core-lib"));
        assertTrue(result.standardOutput().contains(
                "package-a@2.0.0 requires core-lib >=5.0.0"));
    }

    @Test
    void printsCatalogErrorsAndReturnsInputExitCode() {
        CommandResult result = execute(ignored -> {
            throw new DependencySourceException("Invalid solver catalog");
        });

        assertEquals(2, result.exitCode());
        assertEquals("", result.standardOutput());
        assertTrue(result.standardError().contains("Error: Invalid solver catalog"));
    }

    private CommandResult execute(SafeUpgradeService service) {
        CommandLine commandLine = new CommandLine(new SolveCommand(service));
        StringWriter standardOutput = new StringWriter();
        StringWriter standardError = new StringWriter();
        commandLine.setOut(new PrintWriter(standardOutput, true));
        commandLine.setErr(new PrintWriter(standardError, true));
        int exitCode = commandLine.execute(
                temporaryDirectory.resolve("catalog.json").toString());
        return new CommandResult(exitCode, standardOutput.toString(), standardError.toString());
    }

    private static SolverProblem problem() {
        PackageCandidate root = new PackageCandidate(
                version("app", "1.0.0"), Map.of("package-a", VersionRange.any()));
        PackageCandidate current = new PackageCandidate(
                version("package-a", "1.0.0"), Map.of());
        PackageCandidate requested = new PackageCandidate(
                version("package-a", "2.0.0"), Map.of());
        return new SolverProblem(
                root,
                Map.of("app", root.packageVersion(), "package-a", current.packageVersion()),
                Map.of("package-a", requested.packageVersion()),
                Map.of("app", List.of(root), "package-a", List.of(current, requested)));
    }

    private static PackageVersion version(String name, String version) {
        return new PackageVersion(name, version);
    }

    private static SemanticVersion semantic(String version) {
        return SemanticVersion.parse(version);
    }

    private record CommandResult(
            int exitCode,
            String standardOutput,
            String standardError) {
    }
}
