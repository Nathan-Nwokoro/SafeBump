package io.safebump.core.solver;

import io.safebump.core.model.PackageVersion;
import io.safebump.core.version.SemanticVersion;
import io.safebump.core.version.VersionRange;
import io.safebump.core.version.VersionRequirement;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

/** Finds a compatible assignment while minimizing changes from the current resolution. */
public final class SafeUpgradeSolver {

    public SolveOutcome solve(SolverProblem problem) {
        Objects.requireNonNull(problem, "problem");
        Search search = new Search(problem);
        State initial = new State();
        assign(initial, problem.root());
        problem.requestedVersions().forEach((packageName, requestedVersion) ->
                addRequirement(
                        initial,
                        packageName,
                        new VersionRequirement(
                                problem.root().packageVersion(),
                                VersionRange.exact(parse(requestedVersion)))));

        search(initial, search);
        if (search.bestAssignments != null) {
            return new SolverSolution(
                    search.bestAssignments,
                    changes(problem.currentVersions(), search.bestAssignments),
                    search.exploredStates,
                    search.prunedStates);
        }

        Failure failure = search.firstFailure != null
                ? search.firstFailure
                : new Failure(problem.root().packageVersion().name(), List.of());
        return new SolverFailure(
                failure.packageName,
                failure.requirements,
                search.exploredStates,
                search.prunedStates);
    }

    private static void search(State state, Search search) {
        search.exploredStates++;
        if (!propagate(state, search)) {
            return;
        }

        int lowerBound = lowerBound(state, search.problem);
        if (lowerBound >= search.bestCost) {
            search.prunedStates++;
            return;
        }

        Optional<Branch> nextBranch = chooseBranch(state, search.problem);
        if (nextBranch.isEmpty()) {
            int cost = changes(
                    search.problem.currentVersions(), state.assignments).size();
            if (cost < search.bestCost) {
                search.bestCost = cost;
                search.bestAssignments = Map.copyOf(state.assignments);
            }
            return;
        }

        Branch branch = nextBranch.orElseThrow();
        for (PackageCandidate candidate : branch.candidates) {
            State child = state.copy();
            assign(child, candidate);
            search(child, search);
        }
    }

    private static boolean propagate(State state, Search search) {
        boolean changed;
        do {
            changed = false;
            for (Map.Entry<String, PackageCandidate> assignment
                    : state.assignments.entrySet()) {
                List<VersionRequirement> requirements = state.requirements.getOrDefault(
                        assignment.getKey(), List.of());
                if (!satisfies(assignment.getValue(), requirements)) {
                    recordFailure(search, assignment.getKey(), requirements);
                    return false;
                }
            }

            for (String packageName : unresolvedPackages(state)) {
                List<VersionRequirement> requirements = state.requirements.get(packageName);
                List<PackageCandidate> candidates = viableCandidates(
                        packageName, requirements, search.problem);
                if (candidates.isEmpty()) {
                    recordFailure(search, packageName, requirements);
                    return false;
                }
                if (candidates.size() == 1) {
                    assign(state, candidates.getFirst());
                    changed = true;
                    break;
                }
            }
        } while (changed);
        return true;
    }

    private static Optional<Branch> chooseBranch(State state, SolverProblem problem) {
        Branch best = null;
        for (String packageName : unresolvedPackages(state)) {
            List<PackageCandidate> candidates = viableCandidates(
                    packageName, state.requirements.get(packageName), problem);
            Branch candidateBranch = new Branch(packageName, candidates);
            if (best == null
                    || candidates.size() < best.candidates.size()
                    || (candidates.size() == best.candidates.size()
                            && packageName.compareTo(best.packageName) < 0)) {
                best = candidateBranch;
            }
        }
        return Optional.ofNullable(best);
    }

    private static List<PackageCandidate> viableCandidates(
            String packageName,
            List<VersionRequirement> requirements,
            SolverProblem problem) {
        PackageVersion currentVersion = problem.currentVersions().get(packageName);
        Comparator<PackageCandidate> ordering = Comparator
                .comparingInt((PackageCandidate candidate) ->
                        candidate.packageVersion().equals(currentVersion) ? 0 : 1)
                .thenComparing(
                        (PackageCandidate candidate) -> parse(candidate.packageVersion()),
                        Comparator.reverseOrder())
                .thenComparing(candidate -> candidate.packageVersion().version());
        return problem.candidatesByPackage()
                .getOrDefault(packageName, List.of())
                .stream()
                .filter(candidate -> satisfies(candidate, requirements))
                .sorted(ordering)
                .toList();
    }

