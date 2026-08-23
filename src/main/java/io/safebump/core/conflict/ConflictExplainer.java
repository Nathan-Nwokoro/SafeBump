package io.safebump.core.conflict;

import io.safebump.core.graph.DependencyGraph;
import io.safebump.core.model.PackageVersion;
import io.safebump.core.version.VersionConflict;

import java.util.List;
import java.util.Objects;

/** Traces incompatible requirements back through the shortest dependency paths. */
public final class ConflictExplainer {

    public ConflictExplanation explain(
            DependencyGraph graph,
            PackageVersion root,
            VersionConflict conflict) {
        Objects.requireNonNull(graph, "graph");
        Objects.requireNonNull(root, "root");
        Objects.requireNonNull(conflict, "conflict");

        List<PackageVersion> firstPath = graph.findShortestPath(
                root, conflict.first().origin()).orElseThrow(() ->
                        missingPath(root, conflict.first().origin()));
        List<PackageVersion> secondPath = graph.findShortestPath(
                root, conflict.second().origin()).orElseThrow(() ->
                        missingPath(root, conflict.second().origin()));
        return new ConflictExplanation(conflict, firstPath, secondPath);
    }

    private static IllegalArgumentException missingPath(
            PackageVersion root,
            PackageVersion origin) {
        return new IllegalArgumentException(
                "No dependency path from " + root + " to constraint origin " + origin);
    }
}
