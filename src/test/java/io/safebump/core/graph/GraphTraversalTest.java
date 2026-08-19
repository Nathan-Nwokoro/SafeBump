package io.safebump.core.graph;

import io.safebump.core.model.PackageVersion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GraphTraversalTest {

    private static final PackageVersion KCAL = packageVersion("kcal");
    private static final PackageVersion FIREBASE = packageVersion("firebase");
    private static final PackageVersion IMAGE_PICKER = packageVersion("image_picker");
    private static final PackageVersion CORE = packageVersion("core");
    private static final PackageVersion AUTH = packageVersion("auth");

    private DependencyGraph graph;

    @BeforeEach
    void setUp() {
        graph = new DependencyGraph();
        graph.addDependency(KCAL, FIREBASE);
        graph.addDependency(KCAL, IMAGE_PICKER);
        graph.addDependency(FIREBASE, CORE);
        graph.addDependency(FIREBASE, AUTH);
        graph.addDependency(AUTH, CORE);
    }

    @Test
    void traversesDependenciesInDeterministicDepthFirstOrder() {
        assertEquals(
                List.of(FIREBASE, AUTH, CORE, IMAGE_PICKER),
                graph.getDependenciesDepthFirst(KCAL));
    }

    @Test
    void returnsAnEmptyDepthFirstTraversalForAnUnknownPackage() {
        assertEquals(List.of(), graph.getDependenciesDepthFirst(packageVersion("unknown")));
    }

    @Test
    void depthFirstTraversalTerminatesOnACycle() {
        graph.addDependency(CORE, FIREBASE);

        assertEquals(
                List.of(AUTH, CORE),
                graph.getDependenciesDepthFirst(FIREBASE));
    }

    @Test
    void reportsAnAcyclicGraph() {
        assertFalse(graph.hasCycle());
        assertEquals(Optional.empty(), graph.findCycle());
    }

    @Test
    void detectsASelfCycle() {
        DependencyGraph selfCycleGraph = new DependencyGraph();
        selfCycleGraph.addDependency(AUTH, AUTH);

        assertTrue(selfCycleGraph.hasCycle());
        assertEquals(Optional.of(List.of(AUTH, AUTH)), selfCycleGraph.findCycle());
    }

    @Test
    void returnsThePackagesThatFormAMultiPackageCycle() {
        graph.addDependency(CORE, FIREBASE);

        assertTrue(graph.hasCycle());
        assertEquals(
                Optional.of(List.of(AUTH, CORE, FIREBASE, AUTH)),
                graph.findCycle());
    }

    @Test
    void findsACycleInADisconnectedGraph() {
        DependencyGraph disconnectedGraph = new DependencyGraph();
        PackageVersion isolated = packageVersion("isolated");
        PackageVersion packageX = packageVersion("package_x");
        PackageVersion packageY = packageVersion("package_y");
        disconnectedGraph.addPackage(isolated);
        disconnectedGraph.addDependency(packageX, packageY);
        disconnectedGraph.addDependency(packageY, packageX);

        assertEquals(
                Optional.of(List.of(packageX, packageY, packageX)),
                disconnectedGraph.findCycle());
    }

    private static PackageVersion packageVersion(String name) {
        return new PackageVersion(name, "1.0.0");
    }
}
