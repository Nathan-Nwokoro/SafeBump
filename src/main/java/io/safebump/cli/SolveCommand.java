package io.safebump.cli;

import io.safebump.adapters.dart.DartSafeUpgradeService;
import io.safebump.core.adapter.DependencySourceException;
import io.safebump.core.model.PackageVersion;
import io.safebump.core.solver.SafeUpgradeAnalysis;
import io.safebump.core.solver.SafeUpgradeService;
import io.safebump.core.solver.SolverFailure;
import io.safebump.core.solver.SolverSolution;
import io.safebump.core.solver.VersionSelection;
import io.safebump.core.version.VersionRequirement;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.Callable;

/** Searches an offline candidate catalog for the smallest compatible update set. */
@Command(
        name = "solve",
        mixinStandardHelpOptions = true,
        description = "Find the smallest compatible update set in a candidate catalog.")
public final class SolveCommand implements Callable<Integer> {

    private final SafeUpgradeService safeUpgradeService;

    @Parameters(
            index = "0",
            paramLabel = "<catalog.json>",
            description = "Dart package candidates, constraints, and requested versions.")
    private Path catalog;

    @Spec
    private CommandSpec commandSpec;

    public SolveCommand() {
        this(new DartSafeUpgradeService());
    }

    SolveCommand(SafeUpgradeService safeUpgradeService) {
        this.safeUpgradeService = Objects.requireNonNull(
                safeUpgradeService, "safeUpgradeService");
    }

    @Override
    public Integer call() {
        try {
            SafeUpgradeAnalysis analysis = safeUpgradeService.solve(catalog);
            printRequestedVersions(analysis);
            if (analysis.outcome() instanceof SolverSolution solution) {
                printSolution(solution);
                return 0;
            }
            printFailure((SolverFailure) analysis.outcome());
            return 3;
        } catch (DependencySourceException exception) {
            commandSpec.commandLine().getErr().println("Error: " + exception.getMessage());
            return 2;
        }
    }

    private void printRequestedVersions(SafeUpgradeAnalysis analysis) {
        commandSpec.commandLine().getOut().println("SafeBump safe upgrade solver\n");
        commandSpec.commandLine().getOut().println("Requested versions:");
        analysis.problem().requestedVersions().values().forEach(version ->
                commandSpec.commandLine().getOut().println("  " + version));
    }

    private void printSolution(SolverSolution solution) {
        commandSpec.commandLine().getOut().printf(
                "%nStatus: compatible%n"
                        + "Packages selected: %d%n"
                        + "Packages changed: %d%n"
                        + "States explored: %d%n"
                        + "Branches pruned: %d%n",
                solution.assignments().size(),
                solution.changes().size(),
                solution.exploredStates(),
                solution.prunedStates());
        if (!solution.changes().isEmpty()) {
            commandSpec.commandLine().getOut().println("\nMinimal update set:");
            solution.changes().forEach(change ->
                    commandSpec.commandLine().getOut().println("  " + format(change)));
        }
    }

    private void printFailure(SolverFailure failure) {
        commandSpec.commandLine().getOut().printf(
                "%nStatus: no compatible update set%n"
                        + "Blocked package: %s%n"
                        + "States explored: %d%n"
                        + "Branches pruned: %d%n",
                failure.packageName(),
                failure.exploredStates(),
                failure.prunedStates());
        if (!failure.requirements().isEmpty()) {
            commandSpec.commandLine().getOut().println("\nIncompatible requirements:");
            for (VersionRequirement requirement : failure.requirements()) {
                commandSpec.commandLine().getOut().printf(
                        "  %s requires %s %s%n",
                        requirement.origin(),
                        failure.packageName(),
                        requirement.range());
            }
        }
    }

    private static String format(VersionSelection selection) {
        String before = selection.before()
                .map(PackageVersion::version)
                .orElse("not present");
        String after = selection.after()
                .map(PackageVersion::version)
                .orElse("removed");
        return selection.packageName() + " " + before + " -> " + after;
    }
}
