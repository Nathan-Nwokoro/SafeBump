package io.safebump.core.upgrade;

import io.safebump.core.analysis.ProjectAnalysis;

import java.util.Objects;

/** Two resolved project analyses and their dependency-graph difference. */
public record UpgradeAnalysis(
        ProjectAnalysis before,
        ProjectAnalysis after,
        DependencyGraphDiff diff) {

    public UpgradeAnalysis {
        Objects.requireNonNull(before, "before");
        Objects.requireNonNull(after, "after");
        Objects.requireNonNull(diff, "diff");
    }
}
