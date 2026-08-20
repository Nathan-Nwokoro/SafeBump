package io.safebump.core.version;

import io.safebump.core.model.PackageVersion;

import java.util.Objects;

/** A version range and the dependency which imposed it. */
public record VersionRequirement(PackageVersion origin, VersionRange range)
        implements Comparable<VersionRequirement> {

    public VersionRequirement {
        Objects.requireNonNull(origin, "origin");
        Objects.requireNonNull(range, "range");
    }

    @Override
    public int compareTo(VersionRequirement other) {
        Objects.requireNonNull(other, "other");
        int originComparison = origin.compareTo(other.origin);
        return originComparison != 0
                ? originComparison
                : range.toString().compareTo(other.range.toString());
    }
}
