package io.safebump.core.solver;

import java.util.Objects;

/** The candidate problem and the solver outcome used to report an upgrade search. */
public record SafeUpgradeAnalysis(SolverProblem problem, SolveOutcome outcome) {

    public SafeUpgradeAnalysis {
        Objects.requireNonNull(problem, "problem");
        Objects.requireNonNull(outcome, "outcome");
    }
}
