package io.safebump.adapters.dart;

import io.safebump.core.adapter.DependencySourceException;
import io.safebump.core.solver.SafeUpgradeService;
import io.safebump.core.solver.SafeUpgradeAnalysis;
import io.safebump.core.solver.SafeUpgradeSolver;
import io.safebump.core.solver.SolverProblem;

import java.nio.file.Path;
import java.util.Objects;

/** Loads a Dart candidate catalog and searches it for a safe upgrade plan. */
public final class DartSafeUpgradeService implements SafeUpgradeService {

    private final DartSolverCatalogParser catalogParser;
    private final SafeUpgradeSolver solver;

    public DartSafeUpgradeService() {
        this(new DartSolverCatalogParser(), new SafeUpgradeSolver());
    }

    DartSafeUpgradeService(
            DartSolverCatalogParser catalogParser,
            SafeUpgradeSolver solver) {
        this.catalogParser = Objects.requireNonNull(catalogParser, "catalogParser");
        this.solver = Objects.requireNonNull(solver, "solver");
    }

    @Override
    public SafeUpgradeAnalysis solve(Path catalog) throws DependencySourceException {
        SolverProblem problem = catalogParser.load(catalog);
        return new SafeUpgradeAnalysis(problem, solver.solve(problem));
    }
}
