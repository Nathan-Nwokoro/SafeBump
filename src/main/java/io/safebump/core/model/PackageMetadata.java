package io.safebump.core.model;

import java.util.Objects;

/** Ecosystem-neutral metadata attached to a resolved package. */
public record PackageMetadata(
        PackageVersion packageVersion,
        DependencyKind kind,
        String source) {

    public PackageMetadata {
        Objects.requireNonNull(packageVersion, "packageVersion");
        Objects.requireNonNull(kind, "kind");
        source = requireText(source, "source");
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
