# Core Graph Engine

Production Java code lives under `src/main/java`. The directory structure
matches each class's Java package name.

## `model/PackageVersion.java`

Full path:
[`src/main/java/io/safebump/core/model/PackageVersion.java`](../../src/main/java/io/safebump/core/model/PackageVersion.java)

`PackageVersion` identifies one resolved package version using two values:

```text
package name + resolved version
firebase_core + 3.8.1
```

It is a Java `record`, which makes it an immutable value object. Java generates
its accessor methods, equality logic, and hash code automatically.

The class also:

- rejects null or blank names and versions;
- removes accidental surrounding whitespace;
- sorts package identities consistently by name and then version;
- formats identities as `name@version` for readable output.

The version remains plain text in graph identity so adapters can ingest package
ecosystems with different version schemes. Code that needs semantic comparison
explicitly parses the text through the version engine.

## `graph/DependencyGraph.java`

Full path:
[`src/main/java/io/safebump/core/graph/DependencyGraph.java`](../../src/main/java/io/safebump/core/graph/DependencyGraph.java)

`DependencyGraph` stores packages and the directed relationships between them.
An edge:

```text
A → B
```

means "A depends on B."

The class maintains two adjacency maps:

```text
dependencies: A → packages required by A
dependents:   B → packages that require B
```

Keeping both maps makes queries efficient in either direction. SafeBump can ask
both "what does this package need?" and "what could be affected if this package
changes?"

Its public operations currently support:

- adding a package;
- adding a dependency edge;
- checking whether a package exists;
- counting packages and dependency edges;
- listing all packages;
- finding direct dependencies;
- finding all transitive dependencies;
- listing dependencies in depth-first order;
- finding direct dependents;
- finding all transitive dependents;
- checking whether the graph contains a cycle;
- returning one readable cycle path.

Transitive traversal uses breadth-first search with a visited set. The visited
set prevents duplicates and stops malformed cyclic graphs from causing an
infinite loop. Results are returned as immutable, sorted sets so callers cannot
change the graph accidentally and output remains deterministic.

## `graph/GraphTraversal.java`

Full path:
[`src/main/java/io/safebump/core/graph/GraphTraversal.java`](../../src/main/java/io/safebump/core/graph/GraphTraversal.java)

`GraphTraversal` contains algorithms that operate on the graph's adjacency map.
It is package-private, meaning callers use the public methods on
`DependencyGraph` rather than invoking this implementation class directly.

Its depth-first search visits a dependency branch completely before moving to
the next branch. It preserves that visit order in an immutable list and uses a
visited set so shared dependencies and cycles are handled only once.

Cycle detection uses three states:

```text
unvisited → visiting → visited
```

Finding an edge to a `visiting` package means the traversal has returned to a
package on its current path, proving that a cycle exists. SafeBump extracts that
portion of the path and repeats the starting package at the end:

```text
package_a → package_b → package_c → package_a
```

Packages and dependencies are considered in sorted order, so the same graph
produces the same traversal and cycle explanation each time.
