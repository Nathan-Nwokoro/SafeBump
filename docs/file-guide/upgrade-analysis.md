# Upgrade Analysis

This group compares two complete resolved dependency snapshots. The first is
the current project state and the second is a proposed state prepared in a
separate directory. SafeBump reads both states and never changes them.

## `core/upgrade/PackageChangeType.java`

Full path:
[`src/main/java/io/safebump/core/upgrade/PackageChangeType.java`](../../src/main/java/io/safebump/core/upgrade/PackageChangeType.java)

Defines the five possible outcomes for a named package: `ADDED`, `REMOVED`,
`UPGRADED`, `DOWNGRADED`, or `UNCHANGED`.

## `core/upgrade/PackageChange.java`

Full path:
[`src/main/java/io/safebump/core/upgrade/PackageChange.java`](../../src/main/java/io/safebump/core/upgrade/PackageChange.java)

Stores the before and after metadata appropriate to its change type. It can
also determine whether the affected package is transitive in the relevant
snapshot.

## `core/upgrade/DependencyEdge.java`

Full path:
[`src/main/java/io/safebump/core/upgrade/DependencyEdge.java`](../../src/main/java/io/safebump/core/upgrade/DependencyEdge.java)

Represents a directed package-name relationship such as `http -> async`.
Versions are excluded deliberately: upgrading a package without changing who
it depends on is a package change, not a topology change.

## `core/upgrade/DependencyGraphDiff.java`

Full path:
[`src/main/java/io/safebump/core/upgrade/DependencyGraphDiff.java`](../../src/main/java/io/safebump/core/upgrade/DependencyGraphDiff.java)

Collects every package comparison plus added and removed edges. Convenience
queries return changed packages, changes of a particular type, and transitive
changes in deterministic order.

## `core/upgrade/DependencyGraphDiffer.java`

Full path:
[`src/main/java/io/safebump/core/upgrade/DependencyGraphDiffer.java`](../../src/main/java/io/safebump/core/upgrade/DependencyGraphDiffer.java)

Unifies package names from both snapshots, compares their versions using the
version engine, and subtracts their edge sets. Non-semantic ecosystem versions
fall back to deterministic textual ordering until a future adapter supplies a
specialized comparator.

## `core/upgrade/UpgradeAnalysis.java`

Full path:
[`src/main/java/io/safebump/core/upgrade/UpgradeAnalysis.java`](../../src/main/java/io/safebump/core/upgrade/UpgradeAnalysis.java)

Bundles the complete before analysis, after analysis, and computed graph diff
so reports retain access to metadata issues in either state.

## `core/upgrade/UpgradeAnalysisService.java`

Full path:
[`src/main/java/io/safebump/core/upgrade/UpgradeAnalysisService.java`](../../src/main/java/io/safebump/core/upgrade/UpgradeAnalysisService.java)

Defines the CLI-facing contract for loading and comparing two project
directories.

## `adapters/dart/DartUpgradeAnalyzer.java`

Full path:
[`src/main/java/io/safebump/adapters/dart/DartUpgradeAnalyzer.java`](../../src/main/java/io/safebump/adapters/dart/DartUpgradeAnalyzer.java)

Implements `UpgradeAnalysisService` for Dart and Flutter by running the existing
project analyzer against both directories and passing their snapshots to the
graph differ. It rejects directories with different root package names so an
unrelated project cannot accidentally be presented as an upgrade.
