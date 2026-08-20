package io.safebump.core.version;

import java.util.Objects;

/** Two requirements for one package whose version ranges do not overlap. */
public record VersionConflict(
        String packageName,
        VersionRequirement first,
        VersionRequirement second) implements Comparable<VersionConflict> {

    public VersionConflict {
        Objects.requireNonNull(packageName, "packageName");
        packageName = packageName.trim();
        if (packageName.isEmpty()) {
            throw new IllegalArgumentException("packageName must not be blank");
        }
        Objects.requireNonNull(first, "first");
        Objects.requireNonNull(second, "second");
        if (first.compareTo(second) > 0) {
            VersionRequirement swap = first;
            first = second;
            second = swap;
        }
        if (first.range().overlaps(second.range())) {
            throw new IllegalArgumentException("Conflicting requirements must not overlap");
        }
    }

    @Override
    public int compareTo(VersionConflict other) {
        Objects.requireNonNull(other, "other");
        int packageComparison = packageName.compareTo(other.packageName);
        if (packageComparison != 0) {
            return packageComparison;
        }
        int firstComparison = first.compareTo(other.first);
        return firstComparison != 0 ? firstComparison : second.compareTo(other.second);
    }
}
