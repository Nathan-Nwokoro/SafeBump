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

At this stage, the version remains plain text. Actual semantic-version parsing
and comparison belong to SafeBump's later version-constraint phase.

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
- finding direct dependents;
- finding all transitive dependents.

Transitive traversal uses breadth-first search with a visited set. The visited
set prevents duplicates and stops malformed cyclic graphs from causing an
infinite loop. Results are returned as immutable, sorted sets so callers cannot
change the graph accidentally and output remains deterministic.
