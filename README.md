# SafeBump

**Understand dependency upgrades before they break your project.**

SafeBump is a graph-based dependency upgrade safety engine designed to analyse the impact of package updates before they are applied.

Instead of treating a dependency upgrade as a single version change, SafeBump models the project's complete dependency structure as a directed graph. This allows it to identify transitive changes, explain conflicting version requirements, and eventually determine the smallest compatible set of package upgrades required to safely perform an update.

> **Status:** 🚧 SafeBump is currently under development.

---

## Documentation

New to the codebase? Start with the [SafeBump file guide](docs/file-guide/README.md),
which explains what each project file does and when it should be changed.

---

## The Problem

A dependency upgrade rarely affects just one package.

Consider an application with the following dependency structure:

```text
                    Application
                    /         \
                   ↓           ↓
             Package A      Package B
                 │              │
                 ↓              ↓
             Package C      Package D
                  \            /
                   \          /
                    ↓        ↓
                     Package E
```

Updating `Package A` may change the version of `Package C`, which may introduce a new requirement for `Package E`.

At the same time, `Package D` may require an older version of `Package E`.

What initially looked like:

```text
Package A
1.4.0 → 2.0.0
```

can therefore introduce a conflict several levels down the dependency graph.

Package managers can detect many dependency resolution failures, but the resulting errors do not always make it easy to understand:

* what changed transitively;
* which dependency introduced a conflicting requirement;
* the path through the dependency graph that caused the conflict;
* how large the impact of an upgrade is;
* which additional packages need to change to make the upgrade compatible.

SafeBump aims to make those relationships explicit.

---

## Example

Eventually, SafeBump will support commands such as:

```bash
safebump analyse package-a@2.0.0
```

and produce an analysis similar to:

```text
SafeBump Dependency Analysis

Requested update:
package-a 1.4.0 → 2.0.0

Transitive impact:
12 packages affected
8 upgraded
1 added
3 unchanged

Conflict detected:

package-a@2.0
    │
    └── requires core-lib >= 5.0
                         ▲
                         │
package-d@3.1 ───────────┘
    requires core-lib < 5.0

Why?

application
 → package-a
 → core-lib

application
 → package-b
 → package-d
 → core-lib

The two dependency paths impose incompatible
version constraints on core-lib.

Suggested compatible update set:

package-a  1.4.0 → 2.0.0
package-d  3.1.0 → 4.0.0

Packages changed: 2
```

---

## How It Works

SafeBump represents a project's dependencies as a directed graph:

[
G = (V, E)
]

where:

* **V** represents package versions;
* **E** represents dependency relationships.

A dependency such as:

```text
A requires B >= 2.1.0 < 3.0.0
```

can be represented as:

```text
A ──[>=2.1.0,<3.0.0]──► B
```

This allows dependency analysis to be treated as a combination of **graph traversal** and **constraint solving**.

SafeBump is being designed around algorithms and techniques including:

* breadth-first search;
* depth-first search;
* cycle detection;
* strongly connected components;
* graph differencing;
* shortest dependency paths;
* constraint propagation;
* backtracking and branch-and-bound;
* combinatorial optimisation.

Future versions may explore SAT/SMT-based dependency resolution for more complex compatibility problems.

---

## Planned Features

### Dependency Graph Construction

Parse a project's dependency metadata and construct a directed dependency graph containing both direct and transitive dependencies.

### Transitive Impact Analysis

Determine which packages would change as the result of a requested dependency upgrade.

```text
Requested update
       │
       ▼
   Package A
       │
       ▼
   Package B
      / \
     ▼   ▼
    C     D
```

Rather than reporting only the requested update, SafeBump will expose its complete dependency impact.

### Graph Diffing

Compare the dependency graph before and after an upgrade:

```text
G_before → G_after
```

and identify:

* upgraded packages;
* downgraded packages;
* added packages;
* removed packages;
* new dependency relationships;
* removed dependency relationships;
* modified version constraints.

### Conflict Explanation

When incompatible constraints are discovered, SafeBump will attempt to find the shortest dependency paths explaining where each constraint originated.

Instead of:

```text
Dependency resolution failed.
```

the goal is to provide:

```text
A → B → C requires X >= 5

A → D → E requires X < 5

These constraints do not overlap.
```

### Minimal Compatible Update Set

SafeBump will attempt to answer:

> What is the smallest set of packages that must change for this upgrade to become compatible?

This can be formulated as an optimisation problem:

[
\min \sum_i x_i
]

where:

