package io.safebump.core.analysis;

import java.util.Objects;

/** One deterministic consistency issue found while assembling project metadata. */
public record AnalysisIssue(String code, String message)
        implements Comparable<AnalysisIssue> {

    public AnalysisIssue {
        code = requireText(code, "code");
        message = requireText(message, "message");
    }

    @Override
    public int compareTo(AnalysisIssue other) {
        Objects.requireNonNull(other, "other");
        int codeComparison = code.compareTo(other.code);
        return codeComparison != 0 ? codeComparison : message.compareTo(other.message);
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
