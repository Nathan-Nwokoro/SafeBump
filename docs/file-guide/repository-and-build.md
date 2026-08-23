# Repository and Build Files

These files describe the project and tell Gradle how to build it. They do not
contain the dependency-graph algorithms themselves.

## `README.md`

The main introduction to SafeBump. It explains the problem, planned features,
architecture, roadmap, and current project status.

Edit this when the public description or roadmap changes.

## `.gitignore`

Lists local or generated files that Git should not commit, including Gradle
caches, compiled output, IDE settings, and macOS metadata.

Edit this when a new tool creates files that should remain local.

## `settings.gradle`

Declares the Gradle project name:

```groovy
rootProject.name = 'safebump'
```

This file will also be the place to register subprojects if SafeBump later
becomes a multi-module build.

## `build.gradle`

The main build recipe. It currently:

- enables Gradle's executable application support;
- identifies the project as `io.safebump`;
- compiles source code with Java 21 compatibility;
- adds Jackson Databind for reading ecosystem JSON files;
- adds Jackson's YAML module for Dart pubspec and lockfile parsing;
- adds Picocli for command parsing, help, and exit-code handling;
- configures `SafeBumpApplication` as the executable entry point;
- downloads test libraries from Maven Central;
- adds JUnit 5 for automated tests;
- configures Gradle's `test` task to use JUnit.

Edit this when adding a library, build plugin, application entry point, or new
build task.

The Gradle application plugin also provides:

```bash
./gradlew run
./gradlew installDist
```

`run` launches SafeBump directly. `installDist` creates a local distribution
under `build/install/safebump/` containing platform launchers and required JARs.

## `.github/workflows/ci.yml`

Full path:
[`/.github/workflows/ci.yml`](../../.github/workflows/ci.yml)

Runs on pull requests, pushes to `main`, and manual dispatches. The workflow:

- grants the job read-only repository permissions;
- cancels superseded runs for the same branch or pull request;
- installs Eclipse Temurin Java 21;
- validates the Gradle wrapper and restores the open-source Gradle cache;
- runs a clean build, all tests, and local CLI distribution creation;
- smoke-tests the packaged solver against the compatible catalog fixture;
- uploads test reports for seven days only when a job fails.

The job has a 15-minute timeout and uses `--no-daemon` so a stalled build cannot
consume a runner indefinitely.

## `action.yml`

Full path: [`/action.yml`](../../action.yml)

Defines SafeBump's composite GitHub Action for dependency-update pull requests.
It builds the action revision, compares two caller-provided resolved projects,
adds the Markdown report to the job summary, and exposes the report file to
later workflow steps.

The action itself does not request repository write permissions. The example
integration separates read-only analysis from the trusted workflow that
updates a PR comment; see the
[dependency PR analysis guide](dependency-pr-analysis.md).

## `gradle.properties`

Contains general Gradle behaviour settings. SafeBump enables build caching and
parallel task execution to make repeated builds faster.

Edit this only for project-wide Gradle options, not Java dependencies.

## `gradlew`

The Gradle wrapper launcher for macOS and Linux. Run commands through it, for
example:

```bash
./gradlew test
./gradlew build
```

The wrapper means contributors and CI do not need to install exactly the same
Gradle version themselves. This is generated code and normally should not be
edited manually.

## `gradlew.bat`

The Windows equivalent of `gradlew`. A Windows contributor runs:

```powershell
gradlew.bat test
```

This is also generated and normally should not be edited manually.

## `gradle/wrapper/gradle-wrapper.properties`

Records which Gradle distribution the wrapper downloads. It currently pins
Gradle 9.7.0, keeping local machines and CI consistent.

Change the version through Gradle's `wrapper` task when intentionally upgrading
Gradle, rather than casually editing the generated wrapper files.

## `gradle/wrapper/gradle-wrapper.jar`

A small compiled launcher used by `gradlew` and `gradlew.bat` to download and
start the pinned Gradle distribution.

It is a binary file, belongs in Git, and should not be opened or edited by hand.
