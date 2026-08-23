# Tests

Automated tests live under `src/test/java`. Their package structure mirrors the
production code so each class is easy to find beside its corresponding test.

Run every test with:

```bash
./gradlew test
```

The GitHub Actions workflow runs the full clean build on every pull request and
push to `main`. It also smoke-tests the installed `safebump solve` launcher and
retains HTML/XML test reports when CI fails.

## `model/PackageVersionTest.java`

Full path:
[`src/test/java/io/safebump/core/model/PackageVersionTest.java`](../../src/test/java/io/safebump/core/model/PackageVersionTest.java)

Checks that `PackageVersion`:

- trims its input values;
- produces the readable `name@version` format;
- rejects null fields;
- rejects blank fields.

These tests protect the assumptions used when package identities become graph
keys.

## `graph/DependencyGraphTest.java`

Full path:
[`src/test/java/io/safebump/core/graph/DependencyGraphTest.java`](../../src/test/java/io/safebump/core/graph/DependencyGraphTest.java)

Builds this small graph before each test:

```text
kcal
├── firebase
│   ├── core
│   └── auth
│       └── core
└── image_picker
```

It verifies that the graph:

- automatically registers packages when adding an edge;
- counts packages and unique edges correctly;
- returns direct dependencies in deterministic order;
- finds transitive dependencies without duplicates;
- traverses reverse relationships to find affected dependents;
- returns empty results for unknown packages;
- terminates safely when an input graph contains a cycle;
- finds deterministic shortest dependency paths;
- reports unreachable and unknown path targets;
- returns immutable query results;
- ignores duplicate edges.

When graph behaviour changes, this test file should be updated alongside the
production implementation. A failing test means either the implementation has a
regression or the intended behaviour needs to be stated differently.

## `graph/GraphTraversalTest.java`

Full path:
[`src/test/java/io/safebump/core/graph/GraphTraversalTest.java`](../../src/test/java/io/safebump/core/graph/GraphTraversalTest.java)

Checks the graph-traversal algorithms independently from basic graph storage. It
verifies:

- deterministic depth-first traversal order;
- empty traversal results for unknown packages;
- cycle-safe depth-first traversal;
- correct recognition of an acyclic graph;
- self-cycle detection;
- readable multi-package cycle paths;
- cycle detection in a disconnected graph.

These cases distinguish normal dependency sharing from a genuine directed
cycle and ensure traversal cannot get stuck in malformed package metadata.

## `adapters/dart/DartPubDepsAdapterTest.java`

Full path:
[`src/test/java/io/safebump/adapters/dart/DartPubDepsAdapterTest.java`](../../src/test/java/io/safebump/adapters/dart/DartPubDepsAdapterTest.java)

Loads saved `dart pub deps --json` fixtures and checks that the Dart adapter:

- identifies the project root;
- constructs the expected packages and edges;
- preserves root, direct, dev, and transitive package kinds;
- preserves hosted, Git, path, SDK, and root sources;
- accepts additional SDK and executable fields from Dart's current output;
- reports malformed JSON and missing required fields clearly;
- rejects missing dependency references and duplicate package names;
- rejects unsupported dependency kinds;
- reports unreadable input files.

## `resources/fixtures/dart/`

These JSON files provide stable test inputs without running Dart or accessing
the network during the test suite:

- `valid-pub-deps.json` represents a current Dart 3.12 dependency export with
  hosted, Git, path, SDK, dev, and transitive packages.
- `malformed-json.json` verifies JSON syntax errors.
- `missing-version.json` verifies required-field validation.
- `missing-dependency.json` references a package absent from the package list.
- `duplicate-package.json` declares the same package name twice.
- `unsupported-kind.json` contains an unknown relationship kind.

Fixtures should remain small and focused. Add a new fixture when a schema edge
case cannot be expressed clearly using an existing one.

## `adapters/dart/DartPubDepsCommandTest.java`

Full path:
[`src/test/java/io/safebump/adapters/dart/DartPubDepsCommandTest.java`](../../src/test/java/io/safebump/adapters/dart/DartPubDepsCommandTest.java)

