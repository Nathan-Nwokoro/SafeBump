package io.safebump.core.solver;

/** Result of a safe-upgrade search. */
public sealed interface SolveOutcome permits SolverSolution, SolverFailure {

    int exploredStates();

    int prunedStates();
}
