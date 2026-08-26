package io.safebump.core.analysis;

import io.safebump.core.model.DependencySnapshot;

import java.util.List;
import java.util.Objects;

/** A resolved dependency snapshot and any cross-source consistency issues. */
public record ProjectAnalysis(
        DependencySnapshot dependencySnapshot,
        List<AnalysisIssue> issues,
        String ecosystem) {

    public ProjectAnalysis(DependencySnapshot dependencySnapshot, List<AnalysisIssue> issues) {
        this(dependencySnapshot, issues, "unknown");
    }

    public ProjectAnalysis {
        Objects.requireNonNull(dependencySnapshot, "dependencySnapshot");
        Objects.requireNonNull(issues, "issues");
        issues = issues.stream()
                .map(issue -> Objects.requireNonNull(issue, "issue"))
                .sorted()
                .toList();
        Objects.requireNonNull(ecosystem, "ecosystem");
        ecosystem = ecosystem.trim().toLowerCase(java.util.Locale.ROOT);
        if (ecosystem.isEmpty()) {
            throw new IllegalArgumentException("ecosystem must not be blank");
        }
    }

    public boolean isConsistent() {
        return issues.isEmpty();
    }
}
