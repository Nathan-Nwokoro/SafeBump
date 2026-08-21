package io.safebump.core.solver;

import io.safebump.core.version.VersionRequirement;

import java.util.List;
import java.util.Objects;

/** The first deterministic package domain that made the search unsatisfiable. */
public record SolverFailure(
        String packageName,
        List<VersionRequirement> requirements,
        int exploredStates,
        int prunedStates) implements SolveOutcome {

    public SolverFailure {
        Objects.requireNonNull(packageName, "packageName");
        packageName = packageName.trim();
        if (packageName.isEmpty()) {
            throw new IllegalArgumentException("packageName must not be blank");
        }
        Objects.requireNonNull(requirements, "requirements");
        requirements = requirements.stream().sorted().toList();
        if (exploredStates < 1 || prunedStates < 0) {
            throw new IllegalArgumentException("Search counters must not be negative");
        }
    }
}
