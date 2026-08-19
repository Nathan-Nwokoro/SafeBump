# SafeBump File Guide

This folder explains what each hand-written or committed file in SafeBump does.
It is intended to make the project easier to explore while it is being built.

## Guide sections

1. [Repository and build files](repository-and-build.md)
2. [Core graph engine](core-engine.md)
3. [Tests](tests.md)

## Project map

```text
SafeBump/
├── README.md                         Project overview and roadmap
├── .gitignore                        Files Git should ignore
├── settings.gradle                   Gradle project identity
├── build.gradle                      Java build and test configuration
├── gradle.properties                 General Gradle settings
├── gradlew                           Gradle launcher for macOS/Linux
├── gradlew.bat                       Gradle launcher for Windows
├── gradle/wrapper/                   Pinned Gradle version and launcher code
├── src/main/java/                    Production Java code
│   └── io/safebump/core/
│       ├── model/PackageVersion.java
│       └── graph/DependencyGraph.java
├── src/test/java/                    Automated Java tests
│   └── io/safebump/core/
│       ├── model/PackageVersionTest.java
│       └── graph/DependencyGraphTest.java
└── docs/file-guide/                  The documentation you are reading
```

## Generated folders

You may also see `.gradle/` and `build/` after running Gradle. These are
generated locally and ignored by Git:

- `.gradle/` stores Gradle's project cache.
- `build/` stores compiled classes, test reports, and packaged output.

Neither folder should be edited manually. It is safe to recreate them by
running the build again.
