package io.safebump.core.upgrade;

import io.safebump.core.adapter.DependencySourceException;

import java.nio.file.Path;

/** Loads and compares resolved dependency states from two project directories. */
@FunctionalInterface
public interface UpgradeAnalysisService {

    UpgradeAnalysis analyse(Path beforeProject, Path afterProject)
            throws DependencySourceException;
}
