package io.safebump.core.model;

import io.safebump.core.graph.DependencyGraph;

import java.util.Collections;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;

/** A resolved dependency graph and the metadata used to construct it. */
public record DependencySnapshot(
        PackageVersion rootPackage,
        DependencyGraph graph,
        Map<String, PackageMetadata> packagesByName) {

    public DependencySnapshot {
        Objects.requireNonNull(rootPackage, "rootPackage");
        Objects.requireNonNull(graph, "graph");
        Objects.requireNonNull(packagesByName, "packagesByName");

        NavigableMap<String, PackageMetadata> metadataCopy = new TreeMap<>();
        packagesByName.forEach((identity, metadata) -> {
            Objects.requireNonNull(identity, "package identity");
            Objects.requireNonNull(metadata, "package metadata");
            if (identity.isBlank()) {
                throw new IllegalArgumentException("Package identity must not be blank");
            }
            metadataCopy.put(identity, metadata);
        });
        packagesByName = Collections.unmodifiableNavigableMap(metadataCopy);

        boolean hasRootMetadata = packagesByName.values().stream()
                .anyMatch(metadata -> metadata.packageVersion().equals(rootPackage));
        if (!hasRootMetadata) {
            throw new IllegalArgumentException("Root package metadata is missing");
        }
        if (!graph.containsPackage(rootPackage)) {
            throw new IllegalArgumentException("Root package is missing from the graph");
        }
        if (graph.packageCount() != packagesByName.size()) {
            throw new IllegalArgumentException(
                    "Graph package count must match package metadata count");
        }
    }

    public Optional<PackageMetadata> findPackage(String packageName) {
        Objects.requireNonNull(packageName, "packageName");
        java.util.List<PackageMetadata> matches = findPackages(packageName);
        return matches.size() == 1 ? Optional.of(matches.getFirst()) : Optional.empty();
    }

    /** Returns every resolved version of a package name in deterministic order. */
    public java.util.List<PackageMetadata> findPackages(String packageName) {
        Objects.requireNonNull(packageName, "packageName");
        return packagesByName.values().stream()
                .filter(metadata -> metadata.packageVersion().name().equals(packageName))
                .sorted(java.util.Comparator.comparing(PackageMetadata::packageVersion))
                .toList();
    }
}
