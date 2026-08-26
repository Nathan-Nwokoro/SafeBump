package io.safebump.core.ecosystem;

import io.safebump.core.analysis.ProjectAnalysisService;

import java.nio.file.Path;

/** Extension point that connects one package ecosystem to SafeBump's core. */
public interface DependencyEcosystemProvider {

    /** Stable lowercase identifier used by {@code --ecosystem}. */
    String id();

    /** Human-readable ecosystem name. */
    String displayName();

    /** Returns whether the directory contains this ecosystem's project markers. */
    boolean detects(Path projectDirectory);

    /** Creates an analyser for a resolved project in this ecosystem. */
    ProjectAnalysisService createAnalyzer();
}
