package io.safebump.adapters.dart;

import io.safebump.adapters.dart.model.DartLockfile;
import io.safebump.adapters.dart.model.DartPubspec;
import io.safebump.core.adapter.DependencySourceException;
import io.safebump.core.analysis.AnalysisIssue;
import io.safebump.core.analysis.ProjectAnalysis;
import io.safebump.core.analysis.ProjectAnalysisService;
import io.safebump.core.model.DependencySnapshot;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Loads and reconciles all currently supported Dart project metadata. */
public final class DartProjectAnalyzer implements ProjectAnalysisService {

    private final DartProjectAdapter projectAdapter;
    private final DartPubspecParser pubspecParser;
    private final DartLockfileParser lockfileParser;
    private final DartProjectReconciler reconciler;
    private final DartConstraintAnalyzer constraintAnalyzer;

    public DartProjectAnalyzer() {
        this(
                new DartProjectAdapter(),
                new DartPubspecParser(),
                new DartLockfileParser(),
                new DartProjectReconciler(),
                new DartConstraintAnalyzer());
    }

    DartProjectAnalyzer(
            DartProjectAdapter projectAdapter,
            DartPubspecParser pubspecParser,
            DartLockfileParser lockfileParser,
            DartProjectReconciler reconciler,
            DartConstraintAnalyzer constraintAnalyzer) {
        this.projectAdapter = Objects.requireNonNull(projectAdapter, "projectAdapter");
        this.pubspecParser = Objects.requireNonNull(pubspecParser, "pubspecParser");
        this.lockfileParser = Objects.requireNonNull(lockfileParser, "lockfileParser");
        this.reconciler = Objects.requireNonNull(reconciler, "reconciler");
        this.constraintAnalyzer = Objects.requireNonNull(
                constraintAnalyzer, "constraintAnalyzer");
    }

    @Override
    public ProjectAnalysis analyse(Path projectDirectory) throws DependencySourceException {
        Objects.requireNonNull(projectDirectory, "projectDirectory");
        Path normalizedDirectory = projectDirectory.toAbsolutePath().normalize();

        DependencySnapshot dependencySnapshot = projectAdapter.load(normalizedDirectory);
        DartPubspec pubspec = pubspecParser.load(normalizedDirectory.resolve("pubspec.yaml"));
        DartLockfile lockfile = lockfileParser.load(normalizedDirectory.resolve("pubspec.lock"));

        List<AnalysisIssue> issues = new ArrayList<>(
                reconciler.reconcile(dependencySnapshot, pubspec, lockfile));
        issues.addAll(constraintAnalyzer.analyse(dependencySnapshot, pubspec));
        return new ProjectAnalysis(dependencySnapshot, issues, "dart");
    }
}