    private static boolean satisfies(
            PackageCandidate candidate,
            List<VersionRequirement> requirements) {
        SemanticVersion version = parse(candidate.packageVersion());
        return requirements.stream().allMatch(requirement ->
                requirement.range().contains(version));
    }

    private static Set<String> unresolvedPackages(State state) {
        Set<String> unresolved = new TreeSet<>(state.requirements.keySet());
        unresolved.removeAll(state.assignments.keySet());
        return unresolved;
    }

    private static void assign(State state, PackageCandidate candidate) {
        String packageName = candidate.packageVersion().name();
        PackageCandidate previous = state.assignments.putIfAbsent(packageName, candidate);
        if (previous != null && !previous.equals(candidate)) {
            throw new IllegalStateException("Package already has a different assignment: "
                    + packageName);
        }
        candidate.dependencies().forEach((dependencyName, range) -> addRequirement(
                state,
                dependencyName,
                new VersionRequirement(candidate.packageVersion(), range)));
    }

    private static void addRequirement(
            State state,
            String packageName,
            VersionRequirement requirement) {
        List<VersionRequirement> requirements = state.requirements.computeIfAbsent(
                packageName, ignored -> new ArrayList<>());
        if (!requirements.contains(requirement)) {
            requirements.add(requirement);
            requirements.sort(VersionRequirement::compareTo);
        }
    }

    private static int lowerBound(State state, SolverProblem problem) {
        int changes = 0;
        for (PackageCandidate assignment : state.assignments.values()) {
            PackageVersion current = problem.currentVersions().get(
                    assignment.packageVersion().name());
            if (!assignment.packageVersion().equals(current)) {
                changes++;
            }
        }
        for (String packageName : unresolvedPackages(state)) {
            PackageVersion current = problem.currentVersions().get(packageName);
            boolean currentIsViable = viableCandidates(
                    packageName, state.requirements.get(packageName), problem)
                    .stream()
                    .anyMatch(candidate -> candidate.packageVersion().equals(current));
            if (!currentIsViable) {
                changes++;
            }
        }
        return changes;
    }

    private static List<VersionSelection> changes(
            Map<String, PackageVersion> currentVersions,
            Map<String, PackageCandidate> assignments) {
        Set<String> packageNames = new TreeSet<>(currentVersions.keySet());
        packageNames.addAll(assignments.keySet());
        List<VersionSelection> changes = new ArrayList<>();
        for (String packageName : packageNames) {
            PackageVersion before = currentVersions.get(packageName);
            PackageCandidate assigned = assignments.get(packageName);
            PackageVersion after = assigned == null ? null : assigned.packageVersion();
            if (!Objects.equals(before, after)) {
                changes.add(new VersionSelection(
                        packageName,
                        Optional.ofNullable(before),
                        Optional.ofNullable(after)));
            }
        }
        return changes.stream().sorted().toList();
    }

    private static void recordFailure(
            Search search,
            String packageName,
            List<VersionRequirement> requirements) {
        if (search.firstFailure == null) {
            search.firstFailure = new Failure(packageName, List.copyOf(requirements));
        }
    }

    private static SemanticVersion parse(PackageVersion packageVersion) {
        return SemanticVersion.parse(packageVersion.version());
    }

    private static final class State {
        private final Map<String, PackageCandidate> assignments = new LinkedHashMap<>();
        private final Map<String, List<VersionRequirement>> requirements =
                new LinkedHashMap<>();

        private State copy() {
            State copy = new State();
            copy.assignments.putAll(assignments);
            requirements.forEach((name, values) ->
                    copy.requirements.put(name, new ArrayList<>(values)));
            return copy;
        }
    }

    private static final class Search {
        private final SolverProblem problem;
        private int bestCost = Integer.MAX_VALUE;
        private Map<String, PackageCandidate> bestAssignments;
        private Failure firstFailure;
        private int exploredStates;
        private int prunedStates;

        private Search(SolverProblem problem) {
            this.problem = problem;
        }
    }

    private record Branch(String packageName, List<PackageCandidate> candidates) {
    }

    private record Failure(String packageName, List<VersionRequirement> requirements) {
    }
}
