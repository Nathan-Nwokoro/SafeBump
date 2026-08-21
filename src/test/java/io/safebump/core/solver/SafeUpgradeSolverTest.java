package io.safebump.core.solver;

import io.safebump.core.model.PackageVersion;
import io.safebump.core.version.SemanticVersion;
import io.safebump.core.version.VersionRange;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SafeUpgradeSolverTest {

    private final SafeUpgradeSolver solver = new SafeUpgradeSolver();

    @Test
    void findsTheSmallestCompatibleAdditionalUpdateSet() {
        SolverProblem problem = upgradeProblem(true);

        SolverSolution solution = assertInstanceOf(
                SolverSolution.class, solver.solve(problem));

        assertEquals("2.0.0", selectedVersion(solution, "package-a"));
        assertEquals("2.0.0", selectedVersion(solution, "package-d"));
        assertEquals("5.0.0", selectedVersion(solution, "core-lib"));
        assertEquals(
                List.of("core-lib", "package-a", "package-d"),
                solution.changes().stream()
                        .map(VersionSelection::packageName)
                        .toList());
    }

    @Test
    void returnsTheRequirementsWhichMakeARequestUnsatisfiable() {
        SolverFailure failure = assertInstanceOf(
                SolverFailure.class, solver.solve(upgradeProblem(false)));

        assertEquals("core-lib", failure.packageName());
        assertEquals(2, failure.requirements().size());
        assertEquals(
                List.of("package-a", "package-d"),
                failure.requirements().stream()
                        .map(requirement -> requirement.origin().name())
                        .toList());
    }

    @Test
    void prunesBranchesThatCannotBeatTheCurrentResolution() {
        PackageCandidate root = candidate("app", "1.0.0", Map.of("tool", any()));
        SolverProblem problem = problem(
                root,
                Map.of("app", "1.0.0", "tool", "1.0.0"),
                Map.of(),
                List.of(
                        root,
                        candidate("tool", "1.0.0", Map.of()),
                        candidate("tool", "2.0.0", Map.of())));

        SolverSolution solution = assertInstanceOf(
                SolverSolution.class, solver.solve(problem));

        assertTrue(solution.changes().isEmpty());
        assertTrue(solution.prunedStates() >= 1);
    }

    @Test
    void propagatesThroughDependencyCyclesWithoutLooping() {
        PackageCandidate root = candidate("app", "1.0.0", Map.of("plugin", any()));
        SolverProblem problem = problem(
                root,
                Map.of("app", "1.0.0", "plugin", "1.0.0"),
                Map.of("plugin", "2.0.0"),
                List.of(
                        root,
                        candidate("plugin", "1.0.0", Map.of("app", any())),
                        candidate("plugin", "2.0.0", Map.of("app", any()))));

        SolverSolution solution = assertInstanceOf(
                SolverSolution.class, solver.solve(problem));

        assertEquals("2.0.0", selectedVersion(solution, "plugin"));
        assertEquals(1, solution.changes().size());
    }

    @Test
    void reportsAMissingCandidateDomain() {
        PackageCandidate root = candidate("app", "1.0.0", Map.of("missing", any()));
        SolverProblem problem = problem(
                root,
                Map.of("app", "1.0.0"),
                Map.of(),
                List.of(root));

        SolverFailure failure = assertInstanceOf(
                SolverFailure.class, solver.solve(problem));

        assertEquals("missing", failure.packageName());
        assertEquals(1, failure.requirements().size());
    }

    @Test
    void rejectsDuplicateCandidateVersionsAndMismatchedSelections() {
        PackageCandidate root = candidate("app", "1.0.0", Map.of());
        PackageCandidate duplicateRoot = candidate(
                "app", "1.0.0", Map.of("extra", any()));

        assertThrows(
                IllegalArgumentException.class,
                () -> new SolverProblem(
                        root,
                        versions(Map.of("app", "1.0.0")),
                        Map.of(),
                        Map.of("app", List.of(root, duplicateRoot))));
        assertThrows(
                IllegalArgumentException.class,
                () -> new VersionSelection(
                        "wrong-name",
                        java.util.Optional.of(version("app", "1.0.0")),
                        java.util.Optional.empty()));
    }

    private static SolverProblem upgradeProblem(boolean includeCompatiblePackageD) {
        PackageCandidate root = candidate(
                "app", "1.0.0", Map.of("package-a", any(), "package-d", any()));
        List<PackageCandidate> candidates = new ArrayList<>(List.of(
                root,
                candidate("package-a", "1.0.0", Map.of("core-lib", below("5.0.0"))),
                candidate("package-a", "2.0.0", Map.of("core-lib", atLeast("5.0.0"))),
                candidate("package-d", "1.0.0", Map.of("core-lib", below("5.0.0"))),
                candidate("core-lib", "4.0.0", Map.of()),
                candidate("core-lib", "5.0.0", Map.of())));
        if (includeCompatiblePackageD) {
            candidates.add(candidate(
                    "package-d", "2.0.0", Map.of("core-lib", atLeast("5.0.0"))));
        }
        return problem(
                root,
                Map.of(
                        "app", "1.0.0",
                        "package-a", "1.0.0",
                        "package-d", "1.0.0",
                        "core-lib", "4.0.0"),
                Map.of("package-a", "2.0.0"),
                candidates);
    }

    private static SolverProblem problem(
            PackageCandidate root,
            Map<String, String> current,
            Map<String, String> requested,
            List<PackageCandidate> candidates) {
        Map<String, PackageVersion> currentVersions = versions(current);
        Map<String, PackageVersion> requestedVersions = versions(requested);
        Map<String, List<PackageCandidate>> candidatesByPackage = new LinkedHashMap<>();
        candidates.forEach(candidate -> candidatesByPackage
                .computeIfAbsent(candidate.packageVersion().name(), ignored -> new ArrayList<>())
                .add(candidate));
        return new SolverProblem(
                root, currentVersions, requestedVersions, candidatesByPackage);
    }

    private static Map<String, PackageVersion> versions(Map<String, String> versions) {
        Map<String, PackageVersion> result = new LinkedHashMap<>();
        versions.forEach((name, version) ->
                result.put(name, new PackageVersion(name, version)));
        return result;
    }

    private static PackageCandidate candidate(
            String name,
            String version,
            Map<String, VersionRange> dependencies) {
        return new PackageCandidate(new PackageVersion(name, version), dependencies);
    }

    private static PackageVersion version(String name, String version) {
        return new PackageVersion(name, version);
    }

    private static VersionRange any() {
        return VersionRange.any();
    }

    private static VersionRange below(String version) {
        return VersionRange.atMost(SemanticVersion.parse(version), false);
    }

    private static VersionRange atLeast(String version) {
        return VersionRange.atLeast(SemanticVersion.parse(version), true);
    }

    private static String selectedVersion(SolverSolution solution, String packageName) {
        return solution.assignments().get(packageName).packageVersion().version();
    }
}
