package io.safebump.core.graph;

import io.safebump.core.model.PackageVersion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DependencyGraphTest {

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
    void addsPackagesWhenAddingDependencies() {
        assertEquals(5, graph.packageCount());
        assertEquals(5, graph.dependencyCount());
        assertTrue(graph.containsPackage(KCAL));
        assertTrue(graph.containsPackage(CORE));
    }

    @Test
    void returnsDirectDependenciesInDeterministicOrder() {
        assertEquals(
                List.of(AUTH, CORE),
                List.copyOf(graph.getDirectDependencies(FIREBASE)));
    }

    @Test
    void returnsEveryTransitiveDependencyOnlyOnce() {
        assertEquals(
                List.of(AUTH, CORE, FIREBASE, IMAGE_PICKER),
                List.copyOf(graph.getTransitiveDependencies(KCAL)));
    }

    @Test
    void traversesDirectAndTransitiveDependents() {
        assertEquals(
                List.of(AUTH, FIREBASE),
                List.copyOf(graph.getDirectDependents(CORE)));
        assertEquals(
                List.of(AUTH, FIREBASE, KCAL),
                List.copyOf(graph.getTransitiveDependents(CORE)));
    }

    @Test
    void returnsEmptyResultsForAnUnknownPackage() {
        PackageVersion unknown = packageVersion("unknown");

        assertFalse(graph.containsPackage(unknown));
        assertEquals(Set.of(), graph.getDirectDependencies(unknown));
        assertEquals(Set.of(), graph.getTransitiveDependencies(unknown));
        assertEquals(Set.of(), graph.getDirectDependents(unknown));
        assertEquals(Set.of(), graph.getTransitiveDependents(unknown));
    }

    @Test
    void terminatesOnCyclesAndDoesNotReturnTheStartingPackage() {
        graph.addDependency(CORE, FIREBASE);

        assertEquals(
                List.of(AUTH, CORE),
                List.copyOf(graph.getTransitiveDependencies(FIREBASE)));
        assertEquals(
                List.of(AUTH, FIREBASE, KCAL),
                List.copyOf(graph.getTransitiveDependents(CORE)));
    }

    @Test
    void returnsImmutableGraphViews() {
        Set<PackageVersion> dependencies = graph.getDirectDependencies(FIREBASE);
        Set<PackageVersion> packages = graph.getPackages();

        assertThrows(UnsupportedOperationException.class, () -> dependencies.add(KCAL));
        assertThrows(UnsupportedOperationException.class, () -> packages.remove(KCAL));
    }

    @Test
    void ignoresDuplicateEdges() {
        graph.addDependency(KCAL, FIREBASE);

        assertEquals(5, graph.dependencyCount());
        assertEquals(List.of(FIREBASE, IMAGE_PICKER),
                List.copyOf(graph.getDirectDependencies(KCAL)));
    }

    private static PackageVersion packageVersion(String name) {
        return new PackageVersion(name, "1.0.0");
    }
}
