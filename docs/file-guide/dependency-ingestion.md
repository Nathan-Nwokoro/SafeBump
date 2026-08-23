# Dependency Ingestion and Dart Adapter

This group turns ecosystem-specific dependency data into the graph and metadata
models understood by SafeBump Core.

## `core/adapter/DependencySourceAdapter.java`

Full path:
[`src/main/java/io/safebump/core/adapter/DependencySourceAdapter.java`](../../src/main/java/io/safebump/core/adapter/DependencySourceAdapter.java)

Defines the boundary every package-ecosystem adapter must implement. Its single
operation loads a source file and returns a `DependencySnapshot`.

The core therefore does not need to know how Dart, npm, Maven, or another
ecosystem represents dependencies. A future adapter can implement the same
contract without changing the graph algorithms.

## `core/adapter/DependencySourceException.java`

Full path:
[`src/main/java/io/safebump/core/adapter/DependencySourceException.java`](../../src/main/java/io/safebump/core/adapter/DependencySourceException.java)

Represents a failure to read, parse, or validate dependency source data. It
allows the eventual CLI to show a helpful input error instead of exposing a raw
JSON or filesystem exception.

## `core/model/DependencyKind.java`

Full path:
[`src/main/java/io/safebump/core/model/DependencyKind.java`](../../src/main/java/io/safebump/core/model/DependencyKind.java)

Classifies how a resolved package relates to the analysed project:

- `ROOT` is the project being analysed.
- `DIRECT` is a normal dependency declared by the project.
- `DEV` is a development-only dependency.
- `TRANSITIVE` is brought in by another dependency.

These values are ecosystem-neutral even though the initial Dart adapter maps
them directly from Dart's package `kind` field.

## `core/model/PackageMetadata.java`

Full path:
[`src/main/java/io/safebump/core/model/PackageMetadata.java`](../../src/main/java/io/safebump/core/model/PackageMetadata.java)

Associates a `PackageVersion` with its dependency kind and source. The source is
stored as text because ecosystems use different source types. Current Dart
examples include `root`, `hosted`, `git`, `path`, and `sdk`.

## `core/model/DependencySnapshot.java`

Full path:
[`src/main/java/io/safebump/core/model/DependencySnapshot.java`](../../src/main/java/io/safebump/core/model/DependencySnapshot.java)

Bundles three parts of one resolved project state:

- the root package;
- the constructed `DependencyGraph`;
- immutable package metadata indexed by package name.

It validates that root metadata exists, the graph contains the root, and the
graph and metadata contain the same number of packages. Its `findPackage`
method provides safe metadata lookup without returning null.

## `adapters/dart/DartPubDepsAdapter.java`

Full path:
[`src/main/java/io/safebump/adapters/dart/DartPubDepsAdapter.java`](../../src/main/java/io/safebump/adapters/dart/DartPubDepsAdapter.java)

Reads the JSON produced by:

```bash
dart pub deps --json
```

The adapter performs two passes over the package list:

1. Validate and register every package and its metadata.
2. Resolve dependency names and add directed graph edges.

Two passes allow dependencies to appear before or after their dependants in the
JSON. The adapter rejects duplicate package names and edges to packages missing
from the document, preventing a silently incomplete graph.

Jackson handles JSON syntax and tree parsing. SafeBump performs its own field
validation so errors identify the relevant location, such as
`document.packages[0].version`.

Dart also emits SDK and executable information. Unknown top-level fields are
accepted for forward compatibility. The resolved graph is reconciled with
`pubspec.yaml` and `pubspec.lock` by the project-analysis layer described below.

## `adapters/dart/DartDependencyExporter.java`

Full path:
[`src/main/java/io/safebump/adapters/dart/DartDependencyExporter.java`](../../src/main/java/io/safebump/adapters/dart/DartDependencyExporter.java)

A small internal interface representing anything capable of producing Dart
dependency JSON for a project. Separating export from parsing lets tests provide
saved JSON without launching a real process.

## `adapters/dart/DartPubDepsCommand.java`

Full path:
[`src/main/java/io/safebump/adapters/dart/DartPubDepsCommand.java`](../../src/main/java/io/safebump/adapters/dart/DartPubDepsCommand.java)

Validates that the requested directory and `pubspec.yaml` exist, then executes:

```bash
dart pub deps --json -C <project-directory>
```

Standard output and standard error are consumed concurrently to prevent either
process stream from blocking the command. JSON remains isolated on standard
output, while Dart diagnostics are included in SafeBump errors when the process
fails.

The command also handles missing Dart installations, blank successful output,
and thread interruption. Its command executor is injectable so failure paths
can be tested without starting operating-system processes.

## `adapters/dart/DartProjectAdapter.java`

Full path:
[`src/main/java/io/safebump/adapters/dart/DartProjectAdapter.java`](../../src/main/java/io/safebump/adapters/dart/DartProjectAdapter.java)

Composes the process and JSON layers. It exports the selected project's graph
through `DartPubDepsCommand`, then passes that JSON to `DartPubDepsAdapter` and
returns the resulting `DependencySnapshot`.

This is the adapter used by the CLI. Keeping it behind
`DependencySourceAdapter` means the command can later select npm, Maven, or
other implementations without depending on their internal parsing logic.

## `adapters/dart/DartYamlSupport.java`

Full path:
[`src/main/java/io/safebump/adapters/dart/DartYamlSupport.java`](../../src/main/java/io/safebump/adapters/dart/DartYamlSupport.java)

