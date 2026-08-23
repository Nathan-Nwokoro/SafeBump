package io.safebump.core.version;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/** Finds pairwise incompatible requirements for a package. */
public final class VersionConflictDetector {

    public List<VersionConflict> findConflicts(
            String packageName,
            Collection<VersionRequirement> requirements) {
        Objects.requireNonNull(requirements, "requirements");
        List<VersionRequirement> sortedRequirements = requirements.stream()
                .map(requirement -> Objects.requireNonNull(requirement, "requirement"))
                .sorted()
                .toList();

        List<VersionConflict> conflicts = new ArrayList<>();
        for (int firstIndex = 0;
                firstIndex < sortedRequirements.size();
                firstIndex++) {
            VersionRequirement first = sortedRequirements.get(firstIndex);
            for (int secondIndex = firstIndex + 1;
                    secondIndex < sortedRequirements.size();
                    secondIndex++) {
                VersionRequirement second = sortedRequirements.get(secondIndex);
                if (!first.range().overlaps(second.range())) {
                    conflicts.add(new VersionConflict(packageName, first, second));
                }
            }
        }
        return List.copyOf(conflicts);
    }
}