[
x_i =
\begin{cases}
1 & \text{if package } i \text{ changes} \
0 & \text{otherwise}
\end{cases}
]

subject to all dependency version constraints being satisfied.

### Upgrade Risk Analysis

Future versions will explore scoring dependency upgrades based on characteristics such as:

* number of transitive changes;
* major-version changes;
* dependency depth;
* affected graph size;
* centrality of affected packages;
* compatibility conflicts.

---

## Architecture

SafeBump is designed so that dependency analysis is independent of any individual programming language or package ecosystem.

```text
                         CLI / CI Integration
                                 │
                                 ▼
                        ┌─────────────────┐
                        │ SafeBump Core   │
                        └────────┬────────┘
                                 │
               ┌─────────────────┼─────────────────┐
               │                 │                 │
               ▼                 ▼                 ▼
          Graph Engine    Constraint Solver    Risk Engine
               │                 │                 │
               └─────────────────┼─────────────────┘
                                 │
                         Dependency Model
                                 ▲
               ┌─────────────────┼─────────────────┐
               │                 │                 │
               ▼                 ▼                 ▼
          Dart / Flutter        npm              Maven
             Adapter          Adapter            Adapter
```

Ecosystem-specific adapters are responsible for converting dependency metadata into a common internal graph representation.

The graph and analysis engines can therefore remain ecosystem-independent.

---

## Initial Ecosystem Support

The first target is **Dart / Flutter**, using:

```text
pubspec.yaml
pubspec.lock
```

to construct dependency graphs.

The architecture is intended to allow additional package ecosystems to be added later, including:

* npm / Node.js;
* Python;
* Maven / Gradle;
* Cargo / Rust;
* NuGet / .NET;
* Go modules.

---

## Development Roadmap

### Phase 1 — Graph Engine

* [x] Package and dependency data model
* [x] Directed adjacency-list graph
* [x] BFS and DFS traversal
* [x] Transitive dependency discovery
* [x] Reverse dependency traversal
* [x] Cycle detection
* [x] Unit tests

### Phase 2 — Dart / Flutter Adapter

* [ ] Parse `pubspec.yaml`
* [ ] Parse `pubspec.lock`
* [ ] Convert dependency metadata into the SafeBump graph model
* [ ] Analyse a real Flutter project

### Phase 3 — Version Constraints

* [ ] Semantic version representation
* [ ] Version comparison
* [ ] Version ranges
* [ ] Constraint intersection
* [ ] Basic conflict detection

### Phase 4 — Upgrade Analysis

* [ ] Construct pre-upgrade graph
* [ ] Construct proposed graph
* [ ] Graph diffing
* [ ] Detect transitive upgrades
* [ ] Detect added and removed packages
* [ ] Generate upgrade-impact reports

### Phase 5 — Conflict Explanation

* [ ] Identify conflicting constraints
* [ ] Trace constraints to their originating packages
* [ ] Find minimal explanatory dependency paths
* [ ] Produce human-readable conflict reports

### Phase 6 — Safe Upgrade Solver

* [ ] Constraint propagation
* [ ] Candidate version search
* [ ] Backtracking
* [ ] Graph-based search pruning
* [ ] Minimal compatible update-set optimisation

### Future

* [ ] GitHub Actions integration
* [ ] Dependabot/Renovate PR analysis
* [ ] Additional package ecosystems
* [ ] Dependency graph visualisation
* [ ] Upgrade risk scoring
* [ ] Historical evaluation against real dependency failures
* [ ] API compatibility analysis
* [ ] SAT/SMT-based constraint solving

---

## Project Goals

SafeBump has three primary goals:

1. **Explainability** — show developers *why* an upgrade is problematic rather than returning an opaque dependency resolution error.
2. **Minimal change** — find the smallest compatible set of additional dependency updates rather than recommending broad upgrades.
3. **Ecosystem independence** — separate dependency analysis from package-manager-specific parsing so the core algorithms can operate across multiple programming languages.

---

## Current Status

SafeBump is in early development.

The initial work focuses on implementing the dependency graph and graph traversal algorithms before integrating with real package metadata.

The first real-world target will be a Flutter application, followed by support for larger open-source repositories as the analysis engine develops.

---

## Why SafeBump?

Dependency management is fundamentally a graph problem.

SafeBump explores how graph algorithms, constraint solving, and optimisation can be combined to make dependency upgrades easier to understand and safer to perform.

The long-term goal is simple:

> **Before you bump a dependency, understand the ripple effect.**