Checks project and pubspec validation, exact Dart command construction,
non-zero Dart exits, blank output, missing executables, and interruption
handling. An injected command executor makes every case deterministic.

## `adapters/dart/DartProjectAdapterTest.java`

Full path:
[`src/test/java/io/safebump/adapters/dart/DartProjectAdapterTest.java`](../../src/test/java/io/safebump/adapters/dart/DartProjectAdapterTest.java)

Verifies that exported command output flows through JSON parsing into a complete
snapshot and that invalid command output retains useful source context.

## `adapters/dart/DartProjectReconcilerTest.java`

Full path:
[`src/test/java/io/safebump/adapters/dart/DartProjectReconcilerTest.java`](../../src/test/java/io/safebump/adapters/dart/DartProjectReconcilerTest.java)

Checks a fully consistent project plus every supported mismatch category:
project identity, missing packages, resolved versions, sources, and direct/dev
dependency classifications.

## `adapters/dart/DartProjectAnalyzerTest.java`

Full path:
[`src/test/java/io/safebump/adapters/dart/DartProjectAnalyzerTest.java`](../../src/test/java/io/safebump/adapters/dart/DartProjectAnalyzerTest.java)

Loads the complete `resources/fixtures/dart/project/consistent/` fixture through
the real JSON, pubspec, lockfile, and reconciliation layers. Only the Dart
process exporter is replaced, keeping the test deterministic and offline.

## `version/SemanticVersionTest.java`

Full path:
[`src/test/java/io/safebump/core/version/SemanticVersionTest.java`](../../src/test/java/io/safebump/core/version/SemanticVersionTest.java)

Checks component parsing, pre-release precedence, Dart pub build-suffix
ordering, round-trip formatting, and malformed version rejection.

## `version/VersionRangeTest.java`

Full path:
[`src/test/java/io/safebump/core/version/VersionRangeTest.java`](../../src/test/java/io/safebump/core/version/VersionRangeTest.java)

Checks inclusive and exclusive bounds, containment, tightened intersections,
empty intersections, and a single shared boundary version.

## `version/VersionConflictDetectorTest.java`

Full path:
[`src/test/java/io/safebump/core/version/VersionConflictDetectorTest.java`](../../src/test/java/io/safebump/core/version/VersionConflictDetectorTest.java)

Checks deterministic pairwise conflict detection and overlapping requirements.

## `adapters/dart/DartVersionConstraintParserTest.java`

Full path:
[`src/test/java/io/safebump/adapters/dart/DartVersionConstraintParserTest.java`](../../src/test/java/io/safebump/adapters/dart/DartVersionConstraintParserTest.java)

Checks `any`, exact versions, traditional comparator ranges, inclusive bounds,
and Dart's caret rules above and below version `1.0.0`.

## `adapters/dart/DartConstraintAnalyzerTest.java`

Full path:
[`src/test/java/io/safebump/adapters/dart/DartConstraintAnalyzerTest.java`](../../src/test/java/io/safebump/adapters/dart/DartConstraintAnalyzerTest.java)

Checks resolved versions inside and outside declarations, malformed constraint
and version reporting, and dependency-override precedence.

## `upgrade/DependencyGraphDifferTest.java`

Full path:
[`src/test/java/io/safebump/core/upgrade/DependencyGraphDifferTest.java`](../../src/test/java/io/safebump/core/upgrade/DependencyGraphDifferTest.java)

Checks upgrades, downgrades, additions, removals, unchanged packages,
transitive impact, topology changes, and equivalent snapshots.

## `adapters/dart/DartUpgradeAnalyzerTest.java`

Full path:
[`src/test/java/io/safebump/adapters/dart/DartUpgradeAnalyzerTest.java`](../../src/test/java/io/safebump/adapters/dart/DartUpgradeAnalyzerTest.java)

Checks that before and after project analyses are both loaded and passed into
the graph-diff engine.

## `conflict/ConflictExplainerTest.java`

