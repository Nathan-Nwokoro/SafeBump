package io.safebump.adapters.dart;

import io.safebump.core.adapter.DependencySourceException;
import io.safebump.core.analysis.ProjectAnalysis;
import io.safebump.core.analysis.ProjectAnalysisService;
import io.safebump.core.upgrade.DependencyGraphDiff;
import io.safebump.core.upgrade.DependencyGraphDiffer;
import io.safebump.core.upgrade.UpgradeAnalysis;
import io.safebump.core.upgrade.UpgradeAnalysisService;

import java.nio.file.Path;
import java.util.Objects;

/** Builds and compares two resolved Dart or Flutter project states. */
public final class DartUpgradeAnalyzer implements UpgradeAnalysisService {

    private final ProjectAnalysisService projectAnalyzer;
    private final DependencyGraphDiffer graphDiffer;

    public DartUpgradeAnalyzer() {
        this(new DartProjectAnalyzer(), new DependencyGraphDiffer());
    }

    DartUpgradeAnalyzer(
            ProjectAnalysisService projectAnalyzer,
            DependencyGraphDiffer graphDiffer) {
        this.projectAnalyzer = Objects.requireNonNull(projectAnalyzer, "projectAnalyzer");
        this.graphDiffer = Objects.requireNonNull(graphDiffer, "graphDiffer");
    }

    @Override
    public UpgradeAnalysis analyse(Path beforeProject, Path afterProject)
            throws DependencySourceException {
        ProjectAnalysis before = projectAnalyzer.analyse(beforeProject);
        ProjectAnalysis after = projectAnalyzer.analyse(afterProject);
        String beforeName = before.dependencySnapshot().rootPackage().name();
        String afterName = after.dependencySnapshot().rootPackage().name();
        if (!beforeName.equals(afterName)) {
            throw new DependencySourceException(
                    "Cannot compare different root packages: "
                            + beforeName + " and " + afterName);
        }
        DependencyGraphDiff diff = graphDiffer.compare(
                before.dependencySnapshot(), after.dependencySnapshot());
        return new UpgradeAnalysis(before, after, diff);
    }
}
