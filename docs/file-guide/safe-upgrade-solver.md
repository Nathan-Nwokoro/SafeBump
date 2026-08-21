# Safe-Upgrade Solver

Phase 6 searches a finite candidate catalog for a compatible dependency
assignment while minimizing differences from the current resolution. The
search is deterministic and offline: candidate discovery from registries is a
separate integration concern.

## `core/solver/PackageCandidate.java`

Full path:
[`src/main/java/io/safebump/core/solver/PackageCandidate.java`](../../src/main/java/io/safebump/core/solver/PackageCandidate.java)

Stores one selectable package version and its immutable, sorted dependency
constraints.

## `core/solver/SolverProblem.java`

Full path:
[`src/main/java/io/safebump/core/solver/SolverProblem.java`](../../src/main/java/io/safebump/core/solver/SolverProblem.java)

Combines the fixed root candidate, current versions, exact requested versions,
and every available candidate. It validates package-name keys, root presence,
non-empty domains, and unique versions.

## `core/solver/VersionSelection.java`

Full path:
[`src/main/java/io/safebump/core/solver/VersionSelection.java`](../../src/main/java/io/safebump/core/solver/VersionSelection.java)

Represents one addition, removal, upgrade, or downgrade in a solution's update
set.

## `core/solver/SolveOutcome.java`

Full path:
[`src/main/java/io/safebump/core/solver/SolveOutcome.java`](../../src/main/java/io/safebump/core/solver/SolveOutcome.java)

A sealed result type shared by successful and unsuccessful searches. Both
outcomes expose explored-state and pruned-branch counts.

## `core/solver/SolverSolution.java`

Full path:
[`src/main/java/io/safebump/core/solver/SolverSolution.java`](../../src/main/java/io/safebump/core/solver/SolverSolution.java)

Contains the selected candidate for every required package and the minimal
sorted update set relative to the current versions.

## `core/solver/SolverFailure.java`

Full path:
[`src/main/java/io/safebump/core/solver/SolverFailure.java`](../../src/main/java/io/safebump/core/solver/SolverFailure.java)

Identifies the first deterministic empty candidate domain and preserves the
originating requirements which made that package impossible to select.

## `core/solver/SafeUpgradeSolver.java`

Full path:
[`src/main/java/io/safebump/core/solver/SafeUpgradeSolver.java`](../../src/main/java/io/safebump/core/solver/SafeUpgradeSolver.java)

Runs four related algorithms:

1. Constraint propagation immediately assigns single viable candidates.
2. Candidate filtering rejects versions outside every accumulated range.
3. Backtracking branches on the unresolved package with the fewest choices.
4. Branch-and-bound pruning stops a branch whose minimum possible change count
   cannot improve the best solution already found.

Current versions are tried first, followed by newer candidates. A completed
assignment is compared with the current resolution, including additions and
removals, so the chosen result minimizes the complete update set.

## `core/solver/SafeUpgradeAnalysis.java` and `SafeUpgradeService.java`

Full paths:
[`SafeUpgradeAnalysis.java`](../../src/main/java/io/safebump/core/solver/SafeUpgradeAnalysis.java) and
[`SafeUpgradeService.java`](../../src/main/java/io/safebump/core/solver/SafeUpgradeService.java)

These types retain the input problem beside its outcome and define the
CLI-facing catalog-to-solution boundary.

## `adapters/dart/DartSolverCatalogParser.java`

Full path:
[`src/main/java/io/safebump/adapters/dart/DartSolverCatalogParser.java`](../../src/main/java/io/safebump/adapters/dart/DartSolverCatalogParser.java)

Parses the offline JSON catalog. Dependency values use the same Dart constraint
syntax as pubspec declarations, including `any`, comparator ranges, and caret
constraints.

## `adapters/dart/DartSafeUpgradeService.java`

Full path:
[`src/main/java/io/safebump/adapters/dart/DartSafeUpgradeService.java`](../../src/main/java/io/safebump/adapters/dart/DartSafeUpgradeService.java)

Connects catalog parsing to the core solver without adding registry access or
filesystem mutation to the search algorithm.

## Candidate catalog shape

```json
{
  "root": {"name": "app", "version": "1.0.0"},
  "current": {"app": "1.0.0", "http": "1.0.0"},
  "requested": {"http": "2.0.0"},
  "candidates": [
    {
      "name": "app",
      "version": "1.0.0",
      "dependencies": {"http": "any"}
    },
    {"name": "http", "version": "1.0.0", "dependencies": {}},
    {"name": "http", "version": "2.0.0", "dependencies": {}}
  ]
}
```
