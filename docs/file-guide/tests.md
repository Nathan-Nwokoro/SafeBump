# Tests

Automated tests live under `src/test/java`. Their package structure mirrors the
production code so each class is easy to find beside its corresponding test.

Run every test with:

```bash
./gradlew test
```

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
