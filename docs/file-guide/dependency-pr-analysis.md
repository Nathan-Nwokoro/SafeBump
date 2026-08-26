# Dependency Pull-Request Analysis

Phase 7 turns the before/after graph comparison into a report that dependency
update bots can publish on a pull request. The analysis remains read-only: each
revision is resolved in its own checkout and SafeBump compares the resulting
graphs.

## `core/report/PullRequestReportFormatter.java`

Full path:
[`src/main/java/io/safebump/core/report/PullRequestReportFormatter.java`](../../src/main/java/io/safebump/core/report/PullRequestReportFormatter.java)

Converts an ecosystem-independent `UpgradeAnalysis` into deterministic
GitHub-flavoured Markdown. The report contains:

- package, transitive-change, and dependency-edge totals;
- a version table for every changed package;
- collapsible added and removed edge lists;
- graph/metadata reconciliation warnings;
- a stable hidden marker used to find an earlier SafeBump comment.

The formatter deliberately does not label an update safe or unsafe. It reports
resolved dependency impact, while the project's build and tests remain the
authority on application compatibility.

## `cli/PullRequestReportCommand.java`

Full path:
[`src/main/java/io/safebump/cli/PullRequestReportCommand.java`](../../src/main/java/io/safebump/cli/PullRequestReportCommand.java)

Implements:

```bash
safebump pr-report <before-project> <after-project> [--output report.md]
```

Without `--output`, the Markdown is printed to standard output. With it, the
report is written as UTF-8 for CI to upload or publish. Dependency-source and
file-writing failures return exit code `2`.

## `action.yml`

The repository's root action metadata defines a composite GitHub Action. It
sets up Java and Gradle, builds the SafeBump distribution from the checked-out
action revision, runs `pr-report`, appends the report to the job summary, and
exposes the report path as `steps.<id>.outputs.report-file`.

The calling workflow must prepare two resolved project directories before
invoking the action. All built-in ecosystems are supported. Optional `ecosystem` and
`plugin-directory` inputs handle mixed roots and trusted external providers.
This keeps package-manager setup separate from the ecosystem-independent report
engine.

## Workflow templates

[`docs/examples/dependency-pr-analysis.yml`](../examples/dependency-pr-analysis.yml)
runs with read-only permissions for an exact allow-list of Dependabot and
Renovate identities. It checks out and resolves the base and proposed states,
runs SafeBump, and uploads the report plus the validated PR number as a
short-lived artifact.

[`docs/examples/dependency-pr-comment.yml`](../examples/dependency-pr-comment.yml)
runs only after that analysis succeeds. It has permission to comment but never
checks out or executes pull-request code. It validates the downloaded artifact
and creates or updates the single comment containing SafeBump's marker. This
two-workflow design supports Dependabot's read-only pull-request token without
giving dependency-update code a write-capable token.
