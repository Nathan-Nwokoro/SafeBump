package io.safebump.core.upgrade;

import io.safebump.core.graph.DependencyGraph;
import io.safebump.core.model.DependencySnapshot;
import io.safebump.core.model.PackageMetadata;
import io.safebump.core.model.PackageVersion;
import io.safebump.core.version.SemanticVersion;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/** Compares resolved package versions and dependency topology by package name. */
public final class DependencyGraphDiffer {

    private final Comparator<String> versionComparator;

    public DependencyGraphDiffer() {
        this(DependencyGraphDiffer::compareSemanticVersions);
    }

    public DependencyGraphDiffer(Comparator<String> versionComparator) {
        this.versionComparator = Objects.requireNonNull(
                versionComparator, "versionComparator");
    }

    public DependencyGraphDiff compare(
            DependencySnapshot before,
            DependencySnapshot after) {
        Objects.requireNonNull(before, "before");
        Objects.requireNonNull(after, "after");

        Set<String> packageNames = new TreeSet<>(before.packagesByName().keySet());
        packageNames.addAll(after.packagesByName().keySet());
        List<PackageChange> packageChanges = new ArrayList<>();
        for (String packageName : packageNames) {
            PackageMetadata beforeMetadata = before.packagesByName().get(packageName);
            PackageMetadata afterMetadata = after.packagesByName().get(packageName);
            packageChanges.add(change(packageName, beforeMetadata, afterMetadata));
        }

        Set<DependencyEdge> beforeEdges = edges(before.graph());
        Set<DependencyEdge> afterEdges = edges(after.graph());
        List<DependencyEdge> addedEdges = afterEdges.stream()
                .filter(edge -> !beforeEdges.contains(edge))
                .toList();
        List<DependencyEdge> removedEdges = beforeEdges.stream()
                .filter(edge -> !afterEdges.contains(edge))
                .toList();
        return new DependencyGraphDiff(packageChanges, addedEdges, removedEdges);
    }

    private PackageChange change(
            String packageName,
            PackageMetadata before,
            PackageMetadata after) {
        if (before == null) {
            return new PackageChange(
                    packageName, PackageChangeType.ADDED,
                    java.util.Optional.empty(), java.util.Optional.of(after));
        }
        if (after == null) {
            return new PackageChange(
                    packageName, PackageChangeType.REMOVED,
                    java.util.Optional.of(before), java.util.Optional.empty());
        }

        int versionComparison = versionComparator.compare(
                after.packageVersion().version(), before.packageVersion().version());
        PackageChangeType type = versionComparison > 0
                ? PackageChangeType.UPGRADED
                : versionComparison < 0
                        ? PackageChangeType.DOWNGRADED
                        : PackageChangeType.UNCHANGED;
        return new PackageChange(
                packageName, type,
                java.util.Optional.of(before), java.util.Optional.of(after));
    }

    private static Set<DependencyEdge> edges(DependencyGraph graph) {
        Set<DependencyEdge> edges = new TreeSet<>();
        for (PackageVersion from : graph.getPackages()) {
            for (PackageVersion to : graph.getDirectDependencies(from)) {
                edges.add(new DependencyEdge(from.name(), to.name()));
            }
        }
        return edges;
    }

    private static int compareSemanticVersions(String left, String right) {
        try {
            return SemanticVersion.parse(left).compareTo(SemanticVersion.parse(right));
        } catch (IllegalArgumentException exception) {
            return left.compareTo(right);
        }
    }
}
