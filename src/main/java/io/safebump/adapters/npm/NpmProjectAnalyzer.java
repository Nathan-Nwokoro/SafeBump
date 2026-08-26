package io.safebump.adapters.npm;

import io.safebump.core.adapter.DependencySourceException;
import io.safebump.core.analysis.ProjectAnalysis;
import io.safebump.core.analysis.ProjectAnalysisService;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/** Analyses a resolved npm project from its lockfile. */
public final class NpmProjectAnalyzer implements ProjectAnalysisService {

    private final NpmPackageLockAdapter adapter;

    public NpmProjectAnalyzer() {
        this(new NpmPackageLockAdapter());
    }

    NpmProjectAnalyzer(NpmPackageLockAdapter adapter) {
        this.adapter = Objects.requireNonNull(adapter, "adapter");
    }

    @Override
    public ProjectAnalysis analyse(Path projectDirectory) throws DependencySourceException {
        return new ProjectAnalysis(adapter.load(projectDirectory), List.of(), "npm");
    }
}
