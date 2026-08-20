# Version Constraints

This group parses version text, represents continuous ranges, intersects
requirements, and identifies incompatible constraints independently from the
dependency graph.

## `core/version/SemanticVersion.java`

Full path:
[`src/main/java/io/safebump/core/version/SemanticVersion.java`](../../src/main/java/io/safebump/core/version/SemanticVersion.java)

Parses major, minor, patch, pre-release, and build components. Comparison uses
Dart pub's semantic-version policy: normal semantic precedence applies, and
build suffixes are ordered so pub can choose between releases such as `1.0.0+1`
and `1.0.0+2`.

`PackageVersion` deliberately keeps its version as text because graph identity
must remain usable by future ecosystems. Semantic behavior is requested by
parsing that text explicitly.

## `core/version/VersionRange.java`

Full path:
[`src/main/java/io/safebump/core/version/VersionRange.java`](../../src/main/java/io/safebump/core/version/VersionRange.java)

Represents one continuous interval with optional inclusive or exclusive lower
and upper bounds. It supports unbounded and exact ranges, membership checks,
overlap checks, and intersection. An empty intersection is represented by an
empty `Optional`, making incompatibility explicit.

## `core/version/VersionRequirement.java`

Full path:
[`src/main/java/io/safebump/core/version/VersionRequirement.java`](../../src/main/java/io/safebump/core/version/VersionRequirement.java)

Associates a version range with the exact `PackageVersion` that imposed it.
Typed origins connect conflicts directly to dependency-graph paths, while
origins and ranges still sort deterministically for stable explanations.

## `core/version/VersionConflict.java`

Full path:
[`src/main/java/io/safebump/core/version/VersionConflict.java`](../../src/main/java/io/safebump/core/version/VersionConflict.java)

Records two non-overlapping requirements for the same package. Construction
rejects requirements that actually overlap, preventing false conflict objects.

## `core/version/VersionConflictDetector.java`

Full path:
[`src/main/java/io/safebump/core/version/VersionConflictDetector.java`](../../src/main/java/io/safebump/core/version/VersionConflictDetector.java)

Compares every pair of requirements for a package and returns deterministic
conflicts wherever their ranges do not intersect. Phase 5 will connect these
origins to explanatory graph paths.

## `adapters/dart/DartVersionConstraintParser.java`

Full path:
[`src/main/java/io/safebump/adapters/dart/DartVersionConstraintParser.java`](../../src/main/java/io/safebump/adapters/dart/DartVersionConstraintParser.java)

Translates Dart pub syntax into core ranges. It supports `any`, exact versions,
traditional comparisons such as `>=1.2.3 <2.0.0`, and caret constraints. Dart's
pre-`1.0.0` convention advances the minor component, so `^0.0.3` means
`>=0.0.3 <0.1.0`.

## `adapters/dart/DartConstraintAnalyzer.java`

Full path:
[`src/main/java/io/safebump/adapters/dart/DartConstraintAnalyzer.java`](../../src/main/java/io/safebump/adapters/dart/DartConstraintAnalyzer.java)

Builds the effective root declarations, giving `dependency_overrides`
precedence, then checks each available resolved version against its range. It
adds coded analysis issues for malformed constraints, malformed resolved
versions, and resolved versions outside their declared ranges.
