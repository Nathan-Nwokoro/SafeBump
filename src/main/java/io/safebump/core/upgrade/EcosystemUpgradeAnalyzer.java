package io.safebump.core.upgrade;

import io.safebump.core.adapter.DependencySourceException;
import io.safebump.core.analysis.ProjectAnalysis;
import io.safebump.core.ecosystem.DetectedEcosystem;
import io.safebump.core.ecosystem.EcosystemRegistry;

import java.nio.file.Path;
import java.util.Objects;

/** Compares two project states after resolving their ecosystem providers. */
public final class EcosystemUpgradeAnalyzer implements UpgradeAnalysisService {

    private final EcosystemRegistry registry;
    private final String requestedEcosystem;
    private final DependencyGraphDiffer graphDiffer;

    public EcosystemUpgradeAnalyzer(
            EcosystemRegistry registry,
            String requestedEcosystem) {
        this(registry, requestedEcosystem, new DependencyGraphDiffer());
    }

    EcosystemUpgradeAnalyzer(
            EcosystemRegistry registry,
            String requestedEcosystem,
            DependencyGraphDiffer graphDiffer) {
        this.registry = Objects.requireNonNull(registry, "registry");
        this.requestedEcosystem = requestedEcosystem;
        this.graphDiffer = Objects.requireNonNull(graphDiffer, "graphDiffer");
    }

    @Override
    public UpgradeAnalysis analyse(Path beforeProject, Path afterProject)
            throws DependencySourceException {
        DetectedEcosystem beforeEcosystem = registry.resolve(
                beforeProject, requestedEcosystem);
        DetectedEcosystem afterEcosystem = registry.resolve(
                afterProject, requestedEcosystem);
        if (!beforeEcosystem.id().equals(afterEcosystem.id())) {
            throw new DependencySourceException(
                    "Cannot compare different ecosystems: " + beforeEcosystem.id()
                            + " and " + afterEcosystem.id());
        }
        ProjectAnalysis before = registry.analyse(beforeProject, beforeEcosystem.id());
        ProjectAnalysis after = registry.analyse(afterProject, afterEcosystem.id());
        String beforeName = before.dependencySnapshot().rootPackage().name();
        String afterName = after.dependencySnapshot().rootPackage().name();
        if (!beforeName.equals(afterName)) {
            throw new DependencySourceException(
                    "Cannot compare different root packages: "
                            + beforeName + " and " + afterName);
        }
        return new UpgradeAnalysis(
                before,
                after,
                graphDiffer.compare(
                        before.dependencySnapshot(), after.dependencySnapshot()));
    }
}
