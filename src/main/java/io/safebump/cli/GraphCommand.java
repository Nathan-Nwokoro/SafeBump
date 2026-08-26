package io.safebump.cli;

import io.safebump.adapters.BuiltInEcosystems;
import io.safebump.core.adapter.DependencySourceException;
import io.safebump.core.analysis.AnalysisIssue;
import io.safebump.core.analysis.ProjectAnalysis;
import io.safebump.core.analysis.ProjectAnalysisService;
import io.safebump.core.model.DependencySnapshot;
import io.safebump.core.model.PackageVersion;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

/** Prints a summary of a detected project's resolved dependency graph. */
@Command(
        name = "graph",
        mixinStandardHelpOptions = true,
        description = "Build and summarize a supported dependency graph.")
public final class GraphCommand implements Callable<Integer> {

    private final ProjectAnalysisService projectAnalysisService;

    @Parameters(
            index = "0",
            paramLabel = "<project>",
            description = "Project directory containing supported dependency metadata.")
    private Path projectDirectory;

    @Option(names = "--ecosystem", paramLabel = "<id>",
            description = "Select an ecosystem when project markers are ambiguous.")
    private String ecosystem;

    @Option(names = "--plugin-dir", paramLabel = "<directory>",
            description = "Load additional ecosystem-provider JARs from this directory.")
    private Path pluginDirectory;

    @Spec
    private CommandSpec commandSpec;

    public GraphCommand() {
        this.projectAnalysisService = null;
    }

    GraphCommand(ProjectAnalysisService projectAnalysisService) {
        this.projectAnalysisService = Objects.requireNonNull(
                projectAnalysisService, "projectAnalysisService");
    }

    @Override
    public Integer call() {
        try {
            ProjectAnalysis analysis = projectAnalysisService == null
                    ? BuiltInEcosystems.create(pluginDirectory)
                            .analyse(projectDirectory, ecosystem)
                    : projectAnalysisService.analyse(projectDirectory);
            printSummary(analysis);
            return 0;
        } catch (DependencySourceException exception) {
            commandSpec.commandLine().getErr().println("Error: " + exception.getMessage());
            return 2;
        }
    }

    private void printSummary(ProjectAnalysis analysis) {
        DependencySnapshot snapshot = analysis.dependencySnapshot();
        commandSpec.commandLine().getOut().printf(
                "SafeBump dependency graph%n%n"
                        + "Ecosystem: %s%n"
                        + "Project: %s%n"
                        + "Packages: %d%n"
                        + "Dependencies: %d%n"
                        + "Cycles: %s%n"
                        + "Metadata: %s%n",
                analysis.ecosystem(),
                snapshot.rootPackage(),
                snapshot.graph().packageCount(),
                snapshot.graph().dependencyCount(),
                formatCycle(snapshot.graph().findCycle().orElse(List.of())),
                analysis.isConsistent()
                        ? "consistent"
                        : analysis.issues().size() + " issue(s)");

        for (AnalysisIssue issue : analysis.issues()) {
            commandSpec.commandLine().getOut().printf(
                    "  [%s] %s%n", issue.code(), issue.message());
        }
    }

    private static String formatCycle(List<PackageVersion> cycle) {
        if (cycle.isEmpty()) {
            return "none";
        }
        return cycle.stream()
                .map(PackageVersion::toString)
                .collect(Collectors.joining(" -> "));
    }
}
