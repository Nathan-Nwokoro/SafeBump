package io.safebump.core.model;

import java.util.Objects;

/**
 * Identifies one resolved version of a package.
 *
 * <p>The version is intentionally stored as text at this stage. Semantic
 * version comparison belongs to the later version-constraint milestone.</p>
 */
public record PackageVersion(String name, String version)
        implements Comparable<PackageVersion> {

    public PackageVersion {
        name = requireText(name, "name");
        version = requireText(version, "version");
    }

    @Override
    public int compareTo(PackageVersion other) {
        Objects.requireNonNull(other, "other");

        int nameComparison = name.compareTo(other.name);
        return nameComparison != 0
                ? nameComparison
                : version.compareTo(other.version);
    }

    @Override
    public String toString() {
        return name + "@" + version;
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName);

        String trimmedValue = value.trim();
        if (trimmedValue.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return trimmedValue;
    }
}