Full path:
[`src/test/java/io/safebump/core/conflict/ConflictExplainerTest.java`](../../src/test/java/io/safebump/core/conflict/ConflictExplainerTest.java)

Checks shortest origin paths, readable conflict formatting, and rejection of
constraint origins which are unreachable from the project root.

## `cli/CompareCommandTest.java`

Full path:
[`src/test/java/io/safebump/cli/CompareCommandTest.java`](../../src/test/java/io/safebump/cli/CompareCommandTest.java)

Checks public upgrade-impact summaries, detailed package changes, transitive
labels, source errors, and exit codes.

## `solver/SafeUpgradeSolverTest.java`

Full path:
[`src/test/java/io/safebump/core/solver/SafeUpgradeSolverTest.java`](../../src/test/java/io/safebump/core/solver/SafeUpgradeSolverTest.java)

Checks constraint propagation, companion-package upgrades, unsatisfiable
requirements, current-version preference, branch pruning, dependency cycles,
missing candidate domains, and catalog-model invariants.

## `adapters/dart/DartSolverCatalogParserTest.java`

Full path:
[`src/test/java/io/safebump/adapters/dart/DartSolverCatalogParserTest.java`](../../src/test/java/io/safebump/adapters/dart/DartSolverCatalogParserTest.java)

Checks current and requested versions, candidate grouping, dependency
constraints, missing root candidates, malformed JSON, and unreadable files.

## `adapters/dart/DartSafeUpgradeServiceTest.java`

Full path:
[`src/test/java/io/safebump/adapters/dart/DartSafeUpgradeServiceTest.java`](../../src/test/java/io/safebump/adapters/dart/DartSafeUpgradeServiceTest.java)

Runs the complete catalog-parser-to-solver path against the committed valid
catalog fixture.

## `cli/SolveCommandTest.java`

Full path:
[`src/test/java/io/safebump/cli/SolveCommandTest.java`](../../src/test/java/io/safebump/cli/SolveCommandTest.java)

Checks compatible update reports, unsatisfiable requirement reports, catalog
errors, and the command's distinct success, input-error, and no-solution exit
codes.

## `resources/fixtures/dart/solver/`

Contains compatible and unsatisfiable candidate catalogs, missing-root and
invalid-version cases, and malformed JSON. These fixtures keep solver
integration tests offline and reproducible.

## `cli/GraphCommandTest.java`

Full path:
[`src/test/java/io/safebump/cli/GraphCommandTest.java`](../../src/test/java/io/safebump/cli/GraphCommandTest.java)

Checks successful summary output, metadata consistency and mismatch output,
readable cycle paths, error output, and exit codes using an injected project
analysis service.

## `cli/SafeBumpApplicationTest.java`

Full path:
[`src/test/java/io/safebump/cli/SafeBumpApplicationTest.java`](../../src/test/java/io/safebump/cli/SafeBumpApplicationTest.java)

Checks root help, graph-command discovery, and the application version without
terminating the test JVM.

## `adapters/dart/DartPubspecParserTest.java`

Full path:
[`src/test/java/io/safebump/adapters/dart/DartPubspecParserTest.java`](../../src/test/java/io/safebump/adapters/dart/DartPubspecParserTest.java)

Checks project identity, SDK bounds, default version behaviour, hosted
constraints, Git/path/SDK sources, custom hosted repositories, dev dependencies,
overrides, invalid sections, multiple sources, missing fields, and malformed
YAML.

Its inputs live under `resources/fixtures/dart/pubspec/`.

## `adapters/dart/DartLockfileParserTest.java`

Full path:
[`src/test/java/io/safebump/adapters/dart/DartLockfileParserTest.java`](../../src/test/java/io/safebump/adapters/dart/DartLockfileParserTest.java)

Checks resolved hosted/Git/path/SDK packages, direct/dev/transitive
classification, Dart and Flutter SDK constraints, missing versions, unsupported
kinds, invalid package structures, and malformed YAML.

Its inputs live under `resources/fixtures/dart/lockfile/`.
