package io.safebump.core.upgrade;

import io.safebump.core.graph.DependencyGraph;
import io.safebump.core.model.DependencyKind;
import io.safebump.core.model.DependencySnapshot;
import io.safebump.core.model.PackageMetadata;
import io.safebump.core.model.PackageVersion;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DependencyGraphDifferTest {

    private final DependencyGraphDiffer differ = new DependencyGraphDiffer();

    @Test
    void detectsVersionPackageAndTopologyChanges() {
        DependencySnapshot before = snapshot(
                Map.of(
                        "app", packageData("1.0.0", DependencyKind.ROOT),
                        "direct", packageData("1.0.0", DependencyKind.DIRECT),
                        "shared", packageData("2.0.0", DependencyKind.TRANSITIVE),
                        "removed", packageData("1.0.0", DependencyKind.TRANSITIVE)),
                List.of(edge("app", "direct"), edge("direct", "shared"),
                        edge("app", "removed")));
        DependencySnapshot after = snapshot(
                Map.of(
                        "app", packageData("1.0.0", DependencyKind.ROOT),
                        "direct", packageData("2.0.0", DependencyKind.DIRECT),
                        "shared", packageData("1.5.0", DependencyKind.TRANSITIVE),
                        "added", packageData("1.0.0", DependencyKind.TRANSITIVE)),
                List.of(edge("app", "direct"), edge("direct", "shared"),
                        edge("direct", "added")));

        DependencyGraphDiff diff = differ.compare(before, after);

        assertEquals(List.of("added"), names(diff.changesOfType(PackageChangeType.ADDED)));
        assertEquals(List.of("removed"), names(diff.changesOfType(PackageChangeType.REMOVED)));
        assertEquals(List.of("direct"), names(diff.changesOfType(PackageChangeType.UPGRADED)));
        assertEquals(List.of("shared"), names(diff.changesOfType(PackageChangeType.DOWNGRADED)));
        assertEquals(List.of("app"), names(diff.changesOfType(PackageChangeType.UNCHANGED)));
        assertEquals(List.of("added", "removed", "shared"), names(diff.transitiveChanges()));
        assertEquals(List.of(edge("direct", "added")), diff.addedEdges());
        assertEquals(List.of(edge("app", "removed")), diff.removedEdges());
        assertFalse(diff.isEmpty());
    }

    @Test
    void reportsNoDifferenceForEquivalentSnapshots() {
        DependencySnapshot snapshot = snapshot(
                Map.of(
                        "app", packageData("1.0.0", DependencyKind.ROOT),
                        "direct", packageData("1.0.0", DependencyKind.DIRECT)),
                List.of(edge("app", "direct")));

        assertTrue(differ.compare(snapshot, snapshot).isEmpty());
    }

    private static List<String> names(List<PackageChange> changes) {
        return changes.stream().map(PackageChange::packageName).toList();
    }

    private static DependencySnapshot snapshot(
            Map<String, PackageData> packageData,
            List<DependencyEdge> edges) {
        Map<String, PackageMetadata> metadata = new LinkedHashMap<>();
        DependencyGraph graph = new DependencyGraph();
        packageData.forEach((name, data) -> {
            PackageVersion packageVersion = new PackageVersion(name, data.version());
            metadata.put(name, new PackageMetadata(packageVersion, data.kind(), "hosted"));
            graph.addPackage(packageVersion);
        });
        edges.forEach(edge -> graph.addDependency(
                metadata.get(edge.from()).packageVersion(),
                metadata.get(edge.to()).packageVersion()));
        return new DependencySnapshot(
                metadata.get("app").packageVersion(), graph, metadata);
    }

    private static PackageData packageData(String version, DependencyKind kind) {
        return new PackageData(version, kind);
    }

    private static DependencyEdge edge(String from, String to) {
        return new DependencyEdge(from, to);
    }

    private record PackageData(String version, DependencyKind kind) {
    }
}
