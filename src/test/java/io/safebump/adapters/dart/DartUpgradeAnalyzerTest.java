package io.safebump.adapters.dart;

import io.safebump.core.analysis.ProjectAnalysis;
import io.safebump.core.analysis.ProjectAnalysisService;
import io.safebump.core.graph.DependencyGraph;
import io.safebump.core.model.DependencyKind;
import io.safebump.core.model.DependencySnapshot;
import io.safebump.core.model.PackageMetadata;
import io.safebump.core.model.PackageVersion;
import io.safebump.core.upgrade.DependencyGraphDiffer;
import io.safebump.core.upgrade.PackageChangeType;
import io.safebump.core.upgrade.UpgradeAnalysis;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DartUpgradeAnalyzerTest {

    @Test
    void loadsBothProjectStatesAndBuildsTheirDiff() throws Exception {
        Path beforePath = Path.of("before");
        Path afterPath = Path.of("after");
        ProjectAnalysis before = analysis("1.0.0");
        ProjectAnalysis after = analysis("2.0.0");
        ProjectAnalysisService projectAnalyzer = path -> path.equals(beforePath)
                ? before : after;
        DartUpgradeAnalyzer analyzer = new DartUpgradeAnalyzer(
                projectAnalyzer, new DependencyGraphDiffer());

        UpgradeAnalysis result = analyzer.analyse(beforePath, afterPath);

        assertEquals(before, result.before());
        assertEquals(after, result.after());
        assertEquals(
                List.of("dependency"),
                result.diff().changesOfType(PackageChangeType.UPGRADED).stream()
                        .map(change -> change.packageName())
                        .toList());
    }

    @Test
    void rejectsStatesFromDifferentRootProjects() {
        ProjectAnalysis before = analysis("app", "1.0.0");
        ProjectAnalysis after = analysis("another_app", "1.0.0");
        DartUpgradeAnalyzer analyzer = new DartUpgradeAnalyzer(
                path -> path.toString().equals("before") ? before : after,
                new DependencyGraphDiffer());

        assertThrows(
                io.safebump.core.adapter.DependencySourceException.class,
                () -> analyzer.analyse(Path.of("before"), Path.of("after")));
    }

    private static ProjectAnalysis analysis(String dependencyVersion) {
        return analysis("app", dependencyVersion);
    }

    private static ProjectAnalysis analysis(String rootName, String dependencyVersion) {
        PackageVersion root = new PackageVersion(rootName, "1.0.0");
        PackageVersion dependency = new PackageVersion("dependency", dependencyVersion);
        DependencyGraph graph = new DependencyGraph();
        graph.addDependency(root, dependency);
        return new ProjectAnalysis(
                new DependencySnapshot(
                        root,
                        graph,
                        Map.of(
                                root.name(), new PackageMetadata(
                                        root, DependencyKind.ROOT, "root"),
                                dependency.name(), new PackageMetadata(
                                        dependency, DependencyKind.DIRECT, "hosted"))),
                List.of());
    }
}
