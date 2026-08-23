package io.safebump.core.upgrade;

import java.util.List;
import java.util.Objects;

/** Deterministic package and topology differences between two dependency graphs. */
public record DependencyGraphDiff(
        List<PackageChange> packages,
        List<DependencyEdge> addedEdges,
        List<DependencyEdge> removedEdges) {

    public DependencyGraphDiff {
        packages = immutableSorted(packages, "packages");
        addedEdges = immutableSorted(addedEdges, "addedEdges");
        removedEdges = immutableSorted(removedEdges, "removedEdges");
    }

    public List<PackageChange> changedPackages() {
        return packages.stream().filter(PackageChange::isChanged).toList();
    }

    public List<PackageChange> changesOfType(PackageChangeType type) {
        Objects.requireNonNull(type, "type");
        return packages.stream().filter(change -> change.type() == type).toList();
    }

    public List<PackageChange> transitiveChanges() {
        return changedPackages().stream().filter(PackageChange::isTransitive).toList();
    }

    public boolean isEmpty() {
        return changedPackages().isEmpty()
                && addedEdges.isEmpty()
                && removedEdges.isEmpty();
    }

    private static <T extends Comparable<? super T>> List<T> immutableSorted(
            List<T> values,
            String fieldName) {
        Objects.requireNonNull(values, fieldName);
        return values.stream()
                .map(value -> Objects.requireNonNull(value, fieldName + " value"))
                .sorted()
                .toList();
    }
}
