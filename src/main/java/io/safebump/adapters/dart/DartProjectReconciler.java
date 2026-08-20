package io.safebump.adapters.dart;

import io.safebump.adapters.dart.model.DartDeclaredDependency;
import io.safebump.adapters.dart.model.DartLockfile;
import io.safebump.adapters.dart.model.DartPubspec;
import io.safebump.core.analysis.AnalysisIssue;
import io.safebump.core.model.DependencyKind;
import io.safebump.core.model.DependencySnapshot;
import io.safebump.core.model.PackageMetadata;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Reconciles Dart's graph export, pubspec declarations, and lockfile metadata. */
public final class DartProjectReconciler {

    public List<AnalysisIssue> reconcile(
            DependencySnapshot dependencySnapshot,
            DartPubspec pubspec,
            DartLockfile lockfile) {
        Objects.requireNonNull(dependencySnapshot, "dependencySnapshot");
        Objects.requireNonNull(pubspec, "pubspec");
        Objects.requireNonNull(lockfile, "lockfile");

        List<AnalysisIssue> issues = new ArrayList<>();
        reconcileRoot(dependencySnapshot, pubspec, issues);
        reconcileGraphAndLockfile(dependencySnapshot, lockfile, issues);
        reconcileDeclaredDependencies(dependencySnapshot, pubspec, issues);
        return issues.stream().sorted().toList();
    }

    private static void reconcileRoot(
            DependencySnapshot dependencySnapshot,
            DartPubspec pubspec,
            List<AnalysisIssue> issues) {
        if (!dependencySnapshot.rootPackage().name().equals(pubspec.name())) {
            issues.add(issue(
                    "ROOT_NAME_MISMATCH",
                    "Graph root " + dependencySnapshot.rootPackage().name()
                            + " does not match pubspec package " + pubspec.name()));
        }
        if (!dependencySnapshot.rootPackage().version().equals(pubspec.version())) {
            issues.add(issue(
                    "ROOT_VERSION_MISMATCH",
                    "Graph root version " + dependencySnapshot.rootPackage().version()
                            + " does not match pubspec version " + pubspec.version()));
        }
    }

    private static void reconcileGraphAndLockfile(
            DependencySnapshot dependencySnapshot,
            DartLockfile lockfile,
            List<AnalysisIssue> issues) {
        dependencySnapshot.packagesByName().forEach((packageName, graphMetadata) -> {
            if (graphMetadata.kind() == DependencyKind.ROOT) {
                return;
            }

            PackageMetadata lockedMetadata = lockfile.packages().get(packageName);
            if (lockedMetadata == null) {
                issues.add(issue(
                        "MISSING_FROM_LOCKFILE",
                        "Resolved graph package " + packageName
                                + " is missing from pubspec.lock"));
                return;
            }

            if (!graphMetadata.packageVersion().version()
                    .equals(lockedMetadata.packageVersion().version())) {
                issues.add(issue(
                        "VERSION_MISMATCH",
                        packageName + " is " + graphMetadata.packageVersion().version()
                                + " in the graph but "
                                + lockedMetadata.packageVersion().version()
                                + " in pubspec.lock"));
            }
            if (!graphMetadata.source().equals(lockedMetadata.source())) {
                issues.add(issue(
                        "SOURCE_MISMATCH",
                        packageName + " uses source " + graphMetadata.source()
                                + " in the graph but " + lockedMetadata.source()
                                + " in pubspec.lock"));
            }
            if (graphMetadata.kind() != lockedMetadata.kind()) {
                issues.add(issue(
                        "KIND_MISMATCH",
                        packageName + " is " + graphMetadata.kind()
                                + " in the graph but " + lockedMetadata.kind()
                                + " in pubspec.lock"));
            }
        });

        lockfile.packages().keySet().stream()
                .filter(packageName -> dependencySnapshot.findPackage(packageName).isEmpty())
                .forEach(packageName -> issues.add(issue(
                        "MISSING_FROM_GRAPH",
                        "Locked package " + packageName
                                + " is missing from the resolved graph")));
    }

    private static void reconcileDeclaredDependencies(
            DependencySnapshot dependencySnapshot,
            DartPubspec pubspec,
            List<AnalysisIssue> issues) {
        reconcileDeclarationSection(
                dependencySnapshot,
                pubspec.dependencies(),
                DependencyKind.DIRECT,
                issues);
        reconcileDeclarationSection(
                dependencySnapshot,
                pubspec.devDependencies(),
                DependencyKind.DEV,
                issues);
    }

    private static void reconcileDeclarationSection(
            DependencySnapshot dependencySnapshot,
            Map<String, DartDeclaredDependency> declarations,
            DependencyKind expectedKind,
            List<AnalysisIssue> issues) {
        declarations.forEach((packageName, declaration) -> {
            PackageMetadata resolvedMetadata = dependencySnapshot
                    .findPackage(packageName)
                    .orElse(null);
            if (resolvedMetadata == null) {
                issues.add(issue(
                        "DECLARED_DEPENDENCY_MISSING",
                        "Declared " + declaration.section() + " dependency " + packageName
                                + " is missing from the resolved graph"));
                return;
            }
            if (resolvedMetadata.kind() != expectedKind) {
                issues.add(issue(
                        "DECLARED_KIND_MISMATCH",
                        "Declared " + declaration.section() + " dependency " + packageName
                                + " is classified as " + resolvedMetadata.kind()
                                + " in the resolved graph"));
            }
        });
    }

    private static AnalysisIssue issue(String code, String message) {
        return new AnalysisIssue(code, message);
    }
}
