package io.safebump.core.report;

import io.safebump.core.analysis.AnalysisIssue;
import io.safebump.core.analysis.ProjectAnalysis;
import io.safebump.core.graph.DependencyGraph;
import io.safebump.core.model.DependencyKind;
import io.safebump.core.model.DependencySnapshot;
import io.safebump.core.model.PackageMetadata;
import io.safebump.core.model.PackageVersion;
import io.safebump.core.upgrade.DependencyGraphDiffer;
import io.safebump.core.upgrade.UpgradeAnalysis;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PullRequestReportFormatterTest {

    private final PullRequestReportFormatter formatter = new PullRequestReportFormatter();

    @Test
    void formatsDeterministicMarkdownForChangedPackagesAndEdges() {
        ProjectAnalysis before = analysis("1.0.0", "1.0.0", "old-child", List.of());
        ProjectAnalysis after = analysis(
                "1.0.0",
                "2.0.0",
                "new-child",
                List.of(new AnalysisIssue("LOCK_MISMATCH", "Resolved <version> differs")));
        UpgradeAnalysis analysis = new UpgradeAnalysis(
                before,
                after,
                new DependencyGraphDiffer().compare(
                        before.dependencySnapshot(), after.dependencySnapshot()));

        String report = formatter.format(analysis);

        assertTrue(report.startsWith(PullRequestReportFormatter.COMMENT_MARKER));
        assertTrue(report.contains("## SafeBump dependency analysis"));
        assertTrue(report.contains("| Packages changed | 3 |"));
        assertTrue(report.contains("| `dependency` | Upgraded | `1.0.0` → `2.0.0` | Direct |"));
        assertTrue(report.contains("+ dependency -> new-child"));
        assertTrue(report.contains("- dependency -> old-child"));
        assertTrue(report.contains("**After — LOCK\\_MISMATCH:** Resolved &lt;version&gt; differs"));
        assertEquals(report, formatter.format(analysis));
    }

    @Test
    void reportsWhenResolvedGraphDidNotChange() {
        ProjectAnalysis project = analysis("1.0.0", "1.0.0", "child", List.of());
        UpgradeAnalysis analysis = new UpgradeAnalysis(
                project,
                project,
                new DependencyGraphDiffer().compare(
                        project.dependencySnapshot(), project.dependencySnapshot()));

        String report = formatter.format(analysis);

        assertTrue(report.contains("No resolved package or dependency-edge changes"));
        assertTrue(report.contains("| Packages changed | 0 |"));
    }

    private static ProjectAnalysis analysis(
            String rootVersion,
            String dependencyVersion,
            String childName,
            List<AnalysisIssue> issues) {
        PackageVersion root = new PackageVersion("app", rootVersion);
        PackageVersion dependency = new PackageVersion("dependency", dependencyVersion);
        PackageVersion child = new PackageVersion(childName, "1.0.0");
        DependencyGraph graph = new DependencyGraph();
        graph.addDependency(root, dependency);
        graph.addDependency(dependency, child);
        DependencySnapshot snapshot = new DependencySnapshot(
                root,
                graph,
                Map.of(
                        root.name(), new PackageMetadata(root, DependencyKind.ROOT, "root"),
                        dependency.name(), new PackageMetadata(
                                dependency, DependencyKind.DIRECT, "hosted"),
                        child.name(), new PackageMetadata(
                                child, DependencyKind.TRANSITIVE, "hosted")));
        return new ProjectAnalysis(snapshot, issues);
    }
}
