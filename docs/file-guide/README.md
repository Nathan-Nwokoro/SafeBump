# SafeBump File Guide

This folder explains what each hand-written or committed file in SafeBump does.
It is intended to make the project easier to explore while it is being built.

## Guide sections

1. [Repository and build files](repository-and-build.md)
2. [Core graph engine](core-engine.md)
3. [Version constraints](version-constraints.md)
4. [Upgrade analysis](upgrade-analysis.md)
5. [Conflict explanations](conflict-explanations.md)
6. [Safe-upgrade solver](safe-upgrade-solver.md)
7. [Dependency ingestion and Dart adapter](dependency-ingestion.md)
8. [Command-line application](cli.md)
9. [Dependency pull-request analysis](dependency-pr-analysis.md)
10. [Tests](tests.md)

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
├── action.yml                        Composite dependency-PR analysis action
├── .github/workflows/ci.yml          Pull-request and main-branch CI
├── docs/examples/                    Consumer GitHub workflow templates
├── src/main/java/                    Production Java code
│   └── io/safebump/
│       ├── adapters/dart/
│       │   ├── DartDependencyExporter.java
│       │   ├── DartConstraintAnalyzer.java
│       │   ├── DartLockfileParser.java
│       │   ├── DartProjectAdapter.java
│       │   ├── DartProjectAnalyzer.java
│       │   ├── DartProjectReconciler.java
│       │   ├── DartPubDepsAdapter.java
│       │   ├── DartPubDepsCommand.java
│       │   ├── DartPubspecParser.java
│       │   ├── DartSafeUpgradeService.java
│       │   ├── DartSolverCatalogParser.java
│       │   ├── DartUpgradeAnalyzer.java
│       │   ├── DartVersionConstraintParser.java
│       │   ├── DartYamlSupport.java
│       │   └── model/
│       │       ├── DartDeclaredDependency.java
│       │       ├── DartDependencySection.java
│       │       ├── DartLockfile.java
│       │       └── DartPubspec.java
│       ├── cli/
│       │   ├── CompareCommand.java
│       │   ├── GraphCommand.java
│       │   ├── PullRequestReportCommand.java
│       │   ├── SafeBumpApplication.java
│       │   └── SolveCommand.java
│       └── core/
│           ├── adapter/
│           │   ├── DependencySourceAdapter.java
│           │   └── DependencySourceException.java
│           ├── analysis/
│           │   ├── AnalysisIssue.java
│           │   ├── ProjectAnalysis.java
│           │   └── ProjectAnalysisService.java
│           ├── conflict/
│           │   ├── ConflictExplainer.java
│           │   └── ConflictExplanation.java
│           ├── model/
│           │   ├── DependencyKind.java
│           │   ├── DependencySnapshot.java
│           │   ├── PackageMetadata.java
│           │   └── PackageVersion.java
│           ├── report/
│           │   └── PullRequestReportFormatter.java
│           ├── graph/
│           │   ├── DependencyGraph.java
│           │   └── GraphTraversal.java
│           ├── solver/
│           │   ├── PackageCandidate.java
│           │   ├── SafeUpgradeAnalysis.java
│           │   ├── SafeUpgradeService.java
│           │   ├── SafeUpgradeSolver.java
│           │   ├── SolveOutcome.java
│           │   ├── SolverFailure.java
│           │   ├── SolverProblem.java
│           │   ├── SolverSolution.java
│           │   └── VersionSelection.java
│           ├── version/
│           │   ├── SemanticVersion.java
│           │   ├── VersionConflict.java
│           │   ├── VersionConflictDetector.java
│           │   ├── VersionRange.java
│           │   └── VersionRequirement.java
│           └── upgrade/
│               ├── DependencyEdge.java
│               ├── DependencyGraphDiff.java
│               ├── DependencyGraphDiffer.java
│               ├── PackageChange.java
│               ├── PackageChangeType.java
│               ├── UpgradeAnalysis.java
│               └── UpgradeAnalysisService.java
├── src/test/java/                    Automated Java tests
├── src/test/resources/               Dependency JSON test fixtures
└── docs/file-guide/                  The documentation you are reading
```

## Generated folders

You may also see `.gradle/` and `build/` after running Gradle. These are
generated locally and ignored by Git:

- `.gradle/` stores Gradle's project cache.
- `build/` stores compiled classes, test reports, and packaged output.

Neither folder should be edited manually. It is safe to recreate them by
running the build again.
