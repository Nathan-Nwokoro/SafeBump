package io.safebump.adapters.dart;

import io.safebump.adapters.dart.model.DartDeclaredDependency;
import io.safebump.adapters.dart.model.DartDependencySection;
import io.safebump.adapters.dart.model.DartPubspec;
import io.safebump.core.analysis.AnalysisIssue;
import io.safebump.core.graph.DependencyGraph;
import io.safebump.core.model.DependencyKind;
import io.safebump.core.model.DependencySnapshot;
import io.safebump.core.model.PackageMetadata;
import io.safebump.core.model.PackageVersion;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DartConstraintAnalyzerTest {

    private final DartConstraintAnalyzer analyzer = new DartConstraintAnalyzer();

    @Test
    void acceptsAResolvedVersionInsideItsDeclaredRange() {
        List<AnalysisIssue> issues = analyzer.analyse(
                snapshot("collection", "1.19.1"),
                pubspec(declaration("^1.19.0", DartDependencySection.MAIN), Map.of()));

        assertTrue(issues.isEmpty());
    }

    @Test
    void reportsAResolvedVersionOutsideItsDeclaredRange() {
        List<AnalysisIssue> issues = analyzer.analyse(
                snapshot("collection", "2.0.0"),
                pubspec(declaration("^1.19.0", DartDependencySection.MAIN), Map.of()));

        assertEquals(1, issues.size());
        assertEquals("RESOLVED_VERSION_OUTSIDE_CONSTRAINT", issues.getFirst().code());
    }

    @Test
    void reportsInvalidConstraintAndResolvedVersionText() {
        List<AnalysisIssue> invalidConstraint = analyzer.analyse(
                snapshot("collection", "1.19.1"),
                pubspec(declaration("latest", DartDependencySection.MAIN), Map.of()));
        List<AnalysisIssue> invalidVersion = analyzer.analyse(
                snapshot("collection", "from-git"),
                pubspec(declaration("^1.0.0", DartDependencySection.MAIN), Map.of()));

        assertEquals("INVALID_VERSION_CONSTRAINT", invalidConstraint.getFirst().code());
        assertEquals("INVALID_RESOLVED_VERSION", invalidVersion.getFirst().code());
    }

    @Test
    void usesAnOverrideAsTheEffectiveConstraint() {
        DartDeclaredDependency main = declaration("^1.0.0", DartDependencySection.MAIN);
        DartDeclaredDependency override = declaration("any", DartDependencySection.OVERRIDE);

        List<AnalysisIssue> issues = analyzer.analyse(
                snapshot("collection", "2.0.0"),
                pubspec(main, Map.of("collection", override)));

        assertTrue(issues.isEmpty());
    }

    private static DartDeclaredDependency declaration(
            String constraint,
            DartDependencySection section) {
        return new DartDeclaredDependency("collection", section, constraint, "hosted");
    }

    private static DartPubspec pubspec(
            DartDeclaredDependency dependency,
            Map<String, DartDeclaredDependency> overrides) {
        Map<String, DartDeclaredDependency> main = dependency.section()
                == DartDependencySection.MAIN ? Map.of("collection", dependency) : Map.of();
        Map<String, DartDeclaredDependency> dev = dependency.section()
                == DartDependencySection.DEV ? Map.of("collection", dependency) : Map.of();
        return new DartPubspec(
                "app", "1.0.0", Optional.empty(), main, dev, overrides);
    }

    private static DependencySnapshot snapshot(String name, String version) {
        PackageVersion root = new PackageVersion("app", "1.0.0");
        PackageVersion dependency = new PackageVersion(name, version);
        DependencyGraph graph = new DependencyGraph();
        graph.addDependency(root, dependency);
        return new DependencySnapshot(
                root,
                graph,
                Map.of(
                        root.name(), new PackageMetadata(root, DependencyKind.ROOT, "root"),
                        dependency.name(), new PackageMetadata(
                                dependency, DependencyKind.DIRECT, "hosted")));
    }
}
