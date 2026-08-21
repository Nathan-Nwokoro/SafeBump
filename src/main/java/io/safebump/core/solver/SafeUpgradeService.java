package io.safebump.core.solver;

import io.safebump.core.adapter.DependencySourceException;

import java.nio.file.Path;

/** Loads a candidate catalog and runs the safe-upgrade solver. */
@FunctionalInterface
public interface SafeUpgradeService {

    SafeUpgradeAnalysis solve(Path catalog) throws DependencySourceException;
}
