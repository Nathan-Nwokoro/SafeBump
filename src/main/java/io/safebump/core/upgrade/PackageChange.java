package io.safebump.core.upgrade;

import io.safebump.core.model.PackageMetadata;

import java.util.Objects;
import java.util.Optional;

/** Before/after metadata for one package name. */
public record PackageChange(
        String packageName,
        PackageChangeType type,
        Optional<PackageMetadata> before,
        Optional<PackageMetadata> after) implements Comparable<PackageChange> {

    public PackageChange {
        Objects.requireNonNull(packageName, "packageName");
        packageName = packageName.trim();
        if (packageName.isEmpty()) {
            throw new IllegalArgumentException("packageName must not be blank");
        }
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(before, "before");
        Objects.requireNonNull(after, "after");
        validateShape(type, before, after);
    }

    public boolean isChanged() {
        return type != PackageChangeType.UNCHANGED;
    }

    public boolean isTransitive() {
        return after.or(() -> before)
                .map(metadata -> metadata.kind()
                        == io.safebump.core.model.DependencyKind.TRANSITIVE)
                .orElse(false);
    }

    @Override
    public int compareTo(PackageChange other) {
        Objects.requireNonNull(other, "other");
        return packageName.compareTo(other.packageName);
    }

    private static void validateShape(
            PackageChangeType type,
            Optional<PackageMetadata> before,
            Optional<PackageMetadata> after) {
        boolean valid = switch (type) {
            case ADDED -> before.isEmpty() && after.isPresent();
            case REMOVED -> before.isPresent() && after.isEmpty();
            case UPGRADED, DOWNGRADED, UNCHANGED -> before.isPresent() && after.isPresent();
        };
        if (!valid) {
            throw new IllegalArgumentException(
                    "Before/after package metadata does not match change type " + type);
        }
    }
}
