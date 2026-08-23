package io.safebump.adapters.dart.model;

import java.util.Objects;

/** A dependency declaration read from a Dart pubspec. */
public record DartDeclaredDependency(
        String name,
        DartDependencySection section,
        String constraint,
        String source) {

    public DartDeclaredDependency {
        name = requireText(name, "name");
        Objects.requireNonNull(section, "section");
        constraint = requireText(constraint, "constraint");
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
