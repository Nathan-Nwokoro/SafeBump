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

## `cli/DetectCommand.java`

Full path:
[`src/main/java/io/safebump/cli/DetectCommand.java`](../../src/main/java/io/safebump/cli/DetectCommand.java)

Implements:

```bash
safebump detect <project> [--ecosystem <id>] [--plugin-dir <directory>]
```

It inspects project marker files without running the package manager. A single
match is returned automatically. Mixed roots are rejected with the matching
IDs so the caller can make an explicit selection.

## `cli/GraphCommand.java`

Full path:
[`src/main/java/io/safebump/cli/GraphCommand.java`](../../src/main/java/io/safebump/cli/GraphCommand.java)

Implements:

```bash
safebump graph <project> [--ecosystem <id>] [--plugin-dir <directory>]
```

It detects the project ecosystem, loads its provider, and prints:

- the root package name and resolved version;
- the detected ecosystem ID;
- resolved package count;
- directed dependency-edge count;
- either `none` or one readable cycle path;
- whether the graph and available ecosystem metadata are consistent;
- a coded, readable line for every metadata mismatch.

Successful analysis returns exit code `0`. Input, package-manager, or structured-data
validation failures are written to standard error and return exit code `2`, so
the command is suitable for scripts and future CI integration.

Built-ins cover Dart/Flutter, npm, Python/pip, Maven, and Gradle. The command
still depends on `ProjectAnalysisService`, so plugins and tests can provide
additional implementations without changing graph code.

## `cli/CompareCommand.java`

Full path:
[`src/main/java/io/safebump/cli/CompareCommand.java`](../../src/main/java/io/safebump/cli/CompareCommand.java)

Implements:

```bash
safebump compare <before-project> <after-project> [--ecosystem <id>]
```

Both directories must contain complete, already-resolved projects from the
same ecosystem. The command detects and loads both states, then
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

## `cli/PullRequestReportCommand.java`

Full path:
[`src/main/java/io/safebump/cli/PullRequestReportCommand.java`](../../src/main/java/io/safebump/cli/PullRequestReportCommand.java)

Implements:

```bash
safebump pr-report <before-project> <after-project> [--output report.md]
  [--ecosystem <id>] [--plugin-dir <directory>]
```

It runs the same resolved graph comparison as `compare`, then formats the
result as stable GitHub-flavoured Markdown. Standard output is useful for local
inspection; `--output` gives CI a UTF-8 report file it can upload or publish.
Input and output failures return exit code `2`.
