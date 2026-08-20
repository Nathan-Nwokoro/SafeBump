package io.safebump.core.analysis;

import io.safebump.core.adapter.DependencySourceException;

import java.nio.file.Path;

/** Analyses one project directory using its ecosystem-specific metadata. */
@FunctionalInterface
public interface ProjectAnalysisService {

    ProjectAnalysis analyse(Path projectDirectory) throws DependencySourceException;
}
