# Multi-Ecosystem Adapters and Plugins

Phase 8 separates project detection from graph analysis. Every provider turns
native dependency data into `ProjectAnalysis`, after which traversal, diffing,
reporting, and conflict algorithms remain unchanged.

## Detection and registry

[`core/ecosystem/DependencyEcosystemProvider.java`](../../src/main/java/io/safebump/core/ecosystem/DependencyEcosystemProvider.java)
is the public plugin contract. A provider supplies a stable ID, display name,
marker-file detection, and a `ProjectAnalysisService`.

[`core/ecosystem/EcosystemRegistry.java`](../../src/main/java/io/safebump/core/ecosystem/EcosystemRegistry.java)
indexes providers and resolves a project. Zero matches produce a supported-ID
error; multiple matches require `--ecosystem <id>` instead of guessing.

[`adapters/BuiltInEcosystems.java`](../../src/main/java/io/safebump/adapters/BuiltInEcosystems.java)
registers Dart, npm, Python, Maven, and Gradle, then adds external providers.

## npm

[`adapters/npm/NpmPackageLockAdapter.java`](../../src/main/java/io/safebump/adapters/npm/NpmPackageLockAdapter.java)
parses the machine-readable `packages` map in package-lock or npm-shrinkwrap
versions 2 and 3. It implements Node's ancestor lookup for nested dependencies
and preserves simultaneous versions such as `shared@1.5.0` and `shared@2.1.0`.
No registry or `node_modules` access is required.

## Python

[`adapters/python/PythonPipInspectAdapter.java`](../../src/main/java/io/safebump/adapters/python/PythonPipInspectAdapter.java)
runs `.venv/bin/python -m pip inspect --local` when a local environment exists,
falling back to `python3`. It validates stable report schema version 1,
normalizes Python distribution names, and links installed `requires_dist`
requirements. If the project itself is not installed, direct packages are
inferred from pip's `REQUESTED` metadata and a coded warning is returned.

## Maven

[`adapters/maven/MavenDependencyTreeAdapter.java`](../../src/main/java/io/safebump/adapters/maven/MavenDependencyTreeAdapter.java)
runs the Maven Dependency Plugin's JSON tree goal through `mvnw` or `mvn`.
Coordinates use `groupId:artifactId`, and compile/test/transitive scopes map to
SafeBump's common dependency kinds.

## Gradle

[`adapters/gradle/GradleResolutionAdapter.java`](../../src/main/java/io/safebump/adapters/gradle/GradleResolutionAdapter.java)
creates a temporary init script that registers a read-only reporting task. The
task walks Gradle's `ResolutionResult` API and emits marked JSON for a
`runtimeClasspath`, `compileClasspath`, or fallback resolvable configuration.
SafeBump never edits the target build.

## External plugins

[`core/ecosystem/PluginProviderLoader.java`](../../src/main/java/io/safebump/core/ecosystem/PluginProviderLoader.java)
uses Java `ServiceLoader`. A plugin JAR must:

1. implement `DependencyEcosystemProvider` with a public no-argument constructor;
2. include `META-INF/services/io.safebump.core.ecosystem.DependencyEcosystemProvider`;
3. list the provider's fully qualified class name in that service file.

Run it with:

```bash
safebump graph /path/to/project --plugin-dir /path/to/trusted-plugin-jars
```

Plugin JARs execute code with the SafeBump process's permissions, so only load
plugins from trusted sources. Duplicate provider IDs are rejected.

## Current boundary

Graph construction, comparison, and Markdown PR reports work across all five
built-in ecosystems. The `solve` command still consumes the existing Dart-style
offline candidate catalog. Registry candidate discovery and constraint
translation for npm, Python, Maven, and Gradle remain future solver work.
