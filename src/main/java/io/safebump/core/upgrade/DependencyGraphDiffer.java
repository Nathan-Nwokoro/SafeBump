package io.safebump.core.upgrade;

import io.safebump.core.graph.DependencyGraph;
import io.safebump.core.model.DependencySnapshot;
import io.safebump.core.model.PackageMetadata;
import io.safebump.core.model.PackageVersion;
import io.safebump.core.version.SemanticVersion;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.TreeMap;

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

        Map<String, List<PackageMetadata>> beforeByName = packagesByName(before);
        Map<String, List<PackageMetadata>> afterByName = packagesByName(after);
        Set<String> packageNames = new TreeSet<>(beforeByName.keySet());
        packageNames.addAll(afterByName.keySet());
        List<PackageChange> packageChanges = new ArrayList<>();
        for (String packageName : packageNames) {
            packageChanges.addAll(changes(
                    packageName,
                    beforeByName.getOrDefault(packageName, List.of()),
                    afterByName.getOrDefault(packageName, List.of())));
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

    private List<PackageChange> changes(
            String packageName,
            List<PackageMetadata> before,
            List<PackageMetadata> after) {
        List<PackageMetadata> unmatchedBefore = new ArrayList<>(before);
        List<PackageMetadata> unmatchedAfter = new ArrayList<>(after);
        List<PackageChange> changes = new ArrayList<>();

        for (PackageMetadata beforeMetadata : before) {
            int exactIndex = indexOfVersion(unmatchedAfter,
                    beforeMetadata.packageVersion().version());
            if (exactIndex >= 0) {
                PackageMetadata afterMetadata = unmatchedAfter.remove(exactIndex);
                unmatchedBefore.remove(beforeMetadata);
                changes.add(change(packageName, beforeMetadata, afterMetadata));
            }
        }
        unmatchedBefore.sort(metadataComparator());
        unmatchedAfter.sort(metadataComparator());
        int paired = Math.min(unmatchedBefore.size(), unmatchedAfter.size());
        for (int index = 0; index < paired; index++) {
            changes.add(change(
                    packageName, unmatchedBefore.get(index), unmatchedAfter.get(index)));
        }
        for (int index = paired; index < unmatchedBefore.size(); index++) {
            changes.add(change(packageName, unmatchedBefore.get(index), null));
        }
        for (int index = paired; index < unmatchedAfter.size(); index++) {
            changes.add(change(packageName, null, unmatchedAfter.get(index)));
        }
        return changes;
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
        Map<String, Long> versionsPerName = graph.getPackages().stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        PackageVersion::name, java.util.stream.Collectors.counting()));
        for (PackageVersion from : graph.getPackages()) {
            for (PackageVersion to : graph.getDirectDependencies(from)) {
                edges.add(new DependencyEdge(
                        edgeLabel(from, versionsPerName), edgeLabel(to, versionsPerName)));
            }
        }
        return edges;
    }

    private static Map<String, List<PackageMetadata>> packagesByName(
            DependencySnapshot snapshot) {
        Map<String, List<PackageMetadata>> packages = new TreeMap<>();
        for (PackageMetadata metadata : snapshot.packagesByName().values()) {
            packages.computeIfAbsent(
                    metadata.packageVersion().name(), ignored -> new ArrayList<>())
                    .add(metadata);
        }
        packages.values().forEach(values -> values.sort(metadataComparator()));
        return packages;
    }

    private static int indexOfVersion(List<PackageMetadata> packages, String version) {
        for (int index = 0; index < packages.size(); index++) {
            if (packages.get(index).packageVersion().version().equals(version)) {
                return index;
            }
        }
        return -1;
    }

    private static Comparator<PackageMetadata> metadataComparator() {
        return Comparator.comparing(metadata -> metadata.packageVersion().version());
    }

    private static String edgeLabel(
            PackageVersion packageVersion,
            Map<String, Long> versionsPerName) {
        return versionsPerName.getOrDefault(packageVersion.name(), 0L) > 1
                ? packageVersion.toString()
                : packageVersion.name();
    }

    private static int compareSemanticVersions(String left, String right) {
        try {
            return SemanticVersion.parse(left).compareTo(SemanticVersion.parse(right));
        } catch (IllegalArgumentException exception) {
            return left.compareTo(right);
        }
    }
}
