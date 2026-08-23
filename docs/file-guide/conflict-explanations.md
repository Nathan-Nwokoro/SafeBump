# Conflict Explanations

Phase 5 connects incompatible version requirements to the dependency graph so
SafeBump can explain both what conflicts and where each requirement came from.

## `core/graph/DependencyGraph.findShortestPath`

Full path:
[`src/main/java/io/safebump/core/graph/DependencyGraph.java`](../../src/main/java/io/safebump/core/graph/DependencyGraph.java)

Uses breadth-first search to return the shortest directed path between two
resolved packages. Sorted neighbours make equal-length choices deterministic,
and a predecessor map reconstructs the path without storing every candidate
path in memory.

## `core/conflict/ConflictExplainer.java`

Full path:
[`src/main/java/io/safebump/core/conflict/ConflictExplainer.java`](../../src/main/java/io/safebump/core/conflict/ConflictExplainer.java)

Accepts a `VersionConflict`, the graph, and the root package. It finds the
shortest path from the root to each typed constraint origin and rejects a
conflict whose origin cannot be reached in that graph.

## `core/conflict/ConflictExplanation.java`

Full path:
[`src/main/java/io/safebump/core/conflict/ConflictExplanation.java`](../../src/main/java/io/safebump/core/conflict/ConflictExplanation.java)

Stores the conflict and both validated origin paths. Its `format` method emits
a readable report containing the target package, each origin and requirement,
both root-to-origin paths, and the conclusion that the constraints do not
overlap.

The current Dart JSON export contains resolved edges but not every transitive
package's original constraint text. The explanation API is therefore ready for
constraints supplied by upgrade candidates or later package-metadata
collection; it does not invent missing constraints from a resolved graph.
