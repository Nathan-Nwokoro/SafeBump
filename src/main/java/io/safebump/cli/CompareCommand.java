package io.safebump.cli;

import io.safebump.adapters.BuiltInEcosystems;
import io.safebump.core.adapter.DependencySourceException;
import io.safebump.core.analysis.ProjectAnalysis;
import io.safebump.core.upgrade.DependencyGraphDiff;
import io.safebump.core.upgrade.PackageChange;
import io.safebump.core.upgrade.PackageChangeType;
import io.safebump.core.upgrade.UpgradeAnalysis;
import io.safebump.core.upgrade.UpgradeAnalysisService;
import io.safebump.core.upgrade.EcosystemUpgradeAnalyzer;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;

import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.Callable;

/** Compares two resolved Dart or Flutter dependency states. */
@Command(
        name = "compare",
        mixinStandardHelpOptions = true,
        description = "Compare dependency graphs before and after a proposed upgrade.")
public final class CompareCommand implements Callable<Integer> {

    private final UpgradeAnalysisService upgradeAnalysisService;

    @Parameters(
            index = "0",
            paramLabel = "<before-project>",
            description = "Resolved project directory before the upgrade.")
    private Path beforeProject;

    @Parameters(
            index = "1",
            paramLabel = "<after-project>",
            description = "Resolved project directory after the upgrade.")
    private Path afterProject;

    @Option(names = "--ecosystem", paramLabel = "<id>",
            description = "Select an ecosystem when project markers are ambiguous.")
    private String ecosystem;

    @Option(names = "--plugin-dir", paramLabel = "<directory>",
            description = "Load additional ecosystem-provider JARs from this directory.")
    private Path pluginDirectory;

    @Spec
    private CommandSpec commandSpec;

    public CompareCommand() {
        this.upgradeAnalysisService = null;
    }

    CompareCommand(UpgradeAnalysisService upgradeAnalysisService) {
        this.upgradeAnalysisService = Objects.requireNonNull(
                upgradeAnalysisService, "upgradeAnalysisService");
    }

    @Override
    public Integer call() {
        try {
            UpgradeAnalysisService service = upgradeAnalysisService == null
                    ? new EcosystemUpgradeAnalyzer(
                            BuiltInEcosystems.create(pluginDirectory), ecosystem)
                    : upgradeAnalysisService;
            UpgradeAnalysis analysis = service.analyse(beforeProject, afterProject);
            printAnalysis(analysis);
            return 0;
        } catch (DependencySourceException exception) {
            commandSpec.commandLine().getErr().println("Error: " + exception.getMessage());
            return 2;
        }
    }

    private void printAnalysis(UpgradeAnalysis analysis) {
        DependencyGraphDiff diff = analysis.diff();
        commandSpec.commandLine().getOut().printf(
                "SafeBump upgrade impact%n%n"
                        + "Before: %s%n"
                        + "After: %s%n"
                        + "Ecosystem: %s%n"
                        + "Before metadata: %s%n"
                        + "After metadata: %s%n"
                        + "Packages changed: %d%n"
                        + "Upgraded: %d%n"
                        + "Downgraded: %d%n"
                        + "Added: %d%n"
                        + "Removed: %d%n"
                        + "Transitive changes: %d%n"
                        + "Dependency edges added: %d%n"
                        + "Dependency edges removed: %d%n",
                analysis.before().dependencySnapshot().rootPackage(),
                analysis.after().dependencySnapshot().rootPackage(),
                analysis.before().ecosystem(),
                formatMetadata(analysis.before()),
                formatMetadata(analysis.after()),
                diff.changedPackages().size(),
                diff.changesOfType(PackageChangeType.UPGRADED).size(),
                diff.changesOfType(PackageChangeType.DOWNGRADED).size(),
                diff.changesOfType(PackageChangeType.ADDED).size(),
                diff.changesOfType(PackageChangeType.REMOVED).size(),
                diff.transitiveChanges().size(),
                diff.addedEdges().size(),
                diff.removedEdges().size());

        if (!diff.changedPackages().isEmpty()) {
            commandSpec.commandLine().getOut().println("\nPackage changes:");
            for (PackageChange change : diff.changedPackages()) {
                commandSpec.commandLine().getOut().printf(
                        "  [%s] %s%s%n",
                        change.type(),
                        formatChange(change),
                        change.isTransitive() ? " (transitive)" : "");
            }
        }
    }

    private static String formatChange(PackageChange change) {
        return switch (change.type()) {
            case ADDED -> change.packageName() + " "
                    + change.after().orElseThrow().packageVersion().version();
            case REMOVED -> change.packageName() + " "
                    + change.before().orElseThrow().packageVersion().version();
            case UPGRADED, DOWNGRADED -> change.packageName() + " "
                    + change.before().orElseThrow().packageVersion().version()
                    + " -> "
                    + change.after().orElseThrow().packageVersion().version();
            case UNCHANGED -> change.packageName();
        };
    }

    private static String formatMetadata(ProjectAnalysis analysis) {
        return analysis.isConsistent()
                ? "consistent"
                : analysis.issues().size() + " issue(s)";
    }
}
