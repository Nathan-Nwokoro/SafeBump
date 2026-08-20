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
        packagesByName.forEach((name, metadata) -> {
            Objects.requireNonNull(name, "package name");
            Objects.requireNonNull(metadata, "package metadata");
            if (!name.equals(metadata.packageVersion().name())) {
                throw new IllegalArgumentException(
                        "Package metadata key must match its package name: " + name);
            }
            metadataCopy.put(name, metadata);
        });
        packagesByName = Collections.unmodifiableNavigableMap(metadataCopy);

        PackageMetadata rootMetadata = packagesByName.get(rootPackage.name());
        if (rootMetadata == null || !rootMetadata.packageVersion().equals(rootPackage)) {
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
        return Optional.ofNullable(packagesByName.get(packageName));
    }
}
