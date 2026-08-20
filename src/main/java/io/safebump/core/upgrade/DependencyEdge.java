package io.safebump.core.upgrade;

import java.util.Objects;

/** A package-name dependency relationship independent of resolved versions. */
public record DependencyEdge(String from, String to) implements Comparable<DependencyEdge> {

    public DependencyEdge {
        from = requireText(from, "from");
        to = requireText(to, "to");
    }

    @Override
    public int compareTo(DependencyEdge other) {
        Objects.requireNonNull(other, "other");
        int fromComparison = from.compareTo(other.from);
        return fromComparison != 0 ? fromComparison : to.compareTo(other.to);
    }

    @Override
    public String toString() {
        return from + " -> " + to;
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
