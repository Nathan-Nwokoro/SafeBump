# Command-Line Application

The CLI turns SafeBump's graph and adapter APIs into commands a developer can
run against a project.

## `cli/SafeBumpApplication.java`

Full path:
[`src/main/java/io/safebump/cli/SafeBumpApplication.java`](../../src/main/java/io/safebump/cli/SafeBumpApplication.java)

The Java application entry point. It configures the root `safebump` Picocli
command, standard help and version options, and available subcommands.

Its `main` method executes Picocli and returns the resulting exit code to the
operating system. Running SafeBump without a subcommand displays usage help.

## `cli/GraphCommand.java`

Full path:
[`src/main/java/io/safebump/cli/GraphCommand.java`](../../src/main/java/io/safebump/cli/GraphCommand.java)

Implements:

```bash
safebump graph <project>
```

It loads and reconciles the project through `DartProjectAnalyzer` and prints:

- the root package name and resolved version;
- resolved package count;
- directed dependency-edge count;
- either `none` or one readable cycle path;
- whether graph, pubspec, lockfile, and declared constraints are consistent;
- a coded, readable line for every metadata mismatch.

Successful analysis returns exit code `0`. Input, Dart execution, or JSON
validation failures are written to standard error and return exit code `2`, so
the command is suitable for scripts and future CI integration.

The command depends on the `ProjectAnalysisService` interface rather than the
concrete Dart implementation internally. Tests can therefore supply complete
analyses or failures without launching Dart.

## `cli/CompareCommand.java`

Full path:
[`src/main/java/io/safebump/cli/CompareCommand.java`](../../src/main/java/io/safebump/cli/CompareCommand.java)

Implements:

```bash
safebump compare <before-project> <after-project>
```

Both directories must contain complete, already-resolved Dart or Flutter
projects. The command loads both states through `DartUpgradeAnalyzer` and
prints package-change counts, transitive-change counts, topology changes, and a
deterministic package-by-package report. It also shows whether each input's
metadata is internally consistent. It is intentionally read-only and does not
run `pub upgrade` or edit either project.

## `cli/SolveCommand.java`

Full path:
[`src/main/java/io/safebump/cli/SolveCommand.java`](../../src/main/java/io/safebump/cli/SolveCommand.java)

Implements:

```bash
safebump solve <catalog.json>
```

It prints requested versions followed by either the minimal compatible update
set or the package and originating requirements which blocked all solutions.
Input failures return exit code `2`, unsatisfiable searches return `3`, and a
compatible plan returns `0`. Search-state and pruning counts make the solver's
work visible without exposing its mutable internal state.
