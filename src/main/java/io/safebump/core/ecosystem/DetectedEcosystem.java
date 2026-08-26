package io.safebump.core.ecosystem;

import java.util.Objects;

/** One resolved ecosystem provider and its normalized identity. */
public record DetectedEcosystem(
        String id,
        String displayName,
        DependencyEcosystemProvider provider) {

    public DetectedEcosystem {
        id = requireText(id, "id");
        displayName = requireText(displayName, "displayName");
        Objects.requireNonNull(provider, "provider");
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName);
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return trimmed;
    }
}
