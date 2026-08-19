package io.safebump.core.graph;

import io.safebump.core.model.PackageVersion;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Optional;
import java.util.Set;

/** Algorithms for traversing and validating a dependency graph. */
final class GraphTraversal {

    private GraphTraversal() {
    }

    static List<PackageVersion> depthFirst(
            NavigableMap<PackageVersion, NavigableSet<PackageVersion>> adjacencyMap,
            PackageVersion startingPackage) {
        if (!adjacencyMap.containsKey(startingPackage)) {
            return List.of();
        }

        Set<PackageVersion> visited = new HashSet<>();
        List<PackageVersion> traversalOrder = new ArrayList<>();
        visited.add(startingPackage);
        visitDepthFirst(adjacencyMap, startingPackage, visited, traversalOrder);
        return List.copyOf(traversalOrder);
    }

    static Optional<List<PackageVersion>> findCycle(
            NavigableMap<PackageVersion, NavigableSet<PackageVersion>> adjacencyMap) {
        Map<PackageVersion, VisitState> visitStates = new HashMap<>();
        List<PackageVersion> activePath = new ArrayList<>();

        for (PackageVersion packageVersion : adjacencyMap.navigableKeySet()) {
            if (!visitStates.containsKey(packageVersion)) {
                Optional<List<PackageVersion>> cycle = findCycleFrom(
                        adjacencyMap, packageVersion, visitStates, activePath);
                if (cycle.isPresent()) {
                    return cycle;
                }
            }
        }

        return Optional.empty();
    }

    private static void visitDepthFirst(
            NavigableMap<PackageVersion, NavigableSet<PackageVersion>> adjacencyMap,
            PackageVersion currentPackage,
            Set<PackageVersion> visited,
            List<PackageVersion> traversalOrder) {
        for (PackageVersion dependency : adjacencyMap.get(currentPackage)) {
            if (visited.add(dependency)) {
                traversalOrder.add(dependency);
                visitDepthFirst(adjacencyMap, dependency, visited, traversalOrder);
            }
        }
    }

    private static Optional<List<PackageVersion>> findCycleFrom(
            NavigableMap<PackageVersion, NavigableSet<PackageVersion>> adjacencyMap,
            PackageVersion currentPackage,
            Map<PackageVersion, VisitState> visitStates,
            List<PackageVersion> activePath) {
        visitStates.put(currentPackage, VisitState.VISITING);
        activePath.add(currentPackage);

        for (PackageVersion dependency : adjacencyMap.get(currentPackage)) {
            VisitState dependencyState = visitStates.get(dependency);
            if (dependencyState == VisitState.VISITING) {
                return Optional.of(extractCycle(activePath, dependency));
            }
            if (dependencyState == null) {
                Optional<List<PackageVersion>> cycle = findCycleFrom(
                        adjacencyMap, dependency, visitStates, activePath);
                if (cycle.isPresent()) {
                    return cycle;
                }
            }
        }

        activePath.removeLast();
        visitStates.put(currentPackage, VisitState.VISITED);
        return Optional.empty();
    }

    private static List<PackageVersion> extractCycle(
            List<PackageVersion> activePath,
            PackageVersion cycleStart) {
        int cycleStartIndex = activePath.indexOf(cycleStart);
        List<PackageVersion> cycle = new ArrayList<>(
                activePath.subList(cycleStartIndex, activePath.size()));
        cycle.add(cycleStart);
        return List.copyOf(cycle);
    }

    private enum VisitState {
        VISITING,
        VISITED
    }
}
