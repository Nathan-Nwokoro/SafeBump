package io.safebump.adapters.dart;

import io.safebump.adapters.dart.model.DartDeclaredDependency;
import io.safebump.adapters.dart.model.DartPubspec;
import io.safebump.core.analysis.AnalysisIssue;
import io.safebump.core.model.DependencySnapshot;
import io.safebump.core.model.PackageMetadata;
import io.safebump.core.version.SemanticVersion;
import io.safebump.core.version.VersionRange;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Checks resolved direct Dart packages against their effective declarations. */
public final class DartConstraintAnalyzer {

    private final DartVersionConstraintParser constraintParser;

    public DartConstraintAnalyzer() {
        this(new DartVersionConstraintParser());
    }

    DartConstraintAnalyzer(DartVersionConstraintParser constraintParser) {
        this.constraintParser = Objects.requireNonNull(constraintParser, "constraintParser");
    }

    public List<AnalysisIssue> analyse(
            DependencySnapshot snapshot,
            DartPubspec pubspec) {
        Objects.requireNonNull(snapshot, "snapshot");
        Objects.requireNonNull(pubspec, "pubspec");

        Map<String, DartDeclaredDependency> effectiveDeclarations = new LinkedHashMap<>();
        effectiveDeclarations.putAll(pubspec.dependencies());
        effectiveDeclarations.putAll(pubspec.devDependencies());
        effectiveDeclarations.putAll(pubspec.dependencyOverrides());

        List<AnalysisIssue> issues = new ArrayList<>();
        effectiveDeclarations.forEach((packageName, declaration) -> snapshot
                .findPackage(packageName)
                .ifPresent(metadata -> checkDeclaration(declaration, metadata, issues)));
        return issues.stream().sorted().toList();
    }

    private void checkDeclaration(
            DartDeclaredDependency declaration,
            PackageMetadata metadata,
            List<AnalysisIssue> issues) {
        VersionRange range;
        try {
            range = constraintParser.parse(declaration.constraint());
        } catch (IllegalArgumentException exception) {
            issues.add(new AnalysisIssue(
                    "INVALID_VERSION_CONSTRAINT",
                    declaration.name() + " declares " + declaration.constraint()
                            + ": " + exception.getMessage()));
            return;
        }

        SemanticVersion resolvedVersion;
        try {
            resolvedVersion = SemanticVersion.parse(metadata.packageVersion().version());
        } catch (IllegalArgumentException exception) {
            issues.add(new AnalysisIssue(
                    "INVALID_RESOLVED_VERSION",
                    declaration.name() + " resolved to "
                            + metadata.packageVersion().version()
                            + ": " + exception.getMessage()));
            return;
        }

        if (!range.contains(resolvedVersion)) {
            issues.add(new AnalysisIssue(
                    "RESOLVED_VERSION_OUTSIDE_CONSTRAINT",
                    declaration.name() + " resolved to " + resolvedVersion
                            + " but declares " + declaration.constraint()));
        }
    }
}
