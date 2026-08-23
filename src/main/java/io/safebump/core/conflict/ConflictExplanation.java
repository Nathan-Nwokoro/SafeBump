package io.safebump.core.conflict;

import io.safebump.core.model.PackageVersion;
import io.safebump.core.version.VersionConflict;
import io.safebump.core.version.VersionRequirement;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/** A conflict plus the dependency paths to both requirements which caused it. */
public record ConflictExplanation(
        VersionConflict conflict,
        List<PackageVersion> firstPath,
        List<PackageVersion> secondPath) {

    public ConflictExplanation {
        Objects.requireNonNull(conflict, "conflict");
        firstPath = immutablePath(firstPath, conflict.first(), "firstPath");
        secondPath = immutablePath(secondPath, conflict.second(), "secondPath");
    }

    public String format() {
        return "Conflict detected for " + conflict.packageName() + ":\n"
                + conflict.first().origin() + " requires "
                + conflict.first().range() + "\n"
                + "  path: " + formatPath(firstPath) + "\n"
                + conflict.second().origin() + " requires "
                + conflict.second().range() + "\n"
                + "  path: " + formatPath(secondPath) + "\n"
                + "The two version constraints do not overlap.";
    }

    private static List<PackageVersion> immutablePath(
            List<PackageVersion> path,
            VersionRequirement requirement,
            String fieldName) {
        Objects.requireNonNull(path, fieldName);
        List<PackageVersion> copy = path.stream()
                .map(packageVersion -> Objects.requireNonNull(
                        packageVersion, fieldName + " package"))
                .toList();
        if (copy.isEmpty() || !copy.getLast().equals(requirement.origin())) {
            throw new IllegalArgumentException(
                    fieldName + " must end at " + requirement.origin());
        }
        return copy;
    }

    private static String formatPath(List<PackageVersion> path) {
        return path.stream()
                .map(PackageVersion::toString)
                .collect(Collectors.joining(" -> "));
    }
}
