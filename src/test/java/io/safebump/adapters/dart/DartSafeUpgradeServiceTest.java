package io.safebump.adapters.dart;

import io.safebump.core.solver.SafeUpgradeAnalysis;
import io.safebump.core.solver.SolverSolution;
import io.safebump.core.solver.SolverFailure;
import org.junit.jupiter.api.Test;

import java.net.URL;
import java.nio.file.Path;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class DartSafeUpgradeServiceTest {

    @Test
    void loadsAndSolvesACompleteCatalog() throws Exception {
        URL fixture = Objects.requireNonNull(
                getClass().getResource("/fixtures/dart/solver/valid-catalog.json"));

        SafeUpgradeAnalysis analysis = new DartSafeUpgradeService().solve(
                Path.of(fixture.toURI()));
        SolverSolution solution = assertInstanceOf(
                SolverSolution.class, analysis.outcome());

        assertEquals(3, solution.changes().size());
        assertEquals("5.0.0", solution.assignments().get("core-lib")
                .packageVersion().version());
    }

    @Test
    void preservesBlockingRequirementsForAnUnsatisfiableCatalog() throws Exception {
        URL fixture = Objects.requireNonNull(
                getClass().getResource(
                        "/fixtures/dart/solver/unsatisfiable-catalog.json"));

        SafeUpgradeAnalysis analysis = new DartSafeUpgradeService().solve(
                Path.of(fixture.toURI()));
        SolverFailure failure = assertInstanceOf(
                SolverFailure.class, analysis.outcome());

        assertEquals("core-lib", failure.packageName());
        assertEquals(2, failure.requirements().size());
    }
}
