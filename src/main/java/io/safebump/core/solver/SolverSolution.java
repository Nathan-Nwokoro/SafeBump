package io.safebump.core.solver;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.TreeMap;

/** The smallest compatible assignment found by the solver. */
public record SolverSolution(
        Map<String, PackageCandidate> assignments,
        List<VersionSelection> changes,
        int exploredStates,
        int prunedStates) implements SolveOutcome {

    public SolverSolution {
        Objects.requireNonNull(assignments, "assignments");
        NavigableMap<String, PackageCandidate> assignmentCopy = new TreeMap<>();
        assignments.forEach((name, candidate) -> {
            Objects.requireNonNull(name, "assignment name");
            Objects.requireNonNull(candidate, "assignment candidate");
            if (!name.equals(candidate.packageVersion().name())) {
                throw new IllegalArgumentException(
                        "Assignment key must match package name: " + name);
            }
            assignmentCopy.put(name, candidate);
        });
        assignments = Collections.unmodifiableNavigableMap(assignmentCopy);
        Objects.requireNonNull(changes, "changes");
        changes = changes.stream().sorted().toList();
        if (exploredStates < 1 || prunedStates < 0) {
            throw new IllegalArgumentException("Search counters must not be negative");
        }
    }
}
