package io.safebump.adapters.dart;

import io.safebump.adapters.dart.model.DartDeclaredDependency;
import io.safebump.adapters.dart.model.DartDependencySection;
import io.safebump.adapters.dart.model.DartLockfile;
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

class DartProjectReconcilerTest {

    private static final PackageVersion ROOT = packageVersion("example_app", "1.0.0");
    private static final PackageVersion DIRECT = packageVersion("direct_package", "2.0.0");
    private static final PackageVersion TRANSITIVE = packageVersion(
            "transitive_package", "3.0.0");
    private static final PackageVersion DEV = packageVersion("dev_package", "4.0.0");

    private final DartProjectReconciler reconciler = new DartProjectReconciler();

    @Test
    void returnsNoIssuesWhenAllThreeMetadataViewsAgree() {
        List<AnalysisIssue> issues = reconciler.reconcile(
                dependencySnapshot(),
                consistentPubspec(),
                consistentLockfile());

        assertTrue(issues.isEmpty());
    }

    @Test
    void reportsEverySupportedMismatchInDeterministicOrder() {
        DartPubspec mismatchedPubspec = new DartPubspec(
                "different_app",
                "9.0.0",
                Optional.empty(),
                Map.of(
                        "missing_declaration",
                        declaration("missing_declaration", DartDependencySection.MAIN)),
                Map.of(
                        DIRECT.name(),
                        declaration(DIRECT.name(), DartDependencySection.DEV)),
                Map.of());

        PackageVersion changedDirect = packageVersion(DIRECT.name(), "99.0.0");
        PackageVersion lockedOnly = packageVersion("locked_only", "1.0.0");
        DartLockfile mismatchedLockfile = new DartLockfile(
                Map.of(
                        DIRECT.name(), metadata(
                                changedDirect, DependencyKind.TRANSITIVE, "path"),
                        DEV.name(), metadata(DEV, DependencyKind.DEV, "hosted"),
                        lockedOnly.name(), metadata(
                                lockedOnly, DependencyKind.TRANSITIVE, "hosted")),
                Map.of());

        List<AnalysisIssue> issues = reconciler.reconcile(
                dependencySnapshot(),
                mismatchedPubspec,
                mismatchedLockfile);

        assertEquals(
                List.of(
                        "DECLARED_DEPENDENCY_MISSING",
                        "DECLARED_KIND_MISMATCH",
                        "KIND_MISMATCH",
                        "MISSING_FROM_GRAPH",
                        "MISSING_FROM_LOCKFILE",
                        "ROOT_NAME_MISMATCH",
                        "ROOT_VERSION_MISMATCH",
                        "SOURCE_MISMATCH",
                        "VERSION_MISMATCH"),
                issues.stream().map(AnalysisIssue::code).toList());
    }

    private static DependencySnapshot dependencySnapshot() {
        DependencyGraph graph = new DependencyGraph();
        graph.addDependency(ROOT, DIRECT);
        graph.addDependency(ROOT, DEV);
        graph.addDependency(DIRECT, TRANSITIVE);

        return new DependencySnapshot(
                ROOT,
                graph,
                Map.of(
                        ROOT.name(), metadata(ROOT, DependencyKind.ROOT, "root"),
                        DIRECT.name(), metadata(DIRECT, DependencyKind.DIRECT, "hosted"),
                        TRANSITIVE.name(), metadata(
                                TRANSITIVE, DependencyKind.TRANSITIVE, "git"),
                        DEV.name(), metadata(DEV, DependencyKind.DEV, "hosted")));
    }

    private static DartPubspec consistentPubspec() {
        return new DartPubspec(
                ROOT.name(),
                ROOT.version(),
                Optional.of(">=3.10.0 <4.0.0"),
                Map.of(DIRECT.name(), declaration(
                        DIRECT.name(), DartDependencySection.MAIN)),
                Map.of(DEV.name(), declaration(DEV.name(), DartDependencySection.DEV)),
                Map.of());
    }

    private static DartLockfile consistentLockfile() {
        return new DartLockfile(
                Map.of(
                        DIRECT.name(), metadata(DIRECT, DependencyKind.DIRECT, "hosted"),
                        TRANSITIVE.name(), metadata(
                                TRANSITIVE, DependencyKind.TRANSITIVE, "git"),
                        DEV.name(), metadata(DEV, DependencyKind.DEV, "hosted")),
                Map.of("dart", ">=3.10.0 <4.0.0"));
    }

    private static DartDeclaredDependency declaration(
            String name,
            DartDependencySection section) {
        return new DartDeclaredDependency(name, section, "any", "hosted");
    }

    private static PackageMetadata metadata(
            PackageVersion packageVersion,
            DependencyKind kind,
            String source) {
        return new PackageMetadata(packageVersion, kind, source);
    }

    private static PackageVersion packageVersion(String name, String version) {
        return new PackageVersion(name, version);
    }
}
