package io.safebump.core.solver;

import io.safebump.core.model.PackageVersion;

import java.util.Objects;
import java.util.Optional;

/** One package added, removed, or changed by a solver solution. */
public record VersionSelection(
        String packageName,
        Optional<PackageVersion> before,
        Optional<PackageVersion> after) implements Comparable<VersionSelection> {

    public VersionSelection {
        Objects.requireNonNull(packageName, "packageName");
        packageName = packageName.trim();
        if (packageName.isEmpty()) {
            throw new IllegalArgumentException("packageName must not be blank");
        }
        Objects.requireNonNull(before, "before");
        Objects.requireNonNull(after, "after");
        if (before.isEmpty() && after.isEmpty()) {
            throw new IllegalArgumentException("A selection must have a before or after version");
        }
        if (before.isPresent()) {
            validatePackageName(packageName, before.orElseThrow());
        }
        if (after.isPresent()) {
            validatePackageName(packageName, after.orElseThrow());
        }
        if (before.isPresent() && after.isPresent() && before.get().equals(after.get())) {
            throw new IllegalArgumentException("A selection must change the package version");
        }
    }

    @Override
    public int compareTo(VersionSelection other) {
        Objects.requireNonNull(other, "other");
        return packageName.compareTo(other.packageName);
    }

    private static void validatePackageName(
            String packageName,
            PackageVersion version) {
        if (!packageName.equals(version.name())) {
            throw new IllegalArgumentException(
                    "Selection package name must match version package name");
        }
    }
}
