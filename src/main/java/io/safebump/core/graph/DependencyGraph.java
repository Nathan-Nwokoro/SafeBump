package io.safebump.core.graph;

import io.safebump.core.model.PackageVersion;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * A directed graph of resolved package versions.
 *
 * <p>An edge from {@code A} to {@code B} means that {@code A} depends on
 * {@code B}. Forward and reverse adjacency maps are maintained together so
 * dependency and impact queries are both efficient.</p>
 */
public final class DependencyGraph {

    private final NavigableMap<PackageVersion, NavigableSet<PackageVersion>> dependencies =
            new TreeMap<>();
    private final NavigableMap<PackageVersion, NavigableSet<PackageVersion>> dependents =
            new TreeMap<>();

    public void addPackage(PackageVersion packageVersion) {
        PackageVersion requiredPackage = requirePackage(packageVersion);
        dependencies.computeIfAbsent(requiredPackage, ignored -> new TreeSet<>());
        dependents.computeIfAbsent(requiredPackage, ignored -> new TreeSet<>());
    }

    public void addDependency(PackageVersion from, PackageVersion to) {
        PackageVersion requiredFrom = requirePackage(from);
        PackageVersion requiredTo = requirePackage(to);

        addPackage(requiredFrom);
        addPackage(requiredTo);
        dependencies.get(requiredFrom).add(requiredTo);
        dependents.get(requiredTo).add(requiredFrom);
    }

    public boolean containsPackage(PackageVersion packageVersion) {
        return dependencies.containsKey(requirePackage(packageVersion));
    }

    public int packageCount() {
        return dependencies.size();
    }

    public int dependencyCount() {
        return dependencies.values().stream()
                .mapToInt(Set::size)
                .sum();
    }

    public Set<PackageVersion> getPackages() {
        return immutableSortedCopy(dependencies.navigableKeySet());
    }

    public Set<PackageVersion> getDirectDependencies(PackageVersion packageVersion) {
        return directNeighbours(dependencies, packageVersion);
    }

    public Set<PackageVersion> getTransitiveDependencies(PackageVersion packageVersion) {
        return transitiveNeighbours(dependencies, packageVersion);
    }

    public Set<PackageVersion> getDirectDependents(PackageVersion packageVersion) {
        return directNeighbours(dependents, packageVersion);
    }

    public Set<PackageVersion> getTransitiveDependents(PackageVersion packageVersion) {
        return transitiveNeighbours(dependents, packageVersion);
    }

    private Set<PackageVersion> directNeighbours(
            NavigableMap<PackageVersion, NavigableSet<PackageVersion>> adjacencyMap,
            PackageVersion packageVersion) {
        PackageVersion requiredPackage = requirePackage(packageVersion);
        Set<PackageVersion> neighbours = adjacencyMap.get(requiredPackage);
        return neighbours == null ? Set.of() : immutableSortedCopy(neighbours);
    }

    private Set<PackageVersion> transitiveNeighbours(
            NavigableMap<PackageVersion, NavigableSet<PackageVersion>> adjacencyMap,
            PackageVersion packageVersion) {
        PackageVersion requiredPackage = requirePackage(packageVersion);
        if (!adjacencyMap.containsKey(requiredPackage)) {
            return Set.of();
        }

        NavigableSet<PackageVersion> visited = new TreeSet<>();
        Queue<PackageVersion> packagesToVisit = new ArrayDeque<>();
        visited.add(requiredPackage);
        packagesToVisit.add(requiredPackage);

        while (!packagesToVisit.isEmpty()) {
            PackageVersion currentPackage = packagesToVisit.remove();
            for (PackageVersion neighbour : adjacencyMap.get(currentPackage)) {
                if (visited.add(neighbour)) {
                    packagesToVisit.add(neighbour);
                }
            }
        }

        visited.remove(requiredPackage);
        return immutableSortedCopy(visited);
    }

    private static Set<PackageVersion> immutableSortedCopy(Set<PackageVersion> packages) {
        return Collections.unmodifiableNavigableSet(new TreeSet<>(packages));
    }

    private static PackageVersion requirePackage(PackageVersion packageVersion) {
        return Objects.requireNonNull(packageVersion, "packageVersion");
    }
}
