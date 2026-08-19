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

- enables Gradle's Java library support;
- identifies the project as `io.safebump`;
- compiles source code with Java 21 compatibility;
- downloads test libraries from Maven Central;
- adds JUnit 5 for automated tests;
- configures Gradle's `test` task to use JUnit.

Edit this when adding a library, build plugin, application entry point, or new
build task.

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