Provides shared YAML reading and scalar validation for Dart project files. It
uses Jackson's YAML module and translates syntax, filesystem, and document-shape
failures into `DependencySourceException` messages containing the file type and
path.

## `adapters/dart/model/DartDependencySection.java`

Full path:
[`src/main/java/io/safebump/adapters/dart/model/DartDependencySection.java`](../../src/main/java/io/safebump/adapters/dart/model/DartDependencySection.java)

Identifies whether a declaration came from `dependencies`, `dev_dependencies`,
or `dependency_overrides`. Overrides are kept separate because overriding a
package does not by itself make that package a direct dependency.

## `adapters/dart/model/DartDeclaredDependency.java`

Full path:
[`src/main/java/io/safebump/adapters/dart/model/DartDeclaredDependency.java`](../../src/main/java/io/safebump/adapters/dart/model/DartDeclaredDependency.java)

Stores one declared package name, its pubspec section, raw version constraint,
and source. A declaration without an explicit constraint is normalized to
`any`; sources currently include `hosted`, `git`, `path`, and `sdk`.

Constraints remain text at this stage. Phase 3 will parse expressions such as
`^1.19.1` and `>=2.0.0 <3.0.0` into comparable ranges.

## `adapters/dart/model/DartPubspec.java`

Full path:
[`src/main/java/io/safebump/adapters/dart/model/DartPubspec.java`](../../src/main/java/io/safebump/adapters/dart/model/DartPubspec.java)

Represents the relevant contents of `pubspec.yaml`: project name and version,
optional Dart SDK constraint, main dependencies, development dependencies, and
dependency overrides. All maps are immutable and sorted.

Apps without a declared version use Dart's effective `0.0.0` default.

## `adapters/dart/DartPubspecParser.java`

Full path:
[`src/main/java/io/safebump/adapters/dart/DartPubspecParser.java`](../../src/main/java/io/safebump/adapters/dart/DartPubspecParser.java)

Parses Dart's supported dependency declaration forms:

- shorthand hosted constraints such as `collection: ^1.19.1`;
- unconstrained or `any` packages;
- hosted mappings with a custom repository and version;
- Git dependencies;
- local path dependencies;
- SDK dependencies;
- development dependencies and dependency overrides.

It rejects sections with the wrong YAML shape, unknown mapping fields, and
declarations that attempt to select multiple sources.

## `adapters/dart/model/DartLockfile.java`

Full path:
[`src/main/java/io/safebump/adapters/dart/model/DartLockfile.java`](../../src/main/java/io/safebump/adapters/dart/model/DartLockfile.java)

Stores immutable resolved package metadata and SDK constraints from
`pubspec.lock`. It reuses SafeBump's `PackageMetadata` model for resolved
versions, dependency kinds, and sources.

## `adapters/dart/DartLockfileParser.java`

Full path:
[`src/main/java/io/safebump/adapters/dart/DartLockfileParser.java`](../../src/main/java/io/safebump/adapters/dart/DartLockfileParser.java)

Parses each locked package's resolved version, source, and relationship:

```text
direct main → DIRECT
direct dev  → DEV
transitive  → TRANSITIVE
```

It also preserves Dart and Flutter SDK constraints. Descriptions, hashes, URLs,
Git revisions, and local paths are intentionally ignored in this first slice;
they do not affect graph identity or version comparison yet.

The lockfile does not contain every package-to-package edge, so this parser
complements `DartPubDepsAdapter` rather than replacing it.

## `core/analysis/AnalysisIssue.java`

Full path:
[`src/main/java/io/safebump/core/analysis/AnalysisIssue.java`](../../src/main/java/io/safebump/core/analysis/AnalysisIssue.java)

Represents one deterministic metadata-consistency finding. Each issue has a
stable machine-readable code and a human-readable message, and issues sort by
code and message so CLI and test output remain reproducible.

## `core/analysis/ProjectAnalysis.java`

Full path:
[`src/main/java/io/safebump/core/analysis/ProjectAnalysis.java`](../../src/main/java/io/safebump/core/analysis/ProjectAnalysis.java)

Combines a resolved `DependencySnapshot` with its sorted reconciliation issues.
The `isConsistent` helper reports whether all available project metadata agrees.

## `core/analysis/ProjectAnalysisService.java`

Full path:
[`src/main/java/io/safebump/core/analysis/ProjectAnalysisService.java`](../../src/main/java/io/safebump/core/analysis/ProjectAnalysisService.java)

Defines the ecosystem-neutral boundary used by the CLI to analyse a project.
Future ecosystems can provide their own implementation while preserving the
same command and result model.

## `adapters/dart/DartProjectReconciler.java`

Full path:
[`src/main/java/io/safebump/adapters/dart/DartProjectReconciler.java`](../../src/main/java/io/safebump/adapters/dart/DartProjectReconciler.java)

Cross-checks Dart's three views of the project: the exported dependency graph,
declared pubspec dependencies, and resolved lockfile packages. It reports root
identity, missing-package, version, source, and dependency-kind mismatches
without discarding the usable graph.

## `adapters/dart/DartProjectAnalyzer.java`

Full path:
[`src/main/java/io/safebump/adapters/dart/DartProjectAnalyzer.java`](../../src/main/java/io/safebump/adapters/dart/DartProjectAnalyzer.java)

Orchestrates a complete Dart analysis. It exports and parses the graph, reads
`pubspec.yaml` and `pubspec.lock`, reconciles all three results, and returns one
`ProjectAnalysis` to the CLI. It also invokes `DartConstraintAnalyzer` so the
resolved direct versions are checked against their effective pubspec ranges.
